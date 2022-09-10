package de.open4me.depot.gui.parts;


import org.eclipse.swt.SWT;

import de.willuhn.jameica.gui.util.TabGroup;

public class TabGroupExt extends TabGroup
{

	private ReplaceableComposite rc;
	private String name;

	public TabGroupExt(TabFolderExt parent, String name)
	{
		this(parent, name, true);
	}
	
	public TabGroupExt(TabFolderExt parent, String name, boolean t)
	{
		super(parent, name, true, 1);
		this.name = name;
		parent.addTab(this, name);
		if (t == true) {
			rc = new ReplaceableComposite(getComposite(),SWT.NONE);
		}
	}

	
	public void active() {
		
	}
	
	public String getName() {
		return name;
	}
	
	public ReplaceableComposite getReplaceableComposite() {
		return rc;
	}

}
