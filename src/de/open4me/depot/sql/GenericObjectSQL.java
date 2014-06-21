package de.open4me.depot.sql;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;

import de.willuhn.datasource.GenericObject;

public class GenericObjectSQL implements GenericObject {

	
	private HashMap<String, Object> data;
	private String idfeld;
	private String table;
	private String pa;

	public String getIdfeld() {
		return idfeld;
	}


	public String getTable() {
		return table;
	}

	public GenericObjectSQL(String idfeld, String primaryattr, String table, ResultSet ret) throws SQLException {
		this(idfeld, table, ret);
		pa = primaryattr;
	}

	public GenericObjectSQL(String idfeld, String table, ResultSet ret) throws SQLException {
		this.idfeld = idfeld;
		this.table = table;
		this.pa = idfeld;
		ResultSetMetaData rsmd = ret.getMetaData();
		data = new HashMap<String, Object>();
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String name = rsmd.getColumnLabel(i).toLowerCase();
			// Doppelte Spaltennamen ggf. umbenennen
			if (data.containsKey(name)) {
				int nr = 0;
				while (data.containsKey(name + nr)) {
					nr++;
				}
				name = (name + nr);
			}
			data.put(name, ret.getObject(i));
		}
	}

	@Override
	public Object getAttribute(String name) throws RemoteException {
		return data.get(name);
	}

	@Override
	public String[] getAttributeNames() throws RemoteException {
		return data.keySet().toArray(new String[0]);
	}

	@Override
	public String getID() throws RemoteException {
		return data.get(idfeld).toString();
	}

	@Override
	public String getPrimaryAttribute() throws RemoteException {
		return pa;
	}

	@Override
	public boolean equals(GenericObject other) throws RemoteException {
		return false;
	}
	
	@Override
	public String toString() {
		return data.entrySet().toString();
	}

	public boolean isEmpty(String s) throws RemoteException {
		Object o = getAttribute(s);
		return  (o == null || o.toString().isEmpty());
	}
}
