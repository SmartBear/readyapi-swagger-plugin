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

import com.eviware.soapui.impl.rest.RestRepresentation
import com.eviware.soapui.impl.rest.RestRequestInterface
import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.models.Info
import io.swagger.models.Operation
import io.swagger.models.Path
import io.swagger.models.Response
import io.swagger.models.Swagger
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.HeaderParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.swagger.util.Json
import io.swagger.util.Yaml

/**
 * A simple Swagger exporter - now uses swagger4j library
 *
 * @author Ole Lensmar
 */

class Swagger2Exporter implements SwaggerExporter {

    private final WsdlProject project

    public Swagger2Exporter(WsdlProject project) {
        this.project = project
    }

    String exportToFolder(String path, String apiVersion, String format, RestService[] services, String basePath) {

        Swagger swagger = new Swagger();
        swagger.basePath = basePath
        swagger.info = new Info()

        swagger.info.version = apiVersion
        swagger.info.title = services[0].name

        services.each {
            it.allResources.each {
                Path p = new Path()

                it.restMethodList.each {
                    Operation operation = new Operation()
                    operation.operationId = it.resource.name

                    it.representations.each {
                        if (it.type == RestRepresentation.Type.RESPONSE || it.type == RestRepresentation.Type.FAULT)
                            it.status?.each { operation.addResponse(String.valueOf(it), new Response()) }
                        else if (it.type == RestRepresentation.Type.REQUEST && it.mediaType != null)
                            operation.addConsumes(it.mediaType)
                    }

                    if ( !operation.responses || operation.responses.isEmpty()) {
                        operation.addResponse("200", new Response())
                    }

                    p.set(it.method.name().toLowerCase(), operation)

                    addParametersToOperation(it.params, operation)
                    addParametersToOperation(it.overlayParams, operation)

                    if (it.method == RestRequestInterface.HttpMethod.POST || it.method == RestRequestInterface.HttpMethod.PUT) {
                        def param = new BodyParameter()
                        operation.addParameter(param)
                        param.name = "body"
                        param.description = "Request body"
                        param.required = true
                    }
                }

                swagger.path(it.fullPath, p)
            }
        }

        ObjectMapper mapper = format.equals("yaml") ? Yaml.mapper() : Json.mapper();
        mapper.writeValue(new FileWriter(path + File.separatorChar + "api-docs." + format), swagger);

        return path;
    }

    private void addParametersToOperation(RestParamsPropertyHolder params, Operation op) {

        for (name in params.getPropertyNames()) {
            def param = params.getProperty(name)
            if (!operationHasParameter(op, name)) {
                Parameter p = null

                switch (param.style) {
                    case ParameterStyle.HEADER: p = new HeaderParameter(); break;
                    case ParameterStyle.QUERY: p = new QueryParameter(); break;
                    case ParameterStyle.TEMPLATE: p = new PathParameter(); break;
                }

                if (p != null) {
                    op.addParameter(p)
                    p.name = param.name
                    p.required = p instanceof PathParameter || param.getRequired()
                    p.description = param.description

                    // needs to be extended to support all schema types
                    switch (param.type.localPart) {
                        case "byte": p.type = "byte"; break
                        case "dateTime": p.type = "Date"; break
                        case "float": p.type = "float"; break
                        case "double": p.type = "double"; break
                        case "long": p.type = "long"; break
                        case "short":
                        case "int":
                        case "integer": p.type = "int"; break
                        case "boolean": p.type = "boolean"; break
                        default: p.type = "string"
                    }
                }
            }
        }
    }

    boolean operationHasParameter(Operation operation, String name) {
        operation?.parameters.each { if (it.name == name) return true }

        return false
    }
}
