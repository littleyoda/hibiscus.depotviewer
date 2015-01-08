package de.open4me.depot.gui.parts;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;

public class TabFolderExt extends TabFolder {

	private Map<String, TabGroupExt> tabs = new HashMap<String, TabGroupExt>();
	
	public TabFolderExt(Composite parent, int style) {
		super(parent, style);
		addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String name = getSelection()[0].getText(); 
				TabGroupExt tab = tabs.get(name);
				tab.active();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});

	}

	public void addTab(TabGroupExt tabGroupExt, String name) {
		tabs.put(name, tabGroupExt);
	}

	@Override
	protected void checkSubclass() {
	}

	public void doNotify() {
		String name = getSelection()[0].getText(); 
		TabGroupExt tab = tabs.get(name);
		tab.active();
	}
	
	

}
