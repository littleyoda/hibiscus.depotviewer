package de.open4me.depot.gui.control;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.parts.FormTextPart;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;

public class LizenzinformationControl extends AbstractControl {

	  private Part textPart = null;

	  public LizenzinformationControl(AbstractView view) {
	    super(view);
	  }

	  public Part getTextPart() throws ApplicationException
	  {
	    if (textPart != null)
	      return textPart;
		String path = "de/open4me/depot/gui/control/LizenzinformationControl.txt";
		InputStream is  = Application.getClassLoader().getResourceAsStream(path);
		if (is == null) {
			throw new ApplicationException("LizenzinformationControl.txt wurde nicht gefunden!");
		}
	    StringBuffer buffer = new StringBuffer();
	    buffer.append("<form>");
		try {
			String inhalt = IOUtils.toString(is, "ISO-8859-1");
			for (String x: inhalt.split("\n")) {
				String[] values = x.split(";");
				buffer.append(
			    		"Name: " + values[0] + "<br/>\n" +
					    "WWW: " + values[1] + "<br/>\n" +
					    "Lizenz: " + values[2] + "<br/>\n" +
					    "Lizenz: " + values[3] + "<br/>\n" +
			    	    "=============================================================================<br/>\n"  
						);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//
//	    buffer.append(
//	    		"File: java-stock-quotes-*.jar<br/>" +
//	    	    "<br/>" +
//	    		"Java Stock Quotes<br/>" +
//	    		"<br/>" + 
//	    		"http://github.com/mikekorb/JavaStockQuotes<br/>" +
//				"Apache 2.0 - http://www.apache.org/licenses/LICENSE-2.0<br/>" + 
//	    		"<br/>" + 
//	    		"=============================================================================<br/>" + 
//	    		"File: commons-csv-*-SNAPSHOT.jar<br/>" + 
//	    		"<br/>" + 
//	    		"Apache Commons CSV<br/>" + 
//	    		"Copyright 2005-2014 The Apache Software Foundation<br/>" + 
//	    		"<br/>" + 
//	    		"This product includes software developed at<br/>" + 
//	    		"The Apache Software Foundation (http://www.apache.org/).<br/>" + 
//	    		"<br/>" + 
//	    		"src/main/resources/contract.txt<br/>" + 
//	    		"This file was downloaded from<br/>" + 
//	    		"http://www.ferc.gov/docs-filing/eqr/soft-tools/sample-csv/contract.txt and<br/>" + 
//	    		"contains neither copyright notice nor license.<br/>" + 
//	    		"<br/>" + 
//	    		"src/main/resources/transaction.txt<br/>" + 
//	    		"This file was downloaded from<br/>" + 
//	    		"http://www.ferc.gov/docs-filing/eqr/soft-tools/sample-csv/transaction.txt<br/>" + 
//	    		"and contains neither copyright notice nor license.<br/>" + 
//	    		"<br/>" + 
//	    		"http://commons.apache.org/<br/>" + 
//	    		"<br/>" + 
//	    		"=============================================================================<br/>" + 
//	    		"Files: jcommon-*.jar<br/>" + 
//	    		"JCommon is licensed under the terms of the GNU Lesser General Public Licence<br/>" + 
//	    		"(LGPL) version 2.1 or later.<br/>" + 
//	    		"<br/>" + 
//	    		"http://www.jfree.org/jcommon/<br/>" + 
//	    		"<br/>" + 
//	    		"=============================================================================<br/>" + 
//	    		"Files: jfreechart-*.jar, jfreechart-*-swt.jar, swtgraphics2d.jar<br/>" + 
//	    		"<br/>" + 
//	    		"JFreeChart is licensed under the terms of the GNU Lesser General<br/>" + 
//	    		"Public Licence (LGPL).  A copy of the licence is included in the<br/>" + 
//	    		"distribution.<br/>" + 
//	    		"<br/>" + 
//	    		"Please note that JFreeChart is distributed WITHOUT ANY WARRANTY;<br/>" + 
//	    		"without even the implied warranty of MERCHANTABILITY or FITNESS FOR A<br/>" + 
//	    		"PARTICULAR PURPOSE.  Please refer to the licence for details.<br/>" + 
//	    		"<br/>" + 
//	    		"http://www.jfree.org/jfreechart/<br/>" + 
//	    		"<br/>" + 
//	    		"=============================================================================<br/>" +
//	    		"File: ffb.depot.client-0.2.0-SNAPSHOT.jar<br/>" + 
//	    		"Copyright 2018<br/>" + 
//	    		"*<br/>" + 
//	    		"Licensed under the Apache License, Version 2.0 (the \"License\");<br/>" + 
//	    		"you may not use this file except in compliance with the License.<br/>" + 
//	    		"You may obtain a copy of the License at<br/>" + 
//	    		"*<br/>" + 
//	    		"http://www.apache.org/licenses/LICENSE-2.0<br/>" + 
//	    		"*<br/>" + 
//	    		"Unless required by applicable law or agreed to in writing, software<br/>" + 
//	    		"distributed under the License is distributed on an \"AS IS\" BASIS,<br/>" + 
//	    		"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br/>" + 
//	    		"See the License for the specific language governing permissions and<br/>" + 
//	    		"limitations under the License.<br/>" ); 

	    buffer.append("</form>");
	    System.out.println(buffer.toString());
	    textPart = new FormTextPart(buffer.toString());
	    return textPart;
	  }
	}
