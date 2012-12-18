package com.smartbear.restplugin;

import java.io.File;

import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AField.AFieldType;
import com.eviware.x.form.support.AForm;

/**
 * Shows a simple dialog for specifying the swagger definition and performs the
 * import
 * 
 * @author Ole Lensmar
 */

public class AddSwaggerAction extends AbstractSoapUIAction<WsdlProject>
{
	private XFormDialog dialog;

	public AddSwaggerAction()
	{
		super( "Add Swagger", "Imports a swagger definition" );
	}

	public void perform( WsdlProject project, Object param )
	{
		// initialize form
		if( dialog == null )
		{
			dialog = ADialogBuilder.buildDialog( Form.class );
		}
		else
		{
			dialog.setValue( Form.SWAGGERURL, "" );
		}

		while( dialog.show() )
		{
			try
			{
				// get the specified URL
				String url = dialog.getValue( Form.SWAGGERURL ).trim();
				if( StringUtils.hasContent( url ) )
				{
					// expand any property-expansions
					String expUrl = PathUtils.expandPath( url, project );

					// if this is a file - convert it to a file URL
					if( new File( expUrl ).exists() )
						expUrl = new File( expUrl ).toURI().toURL().toString();

					// create the importer and import!
					SwaggerImporter importer = new SwaggerImporter( project );
					RestService[] services = importer.importSwagger( expUrl );

					// select the first imported REST Service (since a swagger definition can 
					// define multiple APIs
					if( services != null && services.length > 0 )
						UISupport.select( services[0] );

					break;
				}
			}
			catch( Exception ex )
			{
				UISupport.showErrorMessage( ex );
			}
		}
	}

	@AForm( name = "Add Swagger Definition", description = "Creates a REST Service from the specified Swagger definition" )
	public interface Form
	{
		@AField( description = "Location or URL of swagger definition", type = AFieldType.FILE )
		public final static String SWAGGERURL = "Swagger Definition";
	}

}
