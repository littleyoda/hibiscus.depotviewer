package de.open4me.depot.sql;

import java.rmi.RemoteException;
import java.util.HashMap;

import de.willuhn.datasource.GenericObject;

public class GenericObjectHashMap implements GenericObject {

	protected HashMap<?, ?> map;

	public HashMap<?, ?> getMap() {
		return map;
	}
	
	public GenericObjectHashMap(HashMap<?, ?> map) {
		this.map = map;
	}
	@Override
	public Object getAttribute(String name) throws RemoteException {
		return map.get(name);
	}

	@Override
	public String[] getAttributeNames() throws RemoteException {
		return (String[]) map.keySet().toArray();
	}

	@Override
	public String getID() throws RemoteException {
		return null;
	}

	@Override
	public String getPrimaryAttribute() throws RemoteException {
		return null;
	}

	@Override
	public boolean equals(GenericObject other) throws RemoteException {
		if (!(other instanceof GenericObjectHashMap)) {
			return false;
		}
		return getMap().equals(((GenericObjectHashMap) other).getMap());
	}
	
	@Override
	public String toString() {
		return getMap().entrySet().toString();
	}

}
