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

import com.eviware.soapui.impl.rest.RestRepresentation
import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.eviware.soapui.support.StringUtils
import com.smartbear.swagger4j.*
import com.smartbear.swagger4j.impl.Utils

/**
 * A simple Swagger exporter - now uses swagger4j library
 * 
 * @author Ole Lensmar
 */

class SwaggerExporter {

	private final WsdlProject project

	public SwaggerExporter( WsdlProject project ) {
		this.project = project
	}

    String exportToFolder(String path, String apiVersion, Constants.Format format, RestService [] services, String basePath ) {

        ResourceListing rl = generateResourceListing(services, apiVersion, format, basePath )
        return exportResourceListing(format, rl, path)
    }

    public String exportResourceListing(Constants.Format format, ResourceListing rl, String path) {
        def store = new Utils.MapSwaggerStore()
        Swagger.createWriter(format).writeSwagger(store, rl)

        store.fileMap.each { k, v -> Console.println("$k : $v") }


        return FileSwaggerStore.writeSwagger(path, rl, format)
    }

    public ResourceListing generateResourceListing(RestService[] services, String apiVersion, Constants.Format format, String basePath) {
        if( StringUtils.isNullOrEmpty( basePath ))
            basePath = services[0].endpoints[0] + services[0].basePath

        ResourceListing rl = Swagger.createResourceListing(basePath)
        rl.apiVersion = apiVersion;

        services.each {

            def serviceBasePath = it.basePath
            basePath = it.endpoints[0] + serviceBasePath
            ApiDeclaration apiDeclaration = Swagger.createApiDeclaration(basePath, "")
            apiDeclaration.apiVersion = apiVersion

            it.getAllResources().each {

                if (it.getRestMethodCount() > 0) {
                    def fullPath = it.fullPath
                    if (fullPath.startsWith(serviceBasePath))
                        fullPath = fullPath.substring(serviceBasePath.length())

                    Console.println("Adding API for resource at $fullPath")
                    def api = apiDeclaration.addApi(fullPath)

                    it.restMethodList.each {
                        Console.println("Adding Operation for method $it.name")
                        def op = api.addOperation(it.name, Operation.Method.valueOf(it.method.name()))

                        it.responseMediaTypes.each {
                            op.addProduces(it)
                        }

                        it.representations.each {
                            if (it.type == RestRepresentation.Type.FAULT) {
                                it.status.each {
                                    op.addErrorResponse(Integer.valueOf(it), "")
                                }
                            }
                        }

                        addParametersToOperation(it.params, op)
                        addParametersToOperation(it.overlayParams, op)
                    }
                }
            }


            def filename = StringUtils.createFileName(it.name) + "-api-docs." + format.extension
            rl.addApi(apiDeclaration, "/" + filename.toLowerCase())

            Console.println("Added api $apiDeclaration.resourcePath in file $filename")
        }
        return rl
    }

    private void addParametersToOperation(RestParamsPropertyHolder params, Operation op) {
        for (name in params.getPropertyNames()) {
            def param = params.getProperty(name)
            if( op.getParameter( name ) == null )
            {
                def style = null

                switch (param.style) {
                    case ParameterStyle.HEADER: style = Parameter.ParamType.header; break;
                    case ParameterStyle.QUERY: style = Parameter.ParamType.query; break;
                    case ParameterStyle.TEMPLATE: style = Parameter.ParamType.path; break;
                }

                if (style != null) {
                    def p = op.addParameter(param.name, style)
                    p.required = style == Parameter.ParamType.path || param.getRequired()
                    p.description = param.description

                    // needs to be extended to support all schema types
                    switch( param.type.localPart )
                    {
                        case "byte"     : p.dataType = "byte"; break
                        case "dateTime" : p.dataType = "Date"; break
                        case "float"    : p.dataType = "float"; break
                        case "double"   : p.dataType = "double"; break
                        case "long"     : p.dataType = "long"; break
                        case "short"    :
                        case "int"      :
                        case "integer"  : p.dataType = "int"; break
                        case "boolean"  : p.dataType = "boolean"; break
                        default         : p.dataType = "string"
                    }
                }
            }
        }
    }
}
