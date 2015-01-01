package de.open4me.depot.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
		return DriverManager.getConnection(driver.getJdbcUrl(), driver.getJdbcUsername(), driver.getJdbcPassword());
	}

	public static int delete(GenericObjectSQL obj) {
		int val = 0;
		Connection conn = null;
		try {
			Utils.markRecalc(null);
			conn = getConnection();
			String sql = "DELETE FROM " + obj.getTable() + " WHERE " + obj.getIdfeld() +  "=?";
			PreparedStatement prest = conn.prepareStatement(sql);
			prest.setString(1, obj.getID());
			val = prest.executeUpdate();
			conn.close();
		} catch (Exception e) {
			Logger.error("Fehler beim Löschen", e);
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e1) {
				}
			}
		}
		return val;
	}
	
	public static List<GenericObjectSQL> getResultSet(String query, String table, String idfeld) {
		return getResultSet(query, table, idfeld, idfeld);
	}

	public static List<GenericObjectSQL> getResultSet(PreparedStatement statement, String table, String idfeld, String pa) {
		List<GenericObjectSQL> list = new ArrayList<GenericObjectSQL>();
		try {
			ResultSet ret = statement.executeQuery();
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
		Connection conn = null;
		try {
			conn = getConnection();
			Statement statement = conn.createStatement();
			ResultSet ret = statement.executeQuery(query);
			while (ret.next()) {
				list.add(new GenericObjectSQL(idfeld, pa, table, ret));
			}
			conn.close();
		} catch (Exception e) {
			Logger.error("Fehler bei der SQL Anweisung: " + query, e);
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e1) {
				}
			}
		}
		return list;
	}

	private static int getCurrentDBVersion() {
		int version = -1;
		Connection conn = null;
		try {
			conn = getConnection();
			Statement statement = conn.createStatement();
			ResultSet ret = statement.executeQuery("select value from depotviewer_cfg where `key`='dbversion'");
			ret.next();
			version = Integer.parseInt(ret.getString(1));
			conn.close();
		} catch (Exception e) {
			Logger.error("Fehler beim der Bestimmung der dbversion des DeportViewers", e);
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e1) {
				}
			}
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
		Connection conn = null;
		Object obj = null;
		try {
			conn = getConnection();
			ResultSet ret = statement.executeQuery();
			if (!ret.next()) {
				return null;
			}
			obj = ret.getObject(1);
			conn.close();
		} catch (Exception e) {
			Logger.error("Fehler beim Query", e);
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e1) {
				}
			}
		}
		return obj;
	}

	
	public static void checkforupdates() throws ApplicationException {
		List<SQLChange> liste = SQLChange.getChangesSinceVersion(getCurrentDBVersion());
		Connection conn = null;
		try {
			for (SQLChange changeset : liste) {
				Logger.info("Depot-Viewer: Updating DB to " + changeset.getVersion());
				conn = getConnection();
				conn.setAutoCommit(false);
				Statement statement = conn.createStatement();
				for (String query : changeset.getQuery()) {
					statement.execute(query);
				}
				statement.execute("update depotviewer_cfg set value = " + changeset.getVersion() + " where `key`='dbversion'");
				conn.commit();
				conn.close();
			}
		} catch (Exception e) {
			Logger.error("Fehler bei Aktualisierung der Datenbank", e);
			if (conn != null) {
				try {
					conn.rollback();
					conn.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			throw new ApplicationException(e);
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
			Connection conn = DriverManager.getConnection(driver.getJdbcUrl(), driver.getJdbcUsername(), driver.getJdbcPassword());

			Statement statement = conn.createStatement();
			statement.execute(sql);
			conn.close();
		} catch (Exception e) {
			Logger.error("Fehler beim der Ausführung: " + sql, e);
			throw new ApplicationException(e);
		}
	}
	
	public static PreparedStatement getPreparedSQL(String query) throws Exception {
		Connection conn = SQLUtils.getConnection();
		PreparedStatement prest = conn.prepareStatement(query);
		return prest;
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

}
