package de.open4me.depot.sql;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import de.willuhn.datasource.GenericObject;

public class GenericObjectHashMap implements GenericObject {

	protected Map<String, Object> map;

	public Map<String, Object> getMap() {
		return map;
	}
	
	public GenericObjectHashMap() {
		map = new HashMap<String, Object>();
	}

	public GenericObjectHashMap(Map<String, Object> map) {
		this.map = map;
	}
	@Override
	public Object getAttribute(String name) throws RemoteException {
		return map.get(name);
	}

	public void setAttribute(String key, Object value)  {
		getMap().put(key, value);
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
