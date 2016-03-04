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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Mobile extends Action {
	
	// private instance variables
	private ArrayList<Action> _successActions, _errorActions, _onlineActions, _childActions;
	
	// properties
	public ArrayList<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(ArrayList<Action> successActions) { _successActions = successActions; }
	
	public ArrayList<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(ArrayList<Action> errorActions) { _errorActions = errorActions; }
	
	public ArrayList<Action> getOnlineActions() { return _onlineActions; }
	public void setOnlineActions(ArrayList<Action> onlineActions) { _onlineActions = onlineActions; }
	
	// constructors
	
	// used by jaxb
	public Mobile() { 
		super(); 
	}
	// used by designer
	public Mobile(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		this();
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for success and error actions
			if (!"successActions".equals(key) && !"errorActions".equals(key) && !"onlineActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		} 
		
		// upload images was modified to have a number of gallery ids (rather than 1) migrate for old versions
		String type = getProperty("actionType");
		// if this is upload images
		if ("uploadImages".equals(type)) {
			// get any single gallery controlId
			String galleryControlId = getProperty("galleryControlId");
			// if not null
			if (galleryControlId != null) {
				// empty the property
				_properties.remove("galleryControlId");
				// move it into the galleryControlIds
				_properties.put("galleryControlIds", "[\"" + galleryControlId + "\"]");
			}			
		}
		
		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some
		if (jsonSuccessActions != null) {
			_successActions = Control.getActions(rapidServlet, jsonSuccessActions);
		}
		
		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some
		if (jsonErrorActions != null) {
			// instantiate our contols collection
			_errorActions = Control.getActions(rapidServlet, jsonErrorActions);
		}
		
		// grab any onlineActions
		JSONArray jsonOnlineActions = jsonAction.optJSONArray("onlineActions");
		// if we had some
		if (jsonOnlineActions != null) {
			// instantiate our contols collection
			_onlineActions = Control.getActions(rapidServlet, jsonOnlineActions);
		}
	}
		
	// overridden methods
	
	@Override
	public List<Action> getChildActions() {			
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<Action>();
			// add child success actions
			if (_successActions != null) {
				for (Action action : _successActions) _childActions.add(action);			
			}
			// add child error actions
			if (_errorActions != null) {
				for (Action action : _errorActions) _childActions.add(action);			
			}
			// add child online actions
			if (_onlineActions != null) {
				for (Action action : _onlineActions) _childActions.add(action);			
			}
		}
		return _childActions;	
	}
	
	@Override
	public String getPageJavaScript(RapidRequest rapidRequest, Application application, Page page, JSONObject jsonDetails) throws Exception {
		// refrence to these success and fail actions are sent as callbacks to the on-mobile device file upload function
		if (_successActions == null && _errorActions == null) {
			return null;
		} else {
			String js  = "";
			// get our id
			String id = getId();
			// get the control (the slow way)
			Control control = page.getActionControl(id);
			// check if we have any success actions
			if (_successActions != null) {
				js += "function " + id + "success(ev) {\n";
				for (Action action : _successActions) {
					js += "  " + action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
				}
				js += "}\n";
			}
			// check if we have any success actions
			if (_errorActions != null) {
				js += "function " + id + "error(ev, server, status, message) {\n";
				for (Action action : _errorActions) {
					js += "  " + action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
				}
				js += "}\n";
			}
			return js;			
		}
		
	}
	
	// a re-usable function to check whether we are on a mobile device - this is used selectively according to the type and whether the alert should appear or we can silently ignore
	private String getMobileCheck(boolean alert) {
		// check that rapidmobile is available
		String js = "if (typeof _rapidmobile == 'undefined') {\n";
		// check we have errorActions
		if (_errorActions == null) {
			if (alert) js += "  alert('This action is only available in Rapid Mobile');\n";
		} else {
			js += "  " + getId() + "error(ev, {}, 1, 'This action is only available in Rapid Mobile');\n";
		}
		js += "} else {\n";
		return js;
	}
	
	// a re-usable function for printing the details of the outputs
	private String getOutputs(RapidHttpServlet rapidServlet, Application application, Page page, String outputsJSON) throws JSONException {
		
		// start the outputs string
		String outputsString = "";
					
		// read into json Array
		JSONArray jsonOutputs = new JSONArray(outputsJSON);
		
		// loop
		for (int i = 0; i < jsonOutputs.length(); i++) {
									
			// get the gps desintation
			JSONObject jsonGpsDestination = jsonOutputs.getJSONObject(i);
			
			// get the itemId
			String itemId = jsonGpsDestination.getString("itemId");
			// split by escaped .
			String idParts[] = itemId.split("\\.");
			// if there is more than 1 part we are dealing with set properties, for now just update the destintation id
			if (idParts.length > 1) itemId = idParts[0];
			
			// get the field
			String field = jsonGpsDestination.optString("field","");
			
			// first try and look for the control in the page
			Control destinationControl = page.getControl(itemId);
			// assume we found it
			boolean pageControl = true;
			// check we got a control
			if (destinationControl == null) {
				// now look for the control in the application
				destinationControl = application.getControl(rapidServlet.getServletContext(), itemId);
				// set page control to false
				pageControl = false;
			} 
			
			// check we got one from either location
			if (destinationControl != null) {				
												
				// get any details we may have
				String details = destinationControl.getDetailsJavaScript(application, page);
					
				// if we have some details
				if (details != null) {
					// if this is a page control
					if (pageControl) {
						// the details will already be in the page so we can use the short form
						details = destinationControl.getId() + "details";
					} 
				}
				
				// if the idParts is greater then 1 this is a set property
				if (idParts.length > 1) {
					
					// get the property from the second id part
					String property = idParts[1];

					// make the getGps call to the bridge
					outputsString += "{f:'setProperty_" + destinationControl.getType() +  "_" + property + "',id:'" + itemId + "',field:'" + field + "',details:'" + details + "'}";
				
				} else {
					
					outputsString += "{f:'setData_" + destinationControl.getType() + "',id:'" + itemId + "',field:'" + field + "',details:'" + details + "'}";
					
				} // copy / set property check
				
				// add a comma if more are to come
				if (i < jsonOutputs.length() - 1) outputsString += ", ";
				
			} // destination control check	
																															
		} // destination loop
		
		// return
		return outputsString;
		
	}
			
	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) {
		// start the js
		String js = "";		
		// get the servlet
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();
		// get the type
		String type = getProperty("actionType");
		// check we got something
		if (type != null) {
			// check the type
			if ("dial".equals(type) || "sms".equals(type)) {
				// get the number control id
				String numberControlId = getProperty("numberControlId");
				// get the control
				Control numberControl = Control.getControl(rapidServlet.getServletContext(), application, page, numberControlId);				
				// check we got one
				if (numberControl == null) {
					js += "// phone number control " + numberControlId + " not found\n";
				} else {
					// get the number field
					String numberField = getProperty("numberField");
					// mobile check with alert
					js += getMobileCheck(true);															
					// get number
					js += "  var number = " + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, numberControlId, numberField) + ";\n";
					// sms has a message too
					if ("sms".equals(type)) {
						// get the message control id
						String messageControlId = getProperty("messageControlId");
						// get the messagecontrol
						Control messageControl = Control.getControl(rapidServlet.getServletContext(), application, page, messageControlId);
						// check we got one
						if (messageControl == null) {
							js += "// message control " + numberControlId + " not found\n";
						} else {
							// get the field
							String messageField = getProperty("messageField");
							// get the message
							js += "  var message = " + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, messageControlId, messageField) + ";\n";
							// send the message
							js += "  _rapidmobile.openSMS(number, message);\n";
						}
					} else {
						// dial number
						js += "  _rapidmobile.openPhone(number);\n";
					}					
					// close mobile check
					js += "}";
				}
								
			} else if ("addImage".equals(type)) {
				
				// get the gallery control Id
				String galleryControlId = getProperty("galleryControlId");
				// get the gallery control
				Control galleryControl = page.getControl(galleryControlId);
				// check if we got one
				if (galleryControl == null) {
					js += "  //gallery control " + galleryControlId + " not found\n";
				} else {
					// mobile check with alert
					js += getMobileCheck(true);
					int maxSize = Integer.parseInt(getProperty("imageMaxSize"));
					int quality = Integer.parseInt(getProperty("imageQuality"));					
					js += "  _rapidmobile.addImage('" + galleryControlId + "'," + maxSize + "," + quality + ");\n";
					// close mobile check
					js += "}\n";
				}
				
			} else if ("uploadImages".equals(type)) {
												
				// make a list of control ids
				List<String> galleryControlIds = new ArrayList<String>(); 
				
				// get the old style gallery id
				String galleryControlIdProperty = getProperty("galleryControlId");
				// if we got one
				if (galleryControlIdProperty != null) {
					//  add to list if it contains something
					if (galleryControlIdProperty.trim().length() > 0) galleryControlIds.add(galleryControlIdProperty);
				}
				
				// get the new style gallery ids
				String galleryControlIdsProperty = getProperty("galleryControlIds");
				// if we got one
				if (galleryControlIdsProperty != null) {
					// clean it up
					galleryControlIdsProperty = galleryControlIdsProperty.replace("\"","").replace("[", "").replace("]", "");
					// if anything is left
					if (galleryControlIdsProperty.length() > 0) {
						// split and loop
						for (String id : galleryControlIdsProperty.split(",")) {
							// add to collection
							galleryControlIds.add(id);
						}
					}
				}
				
				// check if we got one
				if (galleryControlIds.size() == 0) {
					js += "  // no galleryControls specified\n";
				} else {
					// assume no success call back
					String successCallback = "null";
					// update to name of callback if we have any success actions
					if (_successActions != null) successCallback = "'" + getId() + "success'";
					// assume no error call back
					String errorCallback = "null";
					// update to name of callback  if we have any error actions
					if (_errorActions != null) errorCallback = "'" + getId() + "error'";
					// start building the js
					js += "var urls = '';\n";
					// get any urls from the gallery controls
					for (String id : galleryControlIds) {
						js += "$('#" + id + "').find('img').each( function() { urls += $(this).attr('src') + ',' });\n";
					}
					// if we got any urls
					js += "if (urls) { \n";															
					// mobile check with alert
					js += "  " + getMobileCheck(true).replace("\n", "\n  ");
					// upload the images
					js += "  _rapidmobile.uploadImages('" + getId() + "', urls, " + successCallback + ", " + errorCallback + ");\n";
					// close rapid mobile check 
					js += "  }\n";
					// close urls check and proceed straight to success call back if none
					js += "}";
					// if there is a successCallback call it now
					if (!"null".equals(successCallback) && successCallback.length() > 0) js += " else {\n  " + successCallback.replace("'", "") + "(ev);\n}\n";
				}
				
			} else if ("navigate".equals(type)) {
				
				// get the naviagte source control id
				String navigateControlId = getProperty("navigateControlId");
				// get the control
				Control navigateControl = Control.getControl(rapidServlet.getServletContext(), application, page, navigateControlId);				
				// check we got one
				if (navigateControl == null) {
					js += "// navigate to control " + navigateControlId + " not found\n";
				} else {				
					// get the navigate to field
					String navigateField = getProperty("navigateField");
					// get the mode
					String navigateMode = getProperty("navigateMode");
					// enclose if we got one
					if (navigateMode != null) navigateMode = "'" + navigateMode + "'";
					// mobile check 
					js += getMobileCheck(true);
					// get the data
					js += "  var data = " + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, navigateControlId, navigateField) + ";\n";
					// assume no search fields
					String searchFields = getProperty("navigateSearchFields");
					// if we got some
					if (searchFields != null) {
						// if there's something 
						if (searchFields.trim().length() > 0) {
							// build the JavaScript object
							searchFields = "{searchFields:'" + searchFields.replace("'", "\'") + "'}";
						} else {
							// set to null
							searchFields = null;
						}
					}
					// get a position object
					js += "  var pos = getMapPosition(data, 0, null, null, " + searchFields + ");\n";
					// add js, replacing any dodgy inverted commas
					js += "  if (pos && (pos.lat || pos.lng || pos.s)) _rapidmobile.navigateTo(pos.lat, pos.lng, pos.s, " + navigateMode + ");\n";
					// close mobile check
					js += "}\n";
				}
				
			}  else if ("message".equals(type)) {
				
				// retrieve the message
				String message = getProperty("message");
				// update to empty string if null
				if (message == null) message = "";
				// mobile check with silent fail
				js += getMobileCheck(false);
				// add js, replacing any dodgy inverted commas
				js += "  _rapidmobile.showMessage('" + message.replace("'", "\\'") + "');\n";
				// close mobile check
				js += "}\n";
				
			} else if ("disableBackButton".equals(type)) {
				
				// mobile check with silent fail
				js += getMobileCheck(false);
				// add js
				js += "    _rapidmobile.disableBackButton();\n";
				// close mobile check
				js += "  }\n";
				
			} else if ("sendGPS".equals(type)) {
				
				// mobile check with alert
				js += getMobileCheck(true);
				
				// get whether to check if gps is enabled
				boolean checkGPS = Boolean.parseBoolean(getProperty("gpsCheck"));
				// if we had one call it
				if (checkGPS) js += "  _rapidmobile.checkGPS();\n";
				
				// get the gps frequency into an int
				int gpsFrequency = Integer.parseInt(getProperty("gpsFrequency"));
				
				// get the gps destinations
				String gpsDestinationsString = getProperty("gpsDestinations");
				
				// if we had some
				if (gpsDestinationsString != null) {					
															
					try {
						
						// start the getGPS string
						String getGPSjs = "  _rapidmobile.getGPS(" + gpsFrequency + ",\"[";

						// add the gpsDestinationsString
						getGPSjs += getOutputs(rapidServlet, application, page, gpsDestinationsString);

						// close the get gps string
						getGPSjs += "]\");\n";
						
						// add it into the js
						js += getGPSjs;
						
					} catch (JSONException ex) {
						
						// print an error into the js instead
						js += "  // error reading gpsDestinations : " + ex.getMessage();
						
					}
					
				} // gps destinations check			
				
				// close mobile check
				js += "}\n";
				
			} else if ("stopGPS".equals(type)) {
				
				// mobile check with silent fail
				js += getMobileCheck(false);
				// call stop gps
				js += "  _rapidmobile.stopGPS();\n";
				// close mobile check
				js += "}\n";
				
			} else if ("online".equals(type)) {
				
				// check we have online actions
				if (_onlineActions != null) {
					// check size
					if (_onlineActions.size() > 0) {
						
						try {
						
							// ensure we have a details object
							if (jsonDetails == null) jsonDetails = new JSONObject();
					
							// add js online check
							js += "  if (typeof _rapidmobile == 'undefined' ? true : _rapidmobile.isOnline()) {\n";
							
							// get any working / loading page
							String workingPage = getProperty("onlineWorking");
							// if there was one 
							if (workingPage != null) {
								// show working page as a dialogue
								js += "  if (Action_navigate) Action_navigate('~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + workingPage + "&action=dialogue',true,'" + getId() + "');\n";
								// record that we have a working page in the details
								jsonDetails.put("workingPage", getId());
							}
												
							// get the offline dialogue
							String offlinePage = getProperty("onlineFail");
							
							// loop them (this should clean out the working and offline entries in the details)
							for (Action action : _onlineActions) {
								
								// record that we have an offline page
								jsonDetails.put("offlinePage", offlinePage);
								
								js += "  " + action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
																	
							}
							
							// get the working details page (in case none of the actions have used it
							workingPage = jsonDetails.optString("workingPage", null);
																			
							// js online check fail
							js += "} else {\n";
													
							// if we have an offline page one show it
							if (offlinePage != null) js += "  if (Action_navigate) Action_navigate('~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + offlinePage + "&action=dialogue',true,'" + getId() + "');\n";
							
							// close online check
							js += "}\n";
							
							} catch (Exception ex) {
								// print an error instead
								js = "// failed to print action " + getId() + " JavaScript : " + ex.getMessage() + "\n";
							}
							
						} // online actions size check
						
					} // online actions check non-null check
				
			} else if ("addBarcode".equals(type)) {
				
				try {
					
					// mobile check with fail
					String jsBarcode = getMobileCheck(true);
					
					// get the barcodeDestinations
					String barcodeDestinations = getProperty("barcodeDestinations");
					
					// start the check for the addBarcode function
					jsBarcode += "  if (_rapidmobile.addBarcode) {\n";
					
					// start the add barcode call
					jsBarcode += "    _rapidmobile.addBarcode(\"[";
					
					jsBarcode += getOutputs(rapidServlet, application, page, barcodeDestinations);
					
					// call get barcode
					jsBarcode +=  "]\");\n";
					
					// close function check
					jsBarcode += "  } else alert('Barcode reading is not available in this version of Rapid Mobile');\n";
					
					// close mobile check
					jsBarcode += "}\n";
					
					// now safe to add back into main js
					js += jsBarcode;
					
				} catch (JSONException ex) {
					
					// print an error into the js instead
					js += "  // error reading gpsDestinations : " + ex.getMessage();
					
				}
												
			} // mobile action type check
			
		} // mobile action type non-null check

		// return an empty string
		return js;
	}
	
}
