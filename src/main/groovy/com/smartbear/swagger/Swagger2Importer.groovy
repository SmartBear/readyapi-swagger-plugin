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
import com.wordnik.swagger.models.Info
import com.wordnik.swagger.models.Operation
import com.wordnik.swagger.models.Path
import io.swagger.parser.SwaggerParser

/**
 * A simple Swagger 2.0 importer - now uses swagger-core library
 *
 * Improvements that need to be made:
 * - better error handling
 * - support for reading JSON Models and types
 *
 * @author Ole Lensmar
 */

class Swagger2Importer implements SwaggerImporter {

    private final WsdlProject project

    public Swagger2Importer(WsdlProject project) {
        this.project = project
    }

    public RestService[] importSwagger(String url) {

        def result = []

        def swagger = new SwaggerParser().read(url)
        RestService restService = createRestService(swagger.basePath, swagger.info)
        swagger.paths.each {
            importPath(restService, it.key, it.value)
        }

        result.add(restService)
        ensureEndpoint(restService, url)

        return result.toArray()
    }

    @Override
    RestService importApiDeclaration(String expUrl) {
        return importSwagger(expUrl)
    }

    void ensureEndpoint(RestService restService, String url) {
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

    RestResource importPath(RestService restService, String path, Path resource) {
        RestResource res = restService.addNewResource(path, path)

        if (resource.get != null)
            addOperation(res, resource.get, RestRequestInterface.HttpMethod.GET)

        if (resource.post != null)
            addOperation(res, resource.post, RestRequestInterface.HttpMethod.POST)

        if (resource.put != null)
            addOperation(res, resource.put, RestRequestInterface.HttpMethod.PUT)

        if (resource.delete != null)
            addOperation(res, resource.delete, RestRequestInterface.HttpMethod.DELETE)

        if (resource.patch != null)
            addOperation(res, resource.patch, RestRequestInterface.HttpMethod.PATCH)

        if (resource.options != null)
            addOperation(res, resource.options, RestRequestInterface.HttpMethod.OPTIONS)

        return res;
    }

    RestMethod addOperation(RestResource resource, Operation operation, RestRequestInterface.HttpMethod httpMethod) {
        RestMethod method = resource.addNewMethod(operation.operationId)
        method.method = httpMethod
        method.description = operation.summary

        // loop parameters and add accordingly
        operation.parameters.each {

            // ignore body parameters
            if (it.in != "body") {

                RestParameter p = method.params.addProperty(it.name)
                def paramType = it.in
                if (paramType == "path")
                    paramType = "template"
                else if (paramType == "formData")
                    paramType = "query"

                try {
                    p.style = ParameterStyle.valueOf(paramType.toUpperCase())
                }
                catch (IllegalArgumentException e) {
                    SoapUI.logError(e);
                }

                p.required = it.required
            }
        }

        operation.responses?.each {
            def response = it

            if (operation.produces?.empty) {
                def representation = method.addNewRepresentation(RestRepresentation.Type.RESPONSE)

                representation.status = response.key == "default" ? [] : [response.key]

                // just take the first example
                if (!response.value.examples?.isEmpty()) {
                    representation.mediaType = response.value.examples.iterator().next()
                    representation.sampleContent = response.value.examples[representation.mediaType]
                }
            } else {
                operation.produces?.each {
                    def representation = method.addNewRepresentation(RestRepresentation.Type.RESPONSE)
                    representation.mediaType = it

                    representation.status = response.key == "default" ? [] : [response.key]
                    response.value.examples?.each {

                        if (it.key == representation.mediaType) {
                            representation.sampleContent = it.value
                            representation.mediaType = it.key
                        }
                    }
                }
            }
        }

        if (method.getRepresentations(RestRepresentation.Type.RESPONSE, null)?.length == 0) {
            operation.produces?.each {
                method.addNewRepresentation(RestRepresentation.Type.RESPONSE).mediaType = it
            }
        }

        operation.consumes?.each {
            method.addNewRepresentation(RestRepresentation.Type.REQUEST).mediaType = it
        }

        // add a default request for the generated method
        method.addNewRequest("Request 1")

        return method
    }

    private RestService createRestService(String path, Info info) {
        String name = info?.title
        if (name == null)
            name = path

        RestService restService = project.addNewInterface(name, RestServiceFactory.REST_TYPE)
        restService.description = info.description

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
