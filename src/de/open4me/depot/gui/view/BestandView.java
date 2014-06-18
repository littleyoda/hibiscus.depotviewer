package de.open4me.depot.gui.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.TabFolder;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.control.BestandPieChartControl;
import de.open4me.depot.gui.control.BestandTableControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.util.TabGroup;

public class BestandView extends AbstractView
{

	/**
	 * @see de.willuhn.jameica.gui.AbstractView#bind()
	 */
	public void bind() throws Exception {

		GUI.getView().setTitle(Settings.i18n().tr("Bestand"));

		
		final TabFolder folder = new TabFolder(getParent(), SWT.NONE);
//	    folder.addSelectionListener(new SelectionAdapter() {
//	      public void widgetSelected(SelectionEvent e)
//	      {
//	        if (folder.getSelectionIndex() == 1)
//	          control.handleRefreshChart();
//	      }
//	    });
	    folder.setLayoutData(new GridData(GridData.FILL_BOTH));
	    
	    TabGroup tg1 = new TabGroup(folder, "Tabellarisch");
		BestandTableControl control = new BestandTableControl(this);
		control.getProjectsTable().paint(tg1.getComposite());
		
	    
	    final TabGroup tg2 = new TabGroup(folder, "Graphisch");
	    tg2.getComposite().setLayout(new FillLayout());
	    BestandPieChartControl chart = new BestandPieChartControl(this);

	    chart.getBestandChart(tg2.getComposite());

	}
}