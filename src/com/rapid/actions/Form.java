/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as 
published by the Free Software Foundation, either version 3 of the 
License, or (at your option) any later version. The terms require you 
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.actions;

import java.net.URLEncoder;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.forms.FormAdapter;
import com.rapid.forms.FormAdapter.UserFormDetails;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

import org.json.JSONObject;

/*

This action runs JQuery against a specified control. Can be entered with or without the leading "." Such as hide(), or .css("disabled","disabled");

*/

public class Form extends Action {

	// parameterless constructor (required for jaxb)
	Form() { super(); }
	// designer constructor
	public Form(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super(rapidServlet, jsonAction);				
	}
	
	// methods
		
	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, com.rapid.core.Control control, JSONObject jsonDetails) throws Exception {

		// get the action type
		String actionType = getProperty("actionType");
		// prepare the js
		String js = "";
		// get the form adpater
		FormAdapter formAdapter = application.getFormAdapter();
		// check we got one
		if (formAdapter == null) {
			js = "// no form adapter\n";
		} else {
			// check the action type
			if ("next".equals(actionType)) {
				// next submits the form
				js = "$('form').submit();\n";
			} else if ("prev".equals(actionType)) {
				// go back
				js = "window.history.back();\n";
			} else {
				// get the dataDestination
				String destinationId = getProperty("dataDestination");			
				// first try and look for the control in the page
				Control destinationControl = page.getControl(destinationId);
				// check we got a control
				if (destinationControl == null) {
					js = "// destination control " + destinationId + " could not be found\n" ;
				} else  {				
					// the value we will get
					String value = null;
					// check the action type
					if ("id".equals(actionType)) {
						value = "_formId";
					} else if ("val".equals(actionType)) {
						// get the control value from the _formValues object which we add in the dynamic section
						value = "_formValues['" + getProperty("dataSource") + "']"; 																		
					} else if ("sub".equals(actionType)) {					
						// get the form submit message
						value = "_formValues['sub']";								
					} else if ("err".equals(actionType)) {					
						// get the form error message
						value = "_formValues['err']";						
					} else if ("res".equals(actionType)) {
						// create the resume url
						value = "'~?a=" + application.getId() + "&v=" + application.getVersion() + "&action=resume&f=' + _formId + '&pwd=' + _formValues['res']";
					} else if ("pdf".equals(actionType)) {
						// create the pdf url
						value = "'~?a=" + application.getId() + "&v=" + application.getVersion() + "&action=pdf&f='+ _formId";
					}						
					// use the set data if we got something
					if (value != null) js = "setData_" + destinationControl.getType() + "(ev, '" + destinationId + "', null, " + destinationControl.getDetails() + ", " + value + ");\n";
				} // destination check										
			} // action type
		} // form adapter type
		// return the js
		return js;
	}
				
}
