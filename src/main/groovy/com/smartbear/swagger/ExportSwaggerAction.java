/**
 *  Copyright 2013 SmartBear Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.smartbear.swagger;

import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.rest.RestServiceFactory;
import com.eviware.soapui.impl.settings.XmlBeansSettingsImpl;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormOptionsField;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AField.AFieldType;
import com.eviware.x.form.support.AForm;
import com.smartbear.swagger4j.SwaggerFormat;

/**
 * Shows a simple dialog for specifying the swagger definition and performs the
 * import
 * 
 * @author Ole Lensmar
 */

@ActionConfiguration(actionGroup = "EnabledWsdlProjectActions", afterAction = "AddSwaggerAction")
public class ExportSwaggerAction extends AbstractSoapUIAction<WsdlProject>
{
    private static final String BASE_PATH = Form.class.getName() + Form.BASEPATH;
    private static final String TARGET_PATH = Form.class.getName() + Form.FOLDER;
    private static final String FORMAT = Form.class.getName() + Form.FORMAT;
    private static final String VERSION = Form.class.getName() + Form.VERSION;

    private XFormDialog dialog;

	public ExportSwaggerAction()
	{
		super( "Export Swagger", "Creates a Swagger definition for selected REST APIs" );
	}

	public void perform( WsdlProject project, Object param )
	{
        if( project.getInterfaces(RestServiceFactory.REST_TYPE).isEmpty())
        {
            UISupport.showErrorMessage("Project is missing REST APIs");
            return;
        }

		// initialize form
        XmlBeansSettingsImpl settings = project.getSettings();
        if( dialog == null )
		{
			dialog = ADialogBuilder.buildDialog( Form.class );

            dialog.setValue(Form.FORMAT, settings.getString(FORMAT, "json"));
            dialog.setValue(Form.VERSION, settings.getString(VERSION, "1.0"));
            dialog.setValue(Form.BASEPATH, settings.getString(BASE_PATH, "" ));
            dialog.setValue(Form.FOLDER, settings.getString(TARGET_PATH, "" ));
		}

        XFormOptionsField apis = (XFormOptionsField) dialog.getFormField(Form.APIS);
        apis.setOptions(ModelSupport.getNames(project.getInterfaces(RestServiceFactory.REST_TYPE)));

		while( dialog.show() )
		{
			try
			{
                SwaggerExporter exporter = new SwaggerExporter( project );

                Object[] options = ((XFormOptionsField) dialog.getFormField(Form.APIS)).getSelectedOptions();
                if( options.length == 0 )
                {
                    throw new Exception( "You must select at least one REST API ");
                }

                RestService [] services = new RestService[options.length];
                for( int c = 0; c < options.length; c++ )
                {
                    services[c] = (RestService) project.getInterfaceByName( String.valueOf(options[c]) );
                    if( services[c].getEndpoints().length == 0 )
                    {
                        throw new Exception( "Selected APIs must contain at least one endpoint");
                    }
                }

                // double-check
                if( services.length == 0 )
                {
                    throw new Exception( "You must select at least one REST API to export");
                }

                String version = dialog.getValue(Form.VERSION);
                if( StringUtils.isNullOrEmpty( version ))
                    version = "1.0";

                String path = exporter.exportToFolder( dialog.getValue( Form.FOLDER ), version,
                        SwaggerFormat.valueOf(dialog.getValue( Form.FORMAT ).toLowerCase()), services,
                        dialog.getValue(Form.BASEPATH));

                UISupport.showInfoMessage( "Swagger resource listing has been created at [" + path + "]" );

                settings.setString(BASE_PATH, dialog.getValue(Form.BASEPATH));
                settings.setString(TARGET_PATH, dialog.getValue(Form.FOLDER));
                settings.setString(FORMAT, dialog.getValue(Form.FORMAT));
                settings.setString(VERSION, dialog.getValue(Form.VERSION));

                break;
			}
			catch( Exception ex )
			{
				UISupport.showErrorMessage( ex );
			}
		}
	}

	@AForm( name = "Export Swagger Definition", description = "Creates a Swagger definition for selected REST APIs in this project" )
	public interface Form
	{
        @AField( name = "APIs", description = "Select which REST APIs to include in the Swagger definition", type = AFieldType.MULTILIST )
        public final static String APIS = "APIs";

		@AField( name = "Target Folder", description = "Where to save the Swagger definition", type = AFieldType.FOLDER )
		public final static String FOLDER = "Target Folder";

        @AField( name = "API Version", description = "API Version", type = AFieldType.STRING )
        public final static String VERSION = "API Version";

        @AField( name = "Base Path", description = "Base Path that the Swagger definition will be hosted on", type = AFieldType.STRING )
        public final static String BASEPATH = "Base Path";

        @AField( name = "Format", description = "Select Swagger format", type = AFieldType.RADIOGROUP, values = { "json", "xml"})
        public final static String FORMAT = "Format";
	}

}
