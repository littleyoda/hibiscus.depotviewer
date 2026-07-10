package de.open4me.depot.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class PortfolioPerformanceExporter
{
	private static final String[] HEADERS = {
			"Datum",
			"Typ",
			"Wert",
			"Buchungswährung",
			"Gebühren",
			"Steuern",
			"Stück",
			"ISIN",
			"WKN",
			"Notiz"
	};

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
	private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00",
			DecimalFormatSymbols.getInstance(Locale.GERMANY));

	public List<DepotInfo> getDepots() throws ApplicationException
	{
		List<DepotInfo> depots = new ArrayList<DepotInfo>();
		String sql = "select distinct k.id, k.bezeichnung, k.kontonummer "
				+ "from depotviewer_umsaetze du "
				+ "join konto k on k.id = du.kontoid "
				+ "order by k.bezeichnung";
		try (Connection conn = SQLUtils.getConnection();
				PreparedStatement statement = conn.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				Konto konto = Utils.getKontoByID(Integer.toString(rs.getInt("id")));
				if (konto == null || konto.hasFlag(Konto.FLAG_DISABLED)) {
					continue;
				}
				depots.add(new DepotInfo(
						rs.getInt("id"),
						value(rs, "bezeichnung"),
						value(rs, "kontonummer")));
			}
		} catch (Exception e) {
			throw new ApplicationException("Fehler beim Laden der Depots", e);
		}
		return depots;
	}

	public ExportResult export(File targetDir, List<DepotInfo> depots) throws ApplicationException
	{
		return export(targetDir, depots, null, null);
	}

	public ExportResult export(File targetDir, List<DepotInfo> depots, java.util.Date from, java.util.Date to) throws ApplicationException
	{
		if (targetDir == null) {
			throw new ApplicationException("Kein Zielverzeichnis ausgewählt.");
		}
		if (!targetDir.exists() && !targetDir.mkdirs()) {
			throw new ApplicationException("Zielverzeichnis konnte nicht angelegt werden: " + targetDir);
		}
		if (!targetDir.isDirectory() || !targetDir.canWrite()) {
			throw new ApplicationException("Zielverzeichnis ist nicht beschreibbar: " + targetDir);
		}

		int files = 0;
		int rows = 0;
		for (DepotInfo depot : depots) {
			List<Transaction> transactions = loadTransactions(depot, from, to);
			File file = new File(targetDir, buildFilename(depot, transactions));
			rows += writeDepot(file, transactions);
			files++;
		}
		return new ExportResult(files, rows);
	}

	private List<Transaction> loadTransactions(DepotInfo depot) throws ApplicationException
	{
		return loadTransactions(depot, null, null);
	}

	private List<Transaction> loadTransactions(DepotInfo depot, java.util.Date from, java.util.Date to) throws ApplicationException
	{
		List<Transaction> transactions = new ArrayList<Transaction>();
		String sql = "select du.*, w.isin "
				+ "from depotviewer_umsaetze du "
				+ "left join depotviewer_wertpapier w on w.id = du.wpid "
				+ "where du.kontoid = ? ";
		if (from != null) {
			sql += "and du.buchungsdatum >= ? ";
		}
		if (to != null) {
			sql += "and du.buchungsdatum <= ? ";
		}
		sql += "order by du.buchungsdatum, du.id";
		try (Connection conn = SQLUtils.getConnection();
				PreparedStatement statement = conn.prepareStatement(sql)) {
			int index = 1;
			statement.setInt(index++, depot.id);
			if (from != null) {
				statement.setDate(index++, new Date(from.getTime()));
			}
			if (to != null) {
				statement.setDate(index++, new Date(to.getTime()));
			}
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					Transaction transaction = new Transaction();
					transaction.id = rs.getInt("id");
					transaction.action = value(rs, "aktion");
					transaction.bookingDate = rs.getDate("buchungsdatum");
					transaction.quantity = decimal(rs, "anzahl");
					transaction.costs = decimal(rs, "kosten");
					transaction.transactionCosts = decimal(rs, "transaktionskosten");
					transaction.taxes = decimal(rs, "steuern");
					transaction.isin = value(rs, "isin");
					transaction.bookingInformation = value(rs, "buchungsinformationen");
					transactions.add(transaction);
				}
			}
		} catch (Exception e) {
			throw new ApplicationException("Fehler beim Laden der Umsätze für " + depot.bezeichnung, e);
		}
		return transactions;
	}

	private int writeDepot(File file, List<Transaction> transactions) throws ApplicationException
	{
		int written = 0;
		CSVFormat format = CSVFormat.EXCEL.withDelimiter(';');
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
				CSVPrinter printer = new CSVPrinter(writer, format)) {
			printer.printRecord((Object[]) HEADERS);
			for (Transaction transaction : transactions) {
				String type = mapAction(transaction.action, transaction.id);
				if (type == null) {
					continue;
				}
				Map<String, String> bookingInfo = parseBookingInformation(transaction.bookingInformation);
				printer.printRecord(
						formatDate(transaction.bookingDate),
						type,
						formatAmount(transaction.costs.negate()),
						"",
						formatAmount(transaction.transactionCosts),
						formatAmount(transaction.taxes),
						formatQuantity(transaction.quantity),
						transaction.isin,
						firstNotEmpty(bookingInfo.get("wkn"), ""),
						"Depotviewer-ID: " + transaction.id);
				written++;
			}
		} catch (Exception e) {
			throw new ApplicationException("Fehler beim CSV-Export: " + file.getName(), e);
		}
		return written;
	}

	private String mapAction(String action, int transactionId)
	{
		String normalized = action == null ? "" : action.trim().toUpperCase(Locale.GERMANY);
		if ("KAUF".equals(normalized) || "EINBUCHUNG".equals(normalized) || "EINLIEFERUNG".equals(normalized)) {
			return "Einlieferung";
		}
		if ("VERKAUF".equals(normalized) || "AUSBUCHUNG".equals(normalized) || "AUSLIEFERUNG".equals(normalized)) {
			return "Auslieferung";
		}
		Logger.warn("Portfolio-Performance-Export: unbekannte Aktion '" + action
				+ "' bei Depotviewer-ID " + transactionId + ". Eintrag wird übersprungen.");
		return null;
	}

	private String buildFilename(DepotInfo depot, List<Transaction> transactions)
	{
		StringBuilder name = new StringBuilder();
		if (depot.kontonummer != null && depot.kontonummer.trim().length() > 0) {
			name.append(sanitizeFilename(depot.kontonummer)).append(' ');
		}
		name.append(sanitizeFilename(depot.bezeichnung));

		if (!transactions.isEmpty()) {
			String firstYear = year(transactions.get(0).bookingDate);
			String lastYear = year(transactions.get(transactions.size() - 1).bookingDate);
			if (firstYear.length() > 0 && lastYear.length() > 0) {
				name.append(' ');
				name.append(firstYear.equals(lastYear) ? firstYear : firstYear + "-" + lastYear);
			}
		}
		name.append(".csv");
		return name.toString();
	}

	private Map<String, String> parseBookingInformation(String text)
	{
		Map<String, String> result = new HashMap<String, String>();
		if (text == null) {
			return result;
		}
		String value = text.trim();
		if (value.startsWith("{") && value.endsWith("}")) {
			value = value.substring(1, value.length() - 1);
		}
		String[] parts = value.split(", ");
		for (String part : parts) {
			int separator = part.indexOf('=');
			if (separator < 0) {
				continue;
			}
			result.put(part.substring(0, separator).trim().toLowerCase(Locale.GERMANY),
					part.substring(separator + 1).trim());
		}
		return result;
	}

	private static String sanitizeFilename(String name)
	{
		String value = name == null ? "" : name.trim();
		value = value.replaceAll("[<>:\"/\\\\|?*]", "_").replaceAll("[. ]+$", "");
		return value.length() == 0 ? "Depot" : value;
	}

	private static String formatDate(Date date)
	{
		if (date == null) {
			return "";
		}
		return DATE_FORMAT.format(date);
	}

	private static String formatAmount(BigDecimal value)
	{
		if (value == null) {
			value = BigDecimal.ZERO;
		}
		return AMOUNT_FORMAT.format(value.setScale(2, RoundingMode.HALF_UP));
	}

	private static String formatQuantity(BigDecimal value)
	{
		if (value == null) {
			return "0";
		}
		BigDecimal stripped = value.stripTrailingZeros();
		return stripped.toPlainString().replace('.', ',');
	}

	private static String year(Date date)
	{
		if (date == null) {
			return "";
		}
		String value = date.toString();
		return value.length() >= 4 ? value.substring(0, 4) : "";
	}

	private static String value(ResultSet rs, String column) throws Exception
	{
		String value = rs.getString(column);
		return value == null ? "" : value;
	}

	private static BigDecimal decimal(ResultSet rs, String column) throws Exception
	{
		BigDecimal value = rs.getBigDecimal(column);
		return value == null ? BigDecimal.ZERO : value;
	}

	private static String firstNotEmpty(String first, String second)
	{
		if (first != null && first.trim().length() > 0) {
			return first;
		}
		return second == null ? "" : second;
	}

	public static class DepotInfo
	{
		public final int id;
		public final String bezeichnung;
		public final String kontonummer;

		public DepotInfo(int id, String bezeichnung, String kontonummer)
		{
			this.id = id;
			this.bezeichnung = bezeichnung;
			this.kontonummer = kontonummer;
		}

		public String getDisplayName()
		{
			if (kontonummer != null && kontonummer.trim().length() > 0) {
				return kontonummer + " " + bezeichnung;
			}
			return bezeichnung;
		}
	}

	public static class ExportResult
	{
		public final int files;
		public final int rows;

		public ExportResult(int files, int rows)
		{
			this.files = files;
			this.rows = rows;
		}
	}

	private static class Transaction
	{
		private int id;
		private String action;
		private Date bookingDate;
		private BigDecimal quantity;
		private BigDecimal costs;
		private BigDecimal transactionCosts;
		private BigDecimal taxes;
		private String isin;
		private String bookingInformation;
	}
}
