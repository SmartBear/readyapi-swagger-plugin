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

package com.smartbear.restplugin

import com.eviware.soapui.SoapUI
import com.eviware.soapui.impl.rest.RestMethod
import com.eviware.soapui.impl.rest.RestRequestInterface
import com.eviware.soapui.impl.rest.RestResource
import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.RestServiceFactory
import com.eviware.soapui.impl.rest.support.RestParameter
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.smartbear.swagger4j.ApiDeclaration
import com.smartbear.swagger4j.Parameter
import com.smartbear.swagger4j.Swagger

/**
 * A simple Swagger importer - now uses swagger4j library
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

        def resourceListing = Swagger.readSwagger( URI.create(url) )
        resourceListing.apiList.each {

            String name = it.path
            if( name.startsWith( "/api-docs"))
            {
                def ix = name.indexOf( "/", 1 )
                if( ix > 0 )
                    name = name.substring( ix )
            }

            Console.println( "Importing API declaration with path $it.path")

            def restService = importApiDeclaration(it.declaration, name)
            ensureEndpoint( restService, url )
            result.add(restService)
        }

		return result.toArray()
	}

    public RestService importApiDeclaration( String url )
    {
        def declaration = Swagger.createReader().readApiDeclaration(URI.create(url))
        def name = declaration.basePath == null ? url : declaration.basePath

        def restService = importApiDeclaration( declaration, name );

        ensureEndpoint(restService, url)

        return restService
    }

    public void ensureEndpoint(RestService restService, String url) {
        if (restService.endpoints.length == 0) {

            def ix = url.indexOf("://")
            if (ix > 0) {
                ix = url.indexOf("/", ix + 3)

                url = ix == -1 ? url : url.substring(0, ix)
                restService.addEndpoint(url)
            }
        }
    }

    /**
	 * Imports all swagger api declarations in the specified JSON document into a RestService
	 * @url the url of the JSON document defining swagger APIs to import
	 * @return the created RestService
	 */

	public RestService importApiDeclaration( ApiDeclaration apiDeclaration, String name )
    {
		// create the RestService
		RestService restService = createRestService( apiDeclaration.basePath, name )

		// loop apis in document
		apiDeclaration.apiList.each {

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

				RestMethod method = resource.addNewMethod( it.nickName )
				method.method = RestRequestInterface.HttpMethod.valueOf( it.method.name().toUpperCase() )
				method.description = it.summary

				// loop parameters and add accordingly
				it.parameterList.each {
					it

					// ignore body parameters
					if( it.paramType != Parameter.ParamType.body ) {

						RestParameter p = method.params.addProperty( it.name )
						def paramType = it.paramType.name()
						if( paramType == "path" )
						   paramType = "template"
                        else if( paramType == "form" )
                            paramType = "query"

                        try
                        {
						    p.style = ParameterStyle.valueOf( paramType.toUpperCase() )
                        }
                        catch( IllegalArgumentException e )
                        {
                            SoapUI.logError( e );
                        }

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
        RestService restService = project.addNewInterface( name, RestServiceFactory.REST_TYPE )

        if( path != null )
        {
            try {
                if( path.startsWith( "/"))
                {
                    restService.basePath = path
                }
                else {
                    URL url = new URL(path)
                    def pathPos = path.length() - url.path.length()

                    restService.basePath = path.substring(pathPos)
                    restService.addEndpoint(path.substring(0, pathPos))
                }
            }
            catch( Exception e )
            {
                SoapUI.logError( e )
            }
        }

        return restService
	}
}
