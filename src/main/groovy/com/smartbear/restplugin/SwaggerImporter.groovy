package com.smartbear.restplugin

import groovy.json.*

import com.eviware.soapui.impl.rest.RestMethod
import com.eviware.soapui.impl.rest.RestResource
import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.RestServiceFactory
import com.eviware.soapui.impl.rest.RestRequestInterface.RequestMethod
import com.eviware.soapui.impl.rest.support.RestParameter
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle
import com.eviware.soapui.impl.wsdl.WsdlProject

/**
 * A simple Swagger importer - written in groovy so we can use JsonSlurper
 * 
 * Improvements that need to be made:
 * - better error handling
 * - support for reading JSON Models and types
 * 
 * @author Ole Lensmar
 */

class SwaggerImporter {

	private final WsdlProject project

	public SwaggerImporter( WsdlProject project ) {
		this.project = project
	}

	public RestService [] importSwagger( String url ) {

		def result = []

		// resource listing?
		if( url.endsWith( "/api-docs.json") )
		{
			// load swagger document
			def doc = loadJsonDoc( url )
			def basePath = doc.basePath

			// loop apis and import each separately
			doc.apis.each{
				it

				// replace format template in path
				String realPath = it.path.replaceAll "\\{format\\}", "json"
				url = basePath + realPath
				
				result.add( importApiDeclarations( url ))
			}
			
		}
		// expect an API declaration
		else
		{
			result.add( importApiDeclarations( url ))
		}
		
		return result.toArray()
	}

	/**
	 * Imports all swagger api declarations in the specified JSON document into a RestService
	 * @url the url of the JSON document defining swagger APIs to import
	 * @return the created RestService 
	 */
	
	public RestService importApiDeclarations( def url ) {

		// load the specified document
		def doc = loadJsonDoc( url )
				
		// create the RestService
		RestService restService = createRestService( doc.basePath, url )
		
		// loop apis in document
		doc.apis.each {
			it

			// add a resource for this api
			RestResource resource = restService.addNewResource( it.path, it.path )
			resource.description = it.description

			// check for format template parameter - add at resource level so all methods will inherit
			if( it.path.contains( "{format}" )) {
				RestParameter p = resource.params.addProperty( "format" )
				p.setStyle( ParameterStyle.TEMPLATE )
				p.required = true
				p.options = {"json"}
				p.defaultValue = "json"
			}

			// loop all operations - import as methods
			it.operations.each {
				it

				RestMethod method = resource.addNewMethod( it.nickname )
				method.method = RequestMethod.valueOf( it.httpMethod )
				method.description = it.description

				// loop parameters and add accordingly
				it.parameters.each {
					it

					// ignore body parameters
					if( it.paramType != "body" ) {

						RestParameter p = method.params.addProperty( it.name )
						def paramType = it.paramType.toUpperCase()
						if( paramType == "PATH" )
						   paramType = "TEMPLATE"

						p.style = ParameterStyle.valueOf( paramType )
						p.required = it.required
					}
				}

				// add a default request for the generated method
				method.addNewRequest( "Request 1" )
			}
		}

		return restService
	}

	private RestService createRestService( String path, String name )
	{
		URL url = new URL( path )
		def pathPos = path.length()-url.path.length()

		RestService restService = project.addNewInterface( name, RestServiceFactory.REST_TYPE )
		restService.basePath = path.substring( pathPos )
		restService.addEndpoint( path.substring( 0, pathPos ))

		return restService
	}
	
	private loadJsonDoc( String url )
	{
		def payload = new URL(url).text
		def slurper = new JsonSlurper()
		return slurper.parseText(payload)
	}
}
