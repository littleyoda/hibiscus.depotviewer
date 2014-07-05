package de.open4me.depot.gui;

import java.util.ArrayList;
import java.util.Date;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.willuhn.jameica.gui.input.ScaleInput;

public class DatumsSlider extends ScaleInput {

	private Listener listener = new RangeListener();
	private ArrayList<Date> dates;

	public DatumsSlider(ArrayList<Date> dates) {
		super(0);
		this.dates = dates;
		setScaling(0, dates.size(), 1, dates.size() / 10);
		setValue(dates.size());
		addListener(listener);
		listener.handleEvent(null); // einmal initial ausloesen

	}

	public Date getDate() {
		int start = ((Integer)getValue()).intValue();
		if (start >= dates.size()) {
			return null;
		} else {
			return dates.get(start);
		}
	}
	
	private class RangeListener implements Listener
	{
		public void handleEvent(Event event)
		{
			if (dates == null) {
				return;
			}
			int start = ((Integer)getValue()).intValue();
			if (start >= dates.size()) {
				setComment("Heute");
			} else {
				setComment("" + dates.get(start));
			}
		}
	}

}
