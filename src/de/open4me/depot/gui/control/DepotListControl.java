package de.open4me.depot.gui.control;

import java.rmi.RemoteException;

import de.open4me.depot.Settings;
import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.dialogs.ExtendedSynchronizeOptionsDialog;
import de.open4me.depot.sql.GenericObjectHashMap;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.hbci.gui.dialogs.SynchronizeOptionsDialog;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class DepotListControl extends AbstractControl
{

	private TablePart orderList;

	public DepotListControl(AbstractView view) {
		super(view);
	}


	public Part getDepotOverview() throws RemoteException, ApplicationException
	{
		if (orderList != null) {
			return orderList;
		}

		orderList = new TablePart(Utils.getDepotKonten(), new Action() {
			public void handleAction(Object context) throws ApplicationException {
					exec(context);
			}
		});
		orderList.setRememberColWidths(true);
		orderList.setRememberOrder(true);
		orderList.addColumn(Settings.i18n().tr("Depot"), "bezeichnung");
		orderList.addColumn(Settings.i18n().tr("BLZ"), "blz");
		orderList.addColumn(Settings.i18n().tr("Konto"), "kontonummer");
		orderList.addColumn(Settings.i18n().tr("Zugangsart"), "zugangsart"); 
		return orderList;
	}


	public Object getSelectedItem() {
		return orderList.getSelection();
	}
	
	public void exec(Object context) throws ApplicationException {
		GenericObjectHashMap obj = (GenericObjectHashMap) context;
		try
		{
			ExtendedSynchronizeOptionsDialog d = new ExtendedSynchronizeOptionsDialog(obj,SynchronizeOptionsDialog.POSITION_CENTER);
			d.open();
		}
		catch (OperationCanceledException oce)
		{
			Logger.info(oce.getMessage());
			return;
		}
		catch (ApplicationException ae)
		{
			throw ae;
		}
		catch (Exception e)
		{
			Logger.error("unable to configure synchronize options");
			Application.getMessagingFactory().sendMessage(new StatusBarMessage("Fehler beim Konfigurieren der Synchronisierungsoptionen",StatusBarMessage.TYPE_ERROR));
		}
	}
}