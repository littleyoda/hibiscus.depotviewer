package de.open4me.depot.gui.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import de.open4me.depot.tools.PortfolioPerformanceExporter.DepotInfo;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.input.CheckboxInput;
import de.willuhn.jameica.gui.input.DateInput;
import de.willuhn.jameica.gui.input.DirectoryInput;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.jameica.system.Settings;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class PortfolioPerformanceExportDialog extends AbstractDialog<PortfolioPerformanceExportDialog.ExportOptions>
{
	private static final String SETTINGS_LAST_DIR = "portfolioPerformanceExport.lastDir";

	private final List<DepotInfo> depots;
	private final Map<DepotInfo, CheckboxInput> depotInputs = new LinkedHashMap<DepotInfo, CheckboxInput>();
	private final Settings settings = new Settings(PortfolioPerformanceExportDialog.class);
	private DirectoryInput targetDir;
	private DateInput dateFrom;
	private DateInput dateTo;
	private ExportOptions result;

	public PortfolioPerformanceExportDialog(List<DepotInfo> depots)
	{
		super(POSITION_CENTER);
		this.depots = depots;
		setTitle("Export für Portfolio Performance");
		setSize(650, SWT.DEFAULT);
	}

	@Override
	protected void paint(Composite parent) throws Exception
	{
		Container group = new SimpleContainer(parent);
		group.addText("Bitte wählen Sie das Zielverzeichnis und die Depots für den Export aus.", true);
		group.addText("Hinweis: Vorhandene CSV-Dateien mit gleichem Namen werden überschrieben.", true);
		group.addLabelPair("Zielverzeichnis", getTargetDir());
		group.addText("Optionaler Zeitraum:", false);
		group.addLabelPair("von", getDateFrom());
		group.addLabelPair("bis", getDateTo());
		group.addText("Zu exportierende Depots:", false);

		for (DepotInfo depot : depots) {
			CheckboxInput input = new CheckboxInput(true);
			input.setName(depot.getDisplayName());
			depotInputs.put(depot, input);
			group.addInput(input);
		}

		ButtonArea buttons = new ButtonArea();
		buttons.addButton("Exportieren", new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				File dir = new File(String.valueOf(getTargetDir().getValue()));
				if (getSelectedDepots().isEmpty()) {
					throw new ApplicationException("Bitte wählen Sie mindestens ein Depot aus.");
				}
				if (dir.exists() && !dir.isDirectory()) {
					throw new ApplicationException("Das Ziel ist kein Verzeichnis: " + dir);
				}
				Date from = (Date) getDateFrom().getValue();
				Date to = (Date) getDateTo().getValue();
				if (from != null && to != null && from.after(to)) {
					throw new ApplicationException("Das Von-Datum darf nicht nach dem Bis-Datum liegen.");
				}
				result = new ExportOptions(dir, getSelectedDepots(), from, to);
				try {
					settings.setAttribute(SETTINGS_LAST_DIR, dir.getAbsolutePath());
				} catch (Exception e) {
					Logger.warn("Portfolio-Performance-Export: Zielverzeichnis konnte nicht gespeichert werden: "
							+ e.getMessage());
				}
				close();
			}
		}, null, true, "ok.png");
		buttons.addButton("Abbrechen", new Action() {
			public void handleAction(Object context) throws ApplicationException
			{
				throw new OperationCanceledException("Abgebrochen");
			}
		}, null, false, "process-stop.png");
		group.addButtonArea(buttons);
	}

	private DirectoryInput getTargetDir()
	{
		if (targetDir != null) {
			return targetDir;
		}
		String fallback = System.getProperty("user.home");
		String lastDir = settings.getString(SETTINGS_LAST_DIR, fallback);
		File dir = new File(lastDir);
		if (!dir.exists() || !dir.isDirectory()) {
			dir = new File(fallback);
		}
		targetDir = new DirectoryInput(dir.getAbsolutePath());
		targetDir.setName("Zielverzeichnis");
		return targetDir;
	}

	private DateInput getDateFrom()
	{
		if (dateFrom == null) {
			dateFrom = new DateInput(null, de.open4me.depot.Settings.DATEFORMAT);
			dateFrom.setName("von");
		}
		return dateFrom;
	}

	private DateInput getDateTo()
	{
		if (dateTo == null) {
			dateTo = new DateInput(null, de.open4me.depot.Settings.DATEFORMAT);
			dateTo.setName("bis");
		}
		return dateTo;
	}

	private List<DepotInfo> getSelectedDepots()
	{
		List<DepotInfo> selected = new ArrayList<DepotInfo>();
		for (Map.Entry<DepotInfo, CheckboxInput> entry : depotInputs.entrySet()) {
			if (Boolean.TRUE.equals(entry.getValue().getValue())) {
				selected.add(entry.getKey());
			}
		}
		return selected;
	}

	@Override
	protected ExportOptions getData() throws Exception
	{
		return result;
	}

	public static class ExportOptions
	{
		public final File targetDir;
		public final List<DepotInfo> depots;
		public final Date from;
		public final Date to;

		public ExportOptions(File targetDir, List<DepotInfo> depots, Date from, Date to)
		{
			this.targetDir = targetDir;
			this.depots = depots;
			this.from = from;
			this.to = to;
		}
	}
}
