/**
 *  Copyright 2013-2016 SmartBear Software, Inc.
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

package com.smartbear.swagger

import com.eviware.soapui.SoapUI
import com.eviware.soapui.impl.rest.RestMethod
import com.eviware.soapui.impl.rest.RestRepresentation
import com.eviware.soapui.impl.rest.RestRequestInterface
import com.eviware.soapui.impl.rest.RestResource
import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.RestServiceFactory
import com.eviware.soapui.impl.rest.support.RestParameter
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.smartbear.swagger4j.ApiDeclaration
import com.smartbear.swagger4j.Parameter

abstract class AbstractSwagger1XImporter implements SwaggerImporter {

    private final WsdlProject project
    private final String defaultMediaType;

    public AbstractSwagger1XImporter(WsdlProject project, String defaultMediaType) {
        this.project = project
        this.defaultMediaType = defaultMediaType
    }

    public AbstractSwagger1XImporter(WsdlProject project) {
        this(project, "application/json")
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

    public RestService importApiDeclaration(ApiDeclaration apiDeclaration, String name) {
        // create the RestService
        RestService restService = createRestService(apiDeclaration.basePath, name)

        // loop apis in document
        apiDeclaration.apiList.each {

            // add a resource for this api
            RestResource resource = restService.addNewResource(it.path, it.path)
            resource.description = it.description

            // check for format template parameter - add at resource level so all methods will inherit
            if (it.path.contains("{format}")) {
                RestParameter p = resource.params.addProperty("format")
                p.setStyle(ParameterStyle.TEMPLATE)
                p.required = true
                p.options = { "json" }
                p.defaultValue = "json"
            }

            // loop all operations - import as methods
            it.operations.each {
                it
                def operation = it

                def methodName = it.nickName
                def cnt = 0
                while (resource.getRestMethodByName(methodName) != null) {
                    methodName = it.nickName + " " + (++cnt)
                }

                RestMethod method = resource.addNewMethod(methodName)
                method.method = RestRequestInterface.HttpMethod.valueOf(it.method.name().toUpperCase())
                method.description = it.summary

                // loop parameters and add accordingly
                it.parameterList.each {
                    it

                    // ignore body parameters
                    if (it.paramType != Parameter.ParamType.body) {

                        def paramType = it.paramType.name()
                        if (paramType == "path")
                            paramType = "template"
                        else if (paramType == "form")
                            paramType = "query"

                        // path parameters are added at resource level
                        RestParameter p = paramType == "template" ? method.resource.params.addProperty(it.name)
                                : method.params.addProperty(it.name)

                        try {
                            p.style = ParameterStyle.valueOf(paramType.toUpperCase())
                        }
                        catch (IllegalArgumentException e) {
                            SoapUI.logError(e);
                        }

                        p.description = it.description
                        p.required = it.required
                    }
                }

                it.responseMessages?.each {
                    def response = it

                    if (operation.produces == null || operation.produces.empty) {
                        def representation = method.addNewRepresentation(RestRepresentation.Type.RESPONSE)

                        representation.status = [response.code]
                        representation.mediaType = defaultMediaType
                        representation.sampleContent = response.message
                    } else {
                        operation.produces?.each {
                            def representation = method.addNewRepresentation(RestRepresentation.Type.RESPONSE)
                            representation.mediaType = it
                            representation.status = [response.code]
                            representation.sampleContent = response.message
                        }
                    }
                }

                // add a default request for the generated method
                method.addNewRequest("Request 1")
            }
        }

        return restService
    }

    private RestService createRestService(String path, String name) {
        RestService restService = project.addNewInterface(name, RestServiceFactory.REST_TYPE)

        if (path != null) {
            try {
                if (path.startsWith("/")) {
                    restService.basePath = path
                } else {
                    URL url = new URL(path)
                    def pathPos = path.length() - url.path.length()

                    restService.basePath = path.substring(pathPos)
                    restService.addEndpoint(path.substring(0, pathPos))
                }
            }
            catch (Exception e) {
                SoapUI.logError(e)
            }
        }

        return restService
    }
}
