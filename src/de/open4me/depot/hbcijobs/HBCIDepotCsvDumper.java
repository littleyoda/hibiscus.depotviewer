package de.open4me.depot.hbcijobs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.kapott.hbci.GV_Result.GVRWPDepotList;
import org.kapott.hbci.GV_Result.GVRWPDepotList.Entry.Gattung;
import org.kapott.hbci.GV_Result.GVRWPDepotUms;
import org.kapott.hbci.GV_Result.GVRWPDepotUms.Entry.FinancialInstrument;
import org.kapott.hbci.GV_Result.GVRWPDepotUms.Entry.FinancialInstrument.Transaction;
import org.kapott.hbci.structures.BigDecimalValue;
import org.kapott.hbci.structures.TypedValue;

import de.open4me.depot.DepotViewerPlugin;
import de.open4me.depot.abruf.utils.Utils;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.logging.Logger;

class HBCIDepotCsvDumper
{
	private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withDelimiter(';');

	static void dumpBestand(GVRWPDepotList result, Konto konto)
	{
		try {
			File file = file("WPDepotList", konto);
			try (CSVPrinter printer = printer(file)) {
				printer.printRecord(
						"konto_id",
						"konto_name",
						"konto_blz",
						"konto_bic",
						"depot_index",
						"depot_timestamp",
						"depot_total_wert",
						"depot_total_waehrung",
						"wkn",
						"isin",
						"name",
						"saldo_typ",
						"saldo_wert",
						"saldo_waehrung",
						"preis_wert",
						"preis_waehrung",
						"preis_typ",
						"preis_qualifier",
						"depotwert_wert",
						"depotwert_waehrung",
						"preis_timestamp",
						"rohdatensatz");
				int depotIndex = 0;
				for (GVRWPDepotList.Entry depot : result.getEntries()) {
					int gattungIndex = 0;
					for (Gattung g : depot.getEntries()) {
						printer.printRecord(
								konto.getID(),
								konto.getLongName(),
								value(konto.getBLZ()),
								value(konto.getBic()),
								Integer.valueOf(depotIndex),
								value(depot.timestamp),
								value(depot.total),
								currency(depot.total),
								value(g.wkn),
								value(g.isin),
								value(g.name),
								value(g.saldo_type),
								value(g.saldo),
								currency(g.saldo),
								value(g.price),
								currency(g.price),
								value(g.pricetype),
								value(g.pricequalifier),
								value(g.depotwert),
								currency(g.depotwert),
								value(g.timestamp_price),
								value(g));
						gattungIndex++;
					}
					if (gattungIndex == 0) {
						printer.printRecord(
								konto.getID(),
								konto.getLongName(),
								value(konto.getBLZ()),
								value(konto.getBic()),
								Integer.valueOf(depotIndex),
								value(depot.timestamp),
								value(depot.total),
								currency(depot.total),
								"", "", "", "", "", "", "", "", "", "", "", "", "",
								value(depot));
					}
					depotIndex++;
				}
				Logger.info("HBCI Depotbestand als CSV gespeichert: " + file.getAbsolutePath());
			}
		} catch (Exception e) {
			Logger.error("unable to write HBCI depot list CSV dump", e);
		}
	}

	static void dumpUmsaetze(GVRWPDepotUms result, Konto konto)
	{
		try {
			File file = file("WPDepotUms", konto);
			try (CSVPrinter printer = printer(file)) {
				printer.printRecord(
						"konto_id",
						"konto_name",
						"konto_blz",
						"konto_bic",
						"entry_index",
						"instrument_index",
						"transaction_index",
						"wkn",
						"isin",
						"name",
						"datum",
						"storno",
						"transaction_indicator",
						"richtung",
						"bezahlung",
						"anzahl_wert",
						"anzahl_typ",
						"betrag_wert",
						"betrag_waehrung",
						"roh_instrument",
						"roh_transaktion");
				int entryIndex = 0;
				for (GVRWPDepotUms.Entry entry : result.getEntries()) {
					int instrumentIndex = 0;
					for (FinancialInstrument instrument : entry.instruments) {
						int transactionIndex = 0;
						for (Transaction transaction : instrument.transactions) {
							printer.printRecord(
									konto.getID(),
									konto.getLongName(),
									value(konto.getBLZ()),
									value(konto.getBic()),
									Integer.valueOf(entryIndex),
									Integer.valueOf(instrumentIndex),
									Integer.valueOf(transactionIndex),
									value(instrument.wkn),
									value(instrument.isin),
									value(instrument.name),
									value(transaction.datum),
									Boolean.valueOf(transaction.storno),
									Integer.valueOf(transaction.transaction_indicator),
									Integer.valueOf(transaction.richtung),
									Integer.valueOf(transaction.bezahlung),
									value(transaction.anzahl),
									type(transaction.anzahl),
									value(transaction.betrag),
									currency(transaction.betrag),
									value(instrument),
									value(transaction));
							transactionIndex++;
						}
						instrumentIndex++;
					}
					entryIndex++;
				}
				Logger.info("HBCI Depotumsätze als CSV gespeichert: " + file.getAbsolutePath());
			}
		} catch (Exception e) {
			Logger.error("unable to write HBCI depot transaction CSV dump", e);
		}
	}

	private static CSVPrinter printer(File file) throws Exception
	{
		Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
		return new CSVPrinter(writer, CSV_FORMAT);
	}

	private static File file(String prefix, Konto konto) throws RemoteException
	{
		File dir = new File(Utils.getWorkingDir(DepotViewerPlugin.class), "hbci-dumps");
		if (!dir.exists() && !dir.mkdirs()) {
			Logger.warn("unable to create HBCI dump directory: " + dir.getAbsolutePath());
		}
		return new File(dir, prefix + "-" + konto.getID() + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv");
	}

	private static String value(BigDecimalValue value)
	{
		return value == null || value.getValue() == null ? "" : value.getValue().toPlainString();
	}

	private static String currency(BigDecimalValue value)
	{
		return value == null || value.getCurr() == null ? "" : value.getCurr();
	}

	private static String type(TypedValue value)
	{
		return value == null ? "" : Integer.toString(value.getType());
	}

	private static String value(Object value)
	{
		return value == null ? "" : value.toString();
	}
}
