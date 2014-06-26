package de.open4me.depot.gui.control;

import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.parts.FormTextPart;

public class LizenzinformationControl extends AbstractControl {

	  private Part textPart = null;

	  public LizenzinformationControl(AbstractView view) {
	    super(view);
	  }

	  public Part getTextPart()
	  {
	    if (textPart != null)
	      return textPart;

	    StringBuffer buffer = new StringBuffer();
	    buffer.append("<form>");
	    buffer.append(
	    		"File: java-stock-quotes-*.jar<br/>" +
	    	    "<br/>" +
	    		"Java Stock Quotes<br/>" +
	    		"<br/>" + 
	    		"http://github.com/mikekorb/JavaStockQuotes<br/>" +
				"Apache 2.0 - http://www.apache.org/licenses/LICENSE-2.0<br/>" + 
	    		"<br/>" + 
	    		"=============================================================================<br/>" + 
	    		"File: commons-csv-*-SNAPSHOT.jar<br/>" + 
	    		"<br/>" + 
	    		"Apache Commons CSV<br/>" + 
	    		"Copyright 2005-2014 The Apache Software Foundation<br/>" + 
	    		"<br/>" + 
	    		"This product includes software developed at<br/>" + 
	    		"The Apache Software Foundation (http://www.apache.org/).<br/>" + 
	    		"<br/>" + 
	    		"src/main/resources/contract.txt<br/>" + 
	    		"This file was downloaded from<br/>" + 
	    		"http://www.ferc.gov/docs-filing/eqr/soft-tools/sample-csv/contract.txt and<br/>" + 
	    		"contains neither copyright notice nor license.<br/>" + 
	    		"<br/>" + 
	    		"src/main/resources/transaction.txt<br/>" + 
	    		"This file was downloaded from<br/>" + 
	    		"http://www.ferc.gov/docs-filing/eqr/soft-tools/sample-csv/transaction.txt<br/>" + 
	    		"and contains neither copyright notice nor license.<br/>" + 
	    		"<br/>" + 
	    		"http://commons.apache.org/<br/>" + 
	    		"<br/>" + 
	    		"=============================================================================<br/>" + 
	    		"Files: jcommon-*.jar<br/>" + 
	    		"JCommon is licensed under the terms of the GNU Lesser General Public Licence<br/>" + 
	    		"(LGPL) version 2.1 or later.<br/>" + 
	    		"<br/>" + 
	    		"http://www.jfree.org/jcommon/<br/>" + 
	    		"<br/>" + 
	    		"=============================================================================<br/>" + 
	    		"Files: jfreechart-*.jar, jfreechart-*-swt.jar, swtgraphics2d.jar<br/>" + 
	    		"<br/>" + 
	    		"JFreeChart is licensed under the terms of the GNU Lesser General<br/>" + 
	    		"Public Licence (LGPL).  A copy of the licence is included in the<br/>" + 
	    		"distribution.<br/>" + 
	    		"<br/>" + 
	    		"Please note that JFreeChart is distributed WITHOUT ANY WARRANTY;<br/>" + 
	    		"without even the implied warranty of MERCHANTABILITY or FITNESS FOR A<br/>" + 
	    		"PARTICULAR PURPOSE.  Please refer to the licence for details.<br/>" + 
	    		"<br/>" + 
	    		"http://www.jfree.org/jfreechart/<br/>" + 
	    		"<br/>" + 
	    		"=============================================================================<br/>"); 
	    buffer.append("</form>");

	    textPart = new FormTextPart(buffer.toString());
	    return textPart;
	  }
	}
