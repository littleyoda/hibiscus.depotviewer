package de.open4me.depot.sql;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.open4me.depot.abruf.utils.Utils;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.server.AbstractDBSupportImpl;
import de.willuhn.jameica.hbci.server.DBSupportH2Impl;
import de.willuhn.jameica.hbci.server.DBSupportMySqlImpl;
import de.willuhn.jameica.hbci.server.HBCIDBServiceImpl;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class SQLUtils {

	private static AbstractDBSupportImpl driver = null;
	public static Connection getConnection() throws Exception {
		if (driver == null) {
			HBCIDBServiceImpl db = (HBCIDBServiceImpl) Application.getServiceFactory().lookup(HBCI.class,"database");
			driver = (AbstractDBSupportImpl) db.getDriver();
		}
		String url = driver.getJdbcUrl();
		if (driver instanceof de.willuhn.jameica.hbci.server.DBSupportMySqlImpl) {
			url += "&useServerPrepStmts=false&rewriteBatchedStatements=true";
		}
		return DriverManager.getConnection(url, driver.getJdbcUsername(), driver.getJdbcPassword());
	}

	public static int delete(GenericObjectSQL obj) {
		int val = 0;
		try(Connection conn = getConnection()) {
			Utils.markRecalc(null);
			String sql = "DELETE FROM " + obj.getTable() + " WHERE " + obj.getIdfeld() +  "=?";
			try(PreparedStatement prest = conn.prepareStatement(sql)) {
				prest.setString(1, obj.getID());
				val = prest.executeUpdate();
			}
		} catch (Exception e) {
			Logger.error("Fehler beim Löschen", e);
		}
		return val;
	}

	public static List<GenericObjectSQL> getResultSet(String query, String table, String idfeld) {
		return getResultSet(query, table, idfeld, idfeld);
	}

	public static List<GenericObjectSQL> getResultSet(Connection conn, String query, String table, String idfeld) {
		return getResultSet(conn, query, table, idfeld, idfeld);
	}

	public static List<GenericObjectSQL> getResultSet(PreparedStatement statement, String table, String idfeld, String pa) {
		List<GenericObjectSQL> list = new ArrayList<GenericObjectSQL>();
		try(ResultSet ret = statement.executeQuery()) {
			while (ret.next()) {
				list.add(new GenericObjectSQL(idfeld, pa, table, ret));
			}
		} catch (Exception e) {
			Logger.error("Fehler bei der SQL Anweisung: " + statement.toString(), e);
		}
		return list;
	}
	public static List<GenericObjectSQL> getResultSet(String query, String table, String idfeld, String pa) {
		List<GenericObjectSQL> list = new ArrayList<GenericObjectSQL>();
		try(Connection conn = getConnection()) {
			try(Statement statement = conn.createStatement()) {
				return getResultSet(conn, query, table, idfeld, pa);
			}
		} catch (Exception e) {
			Logger.error("Fehler bei der SQL Anweisung: " + query, e);
		}
		return list;
	}

	public static List<GenericObjectSQL> getResultSet(Connection conn, String query, String table, String idfeld, String pa) {
		List<GenericObjectSQL> list = new ArrayList<GenericObjectSQL>();
		try(Statement statement = conn.createStatement(); ResultSet ret = statement.executeQuery(query)) {
			while (ret.next()) {
				list.add(new GenericObjectSQL(idfeld, pa, table, ret));
			}
		} catch (Exception e) {
			Logger.error("Fehler bei der SQL Anweisung: " + query, e);
		}
		return list;
	}


	public static void saveCfg(String key, String value) throws Exception {

		try(PreparedSQL preparedSQL = getPreparedSQL("insert into depotviewer_cfg (key, value) values (?,?)");) {
			preparedSQL.prest.setString(1, key);
			preparedSQL.prest.setString(2, value);
			preparedSQL.prest.executeUpdate();
			Logger.info("Speichere " + key + ":" + value);
		}
	}

	public static String getCfg(String key) {
		try(PreparedSQL preparedSQL = SQLUtils.getPreparedSQL("select value from depotviewer_cfg where `key`=?");) {
			preparedSQL.prest.setString(1, key);
			try(ResultSet ret =  preparedSQL.prest.executeQuery()) {
				if (!ret.next()) {
					return null;
				}
				return ret.getString(1);
			}
		} catch (Exception e) {
			Logger.error("Fehler beim Zugriff auf cfg", e);
			e.printStackTrace();
			return null;
		}
	}
	private static int getCurrentDBVersion() {
		int version = -1;
		try(Connection conn = getConnection();Statement statement = conn.createStatement();ResultSet ret = statement.executeQuery("select value from depotviewer_cfg where `key`='dbversion'");) {
			ret.next();
			version = Integer.parseInt(ret.getString(1));
		} catch (Exception e) {
			Logger.error("Fehler beim der Bestimmung der dbversion des DeportViewers", e);
		}
		return version;
	}

	/**
	 * Liefert das erste Objekt der ersten Zeile zurück.
	 *
	 * Falls die Query überhaupt kein Ergebnisse zurückliefert hat, wird null zurückgeliefert
	 *
	 * @param statement
	 * @return
	 */
	public static Object getObject(PreparedStatement statement) {
		Object obj = null;
		try(ResultSet ret = statement.executeQuery()) {
			if (!ret.next()) {
				return null;
			}
			obj = ret.getObject(1);
		} catch (Exception e) {
			Logger.error("Fehler beim Query", e);
		}
		return obj;
	}


	public static void checkforupdates() throws ApplicationException {
		List<SQLChange> liste = SQLChange.getChangesSinceVersion(getCurrentDBVersion());
		try(Connection conn = getConnection();) {
			for (SQLChange changeset : liste) {
				Logger.info("Depot-Viewer: Updating DB to " + changeset.getVersion());
				conn.setAutoCommit(false);
				try(Statement statement = conn.createStatement()) {
					for (String query : changeset.getQuery()) {
						statement.execute(query);
					}
					statement.execute("update depotviewer_cfg set value = " + changeset.getVersion() + " where `key`='dbversion'");
					conn.commit();
				} catch (Exception e) {
					Logger.error("Fehler bei Aktualisierung der Datenbank", e);
					if (conn != null) {
						try {
							conn.rollback();
						} catch (SQLException e1) {
							e1.printStackTrace();
						}
					}
					throw e;
				}
			}
		} catch (Exception e2) {
			throw new ApplicationException(e2);
		}
	}

	public static String getDateDiff(String value1, String value2) throws ApplicationException {
		if (driver instanceof DBSupportH2Impl) {
			return "datediff('day'," + value1 + ", "+value2+")";
		}

		if (driver instanceof DBSupportMySqlImpl) {
			return "datediff(" + value1 + ", "+value2+")";
		}
		throw new ApplicationException("Unbekannte Datenbank " + driver.getClass().getName());
	}

	public static void exec(String sql) throws ApplicationException {
		try {
			HBCIDBServiceImpl db = (HBCIDBServiceImpl) Application.getServiceFactory().lookup(HBCI.class,"database");
			AbstractDBSupportImpl driver = (AbstractDBSupportImpl) db.getDriver();
			try( Connection conn = DriverManager.getConnection(driver.getJdbcUrl(), driver.getJdbcUsername(), driver.getJdbcPassword()); Statement statement = conn.createStatement()){
				statement.execute(sql);
			}
		} catch (Exception e) {
			Logger.error("Fehler beim der Ausführung: " + sql, e);
			throw new ApplicationException(e);
		}
	}

	public static PreparedSQL getPreparedSQL(String query) throws Exception {
		Connection conn = SQLUtils.getConnection();
		return new PreparedSQL(conn, conn.prepareStatement(query));
	}

	public static PreparedStatement getPreparedSQL(Connection conn, String query) throws Exception {
		return conn.prepareStatement(query);
	}

	public static String addTop(int i, String string) throws ApplicationException {
		if (driver instanceof DBSupportH2Impl) {
			return string.replace("select ",  "select top " + i);
		}

		if (driver instanceof DBSupportMySqlImpl) {
			return string + " limit " + i;
		}
		throw new ApplicationException("Unbekannte Datenbank " + driver.getClass().getName());
	}
	public static Date getSQLDate(java.util.Date d) {
		return new java.sql.Date(d.getTime());
	}

	/**
	 * Group a List of GenericObjectSQL by one of its attributes, so that the map key is the attribute value
	 * and the Map entries are lists of corresponding objects.
	 * @param <T> Type of the attribute, e.g. String or Integer
	 * @param list List to group
	 * @param attribute Attribute to group by
	 * @return A map from the attribute value to all corresponding entries from the list.
	 * @throws RemoteException
	 */
	@SuppressWarnings("unchecked")
	public static <T> Map<T, List<GenericObjectSQL>> groupBy(List<GenericObjectSQL> list, String attribute) throws RemoteException {
		HashMap<T, List<GenericObjectSQL>> ret = new HashMap<T, List<GenericObjectSQL>>();
		for(GenericObjectSQL item : list) {
			T key = (T)item.getAttribute(attribute);
			if(!ret.containsKey(key)) {
				ret.put(key, new ArrayList<GenericObjectSQL>());
			}
			ret.get(key).add(item);
		}
		return ret;
	}

	public static class PreparedSQL implements AutoCloseable {
		public final Connection conn;
		public final PreparedStatement prest;
		protected PreparedSQL(Connection conn, PreparedStatement prest) {
			this.conn = conn;
			this.prest = prest;
		}
		@Override
		public void close() throws Exception {
			Exception e = null;
			if ( Objects.nonNull(prest)) {
				try {
					prest.close();
				} catch( Exception ee) {
					e=ee;
				}
			}
			if ( Objects.nonNull(conn)) {
				try {
					conn.close();
				}catch( Exception ee) {
					e=ee;
				}
			}
			if (Objects.nonNull(e)) {
				throw e;
			}
		}

	}
}
