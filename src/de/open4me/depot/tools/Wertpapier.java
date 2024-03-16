package de.open4me.depot.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;

import de.open4me.depot.sql.SQLUtils;
import de.willuhn.logging.Logger;

public class Wertpapier {

	private static String[] tables = { "depotviewer_bestand", "depotviewer_umsaetze" };
	private static String[][] tablesDelete = {
			{"depotviewer_bestand", "wpid" },
			{"depotviewer_umsaetze","wpid" },
			{"depotviewer_kurse", "wpid" },
			{"depotviewer_kursevent", "wpid" },
			{"depotviewer_wertpapier", "id" },
			{"depotviewer_cfgupdatestock", "wpid" }
	};

	public static void deleteWertpapier(String id) {
		try(Connection conn = SQLUtils.getConnection();) {
			for (String[] table : tablesDelete) {
				try(PreparedStatement pre = SQLUtils.getPreparedSQL(conn, "delete from " + table[0] + " where " + table [1] + " = ? ")) {
					pre.setString(1, id);
					pre.execute();
				}
			}
		} catch (Exception e) {
			Logger.error("Error", e);
		}
	}

	public static boolean isInUse(String id) {
		int sum = 0;
		try(Connection conn = SQLUtils.getConnection();) {
			for (String table : tables) {
				try(PreparedStatement pre = SQLUtils.getPreparedSQL(conn, "select count(wpid) as anz  from " + table + " where wpid = ? ");) {
					pre.setString(1, id);
					Number count = (Number) SQLUtils.getObject(pre);
					sum += count.longValue();
				} catch (Exception e) {
					e.printStackTrace();
					sum++;
				}
			}
		} catch (Exception e) {
			Logger.error("Error", e);
		}
		return sum > 0;
	}

}
