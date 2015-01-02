package database;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import de.open4me.depot.sql.SQLChange;
import de.willuhn.logging.Logger;

public class DataBaseCreation {

	@Test
	public void runH2() throws ClassNotFoundException, SQLException, IOException {
		File file = File.createTempFile("depotviewer", "test");
		file.delete(); // Not safe, but in this case ok
		test("org.h2.Driver","jdbc:h2:" + file.getAbsolutePath(), "", "");
	}

	@Test
	public void runMYSQL() throws ClassNotFoundException, SQLException, IOException {
			FileReader reader = new FileReader( "credential.txt" );

		  Properties credentail = new Properties();
		  credentail.load( reader );
			reader.close();

		String dbname = createTempDB("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/", credentail.getProperty("user"), credentail.getProperty("pwd"));
		test("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/" + dbname + "?useUnicode=Yes&characterEncoding=ISO8859_1", "hibiscus", "hibiscus");
	}

	private String createTempDB(String classname, String jdbc, String user, String pwd) throws ClassNotFoundException, SQLException {
		Class.forName(classname);
		String dbname = "hibiscus_test_" + System.currentTimeMillis();
		Connection conn = DriverManager.getConnection(jdbc, user, pwd);
		conn.setAutoCommit(false);
		Statement statement = conn.createStatement();
		statement.execute("create database " + dbname + ";");
		// CREATE USER 'hibiscus'@'localhost' IDENTIFIED BY 'hibiscus';
		statement.execute("GRANT ALL PRIVILEGES ON " + dbname + ".* TO 'hibiscus'@'localhost';");
		return dbname;
	
	}

	/**
	 * Alle Tabellen innerhalb dieser Datenbank löschen
	 * @param classname
	 * @param jdbc
	 * @param user
	 * @param pwd
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public void clearMysql(String classname, String jdbc, String user, String pwd) throws ClassNotFoundException, SQLException {
		Class.forName(classname);
		Connection conn = DriverManager.getConnection(jdbc, user, pwd);
		conn.setAutoCommit(false);
		Statement statement = conn.createStatement();
		statement.execute("SET FOREIGN_KEY_CHECKS = 0;");
		ResultSet ret = statement.executeQuery("SELECT concat('DROP TABLE IF EXISTS ', table_name, ';')\n" + 
				"FROM information_schema.tables\n" + 
				"WHERE table_schema = 'hibiscus';");
		List<String> queries = new ArrayList<String>();
		while (ret.next()) {
			System.out.println();
			queries.add(ret.getString(1));
		}
		for (String s : queries) {
			statement.execute(s);
		}
		ret.close();
		
		statement.execute("SET FOREIGN_KEY_CHECKS = 1;");
		conn.commit();
	}

	public void test(String classname, String jdbc, String user, String pwd) throws ClassNotFoundException, SQLException {
		Class.forName(classname);



		List<SQLChange> liste = SQLChange.getChangesSinceVersion(0);
		Connection conn = null;

		conn = DriverManager.getConnection(jdbc, user, pwd);
		conn.setAutoCommit(false);
		Statement statement = conn.createStatement();
		statement.execute(
				"create table konto (\n" + 
						"  id int NOT NULL auto_increment,\n" + 
						"  PRIMARY KEY (id)\n" + 
				");");
		conn.commit();
		conn.close();

		try {
			for (SQLChange changeset : liste) {
				Logger.info("Depot-Viewer: Updating DB to " + changeset.getVersion());
				conn = DriverManager.getConnection(jdbc, user, pwd);
				conn.setAutoCommit(false);
				statement = conn.createStatement();
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
			throw e;
		}
	}
}
