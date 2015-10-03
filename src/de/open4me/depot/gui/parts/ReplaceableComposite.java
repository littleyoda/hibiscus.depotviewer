package de.open4me.depot.gui.parts;

import java.rmi.RemoteException;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.util.SWTUtil;

public class ReplaceableComposite extends Composite {

	public ReplaceableComposite(Composite parent, int style) {
		super(parent, style);
		setLayoutData(new GridData(GridData.FILL_BOTH));
		setLayout(new GridLayout());

	}
	
	public void replace(Part tab) {
		SWTUtil.disposeChildren(this);
		setLayoutData(new GridData(GridData.FILL_BOTH));
		setLayout(new GridLayout());
		try {
			tab.paint(this);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		layout(true);
	}

	public void replace(Composite comp) {
//		SWTUtil.disposeChildren(this);
//		setLayoutData(new GridData(GridData.FILL_BOTH));
//		setLayout(new GridLayout());
//		try {
//			comp.paint(this);
//		} catch (RemoteException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		layout(true);
	}

	public void replace(AbstractView rv) {
		SWTUtil.disposeChildren(this);
		setLayoutData(new GridData(GridData.FILL_BOTH));
		setLayout(new GridLayout());
		rv.setParent(this);
		try {
			rv.bind();
		} catch (Exception e) {
			e.printStackTrace();
		}
		layout(true);
	}
}
