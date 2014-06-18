package de.open4me.depot.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.server.DBSupportH2Impl;
import de.willuhn.jameica.hbci.server.HBCIDBServiceImpl;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class SQLUtils {

	public static Connection getConnection() throws Exception {
		HBCIDBServiceImpl db = (HBCIDBServiceImpl) Application.getServiceFactory().lookup(HBCI.class,"database");
		DBSupportH2Impl driver = (DBSupportH2Impl) db.getDriver();
		return DriverManager.getConnection(driver.getJdbcUrl(), driver.getJdbcUsername(), driver.getJdbcPassword());
	}

	public static int delete(GenericObjectSQL obj) {
		int val = 0;
		Connection conn = null;
		try {
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
		List<GenericObjectSQL> list = new ArrayList<GenericObjectSQL>();
		Connection conn = null;
		try {
			conn = getConnection();
			Statement statement = conn.createStatement();
			ResultSet ret = statement.executeQuery(query);
			while (ret.next()) {
				list.add(new GenericObjectSQL(idfeld, table, ret));
			}
			conn.close();
		} catch (Exception e) {
			Logger.error("Fehler bei der SQL Anweisung", e);
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
			ResultSet ret = statement.executeQuery("select value from depotviewer_cfg where key='dbversion'");
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
				statement.execute("update depotviewer_cfg set value = " + changeset.getVersion() + " where key='dbversion'");
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

	public static void exec(String sql) throws ApplicationException {
		try {
			HBCIDBServiceImpl db = (HBCIDBServiceImpl) Application.getServiceFactory().lookup(HBCI.class,"database");
			DBSupportH2Impl driver = (DBSupportH2Impl) db.getDriver();
			Connection conn = DriverManager.getConnection(driver.getJdbcUrl(), driver.getJdbcUsername(), driver.getJdbcPassword());

			Statement statement = conn.createStatement();
			statement.execute(sql);
			conn.close();
		} catch (Exception e) {
			Logger.error("Fehler beim der Ausführung: " + sql, e);
			throw new ApplicationException(e);
		}
	}
}
