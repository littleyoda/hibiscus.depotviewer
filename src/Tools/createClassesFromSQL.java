package Tools;

import java.util.Scanner;

public class createClassesFromSQL {

	static String inpt =  		"CREATE TABLE depotviewer_wertpapier (\n" + 
			"  id NUMERIC NOT NULL auto_increment,\n" + 
			"  wpid NUMERIC,\n" + 
			"  wertpapiername varchar(255) NOT NULL,\n" + 
			"  wkn varchar(6) NOT NULL,\n" + 
			"  isin varchar(6) NOT NULL,\n" + 
			"  UNIQUE (id),\n" + 
			"  PRIMARY KEY (id)\n" + 
			");";
 
	public static void main(String[] args) {
		Scanner scanner = new Scanner(inpt);
		String  impl = "public class UmsatzImpl extends AbstractDBObject implements Umsatz\n" + 
				"{\n" + 
				"\n" + 
				"	/**\n" + 
				"   * @throws RemoteException\n" + 
				"   */\n" + 
				"  public UmsatzImpl() throws RemoteException\n" + 
				"  {\n" + 
				"	    super(); \n" + 
				"\n" + 
				"    }\n" + 
				"\n" + 
				"  /**\n" + 
				"   * We have to return the name of the sql table here.\n" + 
				"	 * @see de.willuhn.datasource.db.AbstractDBObject#getTableName()\n" + 
				"	 */\n" + 
				"	protected String getTableName()\n" + 
				"	{\n" + 
				"		return \"XXXXXXXXXXXX\";\n" + 
				"	}\n" + 
				"\n" + 
				"  /**\n" + 
				"   * Sometimes you can display only one of the projects attributes (in combo boxes).\n" + 
				"   * Here you can define the name of this field.\n" + 
				"   * Please dont confuse this with the \"primary KEY\".\n" + 
				"   * @see de.willuhn.datasource.GenericObject#getPrimaryAttribute()\n" + 
				"	 */\n" + 
				"	public String getPrimaryAttribute() throws RemoteException\n" + 
				"	{\n" + 
				"    // we choose the projects name as primary field.\n" + 
				"		return \"name\";\n" + 
				"	}\n" + 
				"";
		String interf = "public interface Umsatz extends DBObject\n" + 
				"{\n" + 
				"";
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();
			System.out.println(line);
			if (line.startsWith("create") || line.equals("")) {
				continue;
			}
			String[] s = line.split(" ");
			String name = s[0];
			if (s[0].equals("id") || s.length < 2) {
				continue;
			}
			name = name.substring(0, 1).toUpperCase() + name.substring(1);
			String type = s[1].toLowerCase();
			String vartype = "";
			if (type.startsWith("varchar")) {
				vartype = "String";
			} else if (type.startsWith("date")) {
				vartype = "Date";
			} else if (type.startsWith("decimal")) {
				vartype = "Double";
			} else if (type.startsWith("numeric")) {
				vartype = "Integer";
			} else {
				System.out.println("Unkannter Typ: " + type);
			}
			interf += "public " + vartype + " get" + name + "() throws RemoteException;\n";
			interf += "public void set" + name + "(" + vartype + " name) throws RemoteException;\n";
			//			public String getWertPapierName() throws RemoteException;
			//			public void setWertPapierName(String name) throws RemoteException;
			
			
			impl += "	public " + vartype + " get" + name + "() throws RemoteException\n" + 
			"	{\n" + 
			"		return (" + vartype + ") getAttribute(\"" + name.toLowerCase() + "\");\n" + 
			"	}\n" + 
			"	\n" + 
			"	public void set" + name + "(" + vartype + " name) throws RemoteException\n" + 
			"	{\n" + 
			"    setAttribute(\"" + name.toLowerCase() + "\",name);\n" + 
			"	}\n" + 
			"";

		}
		System.out.println("=======================interface");
		System.out.println(interf);

		System.out.println("=======================impl");
		System.out.println(impl);
		scanner.close();
	}

}
