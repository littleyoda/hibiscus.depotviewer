package de.open4me.depot.gui.action;

import java.util.List;

import de.open4me.depot.gui.dialogs.PortfolioPerformanceExportDialog;
import de.open4me.depot.gui.dialogs.PortfolioPerformanceExportDialog.ExportOptions;
import de.open4me.depot.tools.PortfolioPerformanceExporter;
import de.open4me.depot.tools.PortfolioPerformanceExporter.DepotInfo;
import de.open4me.depot.tools.PortfolioPerformanceExporter.ExportResult;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class PortfolioPerformanceExportAction implements Action
{
	@Override
	public void handleAction(Object context) throws ApplicationException
	{
		PortfolioPerformanceExporter exporter = new PortfolioPerformanceExporter();
		List<DepotInfo> depots = exporter.getDepots();
		if (depots.isEmpty()) {
			throw new ApplicationException("Keine Depots mit Umsätzen gefunden.");
		}

		ExportOptions options;
		try {
			PortfolioPerformanceExportDialog dialog = new PortfolioPerformanceExportDialog(depots);
			options = dialog.open();
		} catch (OperationCanceledException e) {
			Logger.info(e.getMessage());
			return;
		} catch (ApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new ApplicationException("Fehler beim Öffnen des Export-Dialogs", e);
		}
		if (options == null) {
			return;
		}

		ExportResult result = exporter.export(options.targetDir, options.depots, options.from, options.to);
		Application.getMessagingFactory().sendMessage(new StatusBarMessage(
				result.files + " CSV-Dateien mit " + result.rows + " Umsätzen exportiert.",
				StatusBarMessage.TYPE_SUCCESS));
	}
}
