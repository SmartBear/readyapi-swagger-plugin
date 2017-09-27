/**
 *  Copyright 2013-2017 SmartBear Software, Inc.
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
import com.eviware.soapui.support.StringUtils
import com.eviware.soapui.support.xml.XmlUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import io.swagger.inflector.examples.ExampleBuilder
import io.swagger.inflector.examples.XmlExampleSerializer
import io.swagger.inflector.examples.models.Example
import io.swagger.inflector.processors.JsonNodeExampleSerializer
import io.swagger.util.Json
import io.swagger.util.Yaml
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import v2.io.swagger.models.Operation
import v2.io.swagger.models.Path
import v2.io.swagger.models.RefModel
import v2.io.swagger.models.Response
import v2.io.swagger.models.Swagger
import v2.io.swagger.models.parameters.BodyParameter
import v2.io.swagger.models.properties.ObjectProperty
import v2.io.swagger.parser.SwaggerParser
import v2.io.swagger.models.properties.Property;
import v2.io.swagger.models.Model;

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

    static ObjectMapper yamlMapper
    static ObjectMapper jsonMapper
    private final WsdlProject project
    private final String defaultMediaType;
    private static Logger logger = LoggerFactory.getLogger(Swagger2Importer)
    private Swagger swagger
    private boolean generateTestCase

    static {
        yamlMapper = Yaml.mapper()
        jsonMapper = Json.mapper()
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(new JsonNodeExampleSerializer());

        yamlMapper.registerModule(simpleModule);
        jsonMapper.registerModule(simpleModule);
    }

    public Swagger2Importer(String defaultMediaType) {
        this(null, defaultMediaType)
    }

    public Swagger2Importer(WsdlProject project, String defaultMediaType) {
        this(project, defaultMediaType, false)
    }

    public Swagger2Importer(WsdlProject project, String defaultMediaType, boolean generateTestCase) {
        this.project = project
        this.defaultMediaType = defaultMediaType
        this.generateTestCase = generateTestCase
    }

    public Swagger2Importer(WsdlProject project) {
        this(project, "application/json")
    }

    public RestService[] importSwagger(String url) {

        def result = []
        Map<String, Object> context = [swaggerUrl: url] as Map

        if (url.startsWith("file:"))
            url = new File(new URL(url).toURI()).absolutePath

        logger.info("Importing swagger [$url]")

        swagger = new SwaggerParser().read(url)
        RestService restService = createRestService(swagger, url)
        swagger.paths.each {
            importPath(restService, it.key, it.value, context)
        }

        result.add(restService)
        ensureEndpoint(restService, url)

        return result.toArray()
    }

    void ensureEndpoint(RestService restService, String url) {
        if (restService != null && restService.endpoints.length == 0) {

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

    RestResource importPath(RestService restService, String path, Path resource, Map<String, Object> context) {
        if (restService == null) {
            return null
        }
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

        if (project != null && generateTestCase) {
            new Swagger2TestCaseGenerator().generateTestCases(project, res, resource, context);
        }
        return res;
    }

    RestMethod addOperation(RestResource resource, Operation operation, RestRequestInterface.HttpMethod httpMethod) {

        def opName = operation.operationId

        if (StringUtils.isNullOrEmpty(opName)) {
            opName = httpMethod.toString()
        }

        RestMethod method = resource.addNewMethod(opName)
        method.method = httpMethod
        method.description = (operation.description ?: "").concat(System.getProperty("line.separator")).concat(operation.summary ?: "")

        // add a default request for the generated method

        // loop parameters and add accordingly
        operation.parameters.each {

            def paramName = it.name == null ? it.ref : it.name
            if (StringUtils.isNullOrEmpty(paramName)) {
                logger.warn("Can not import property without opName or ref [" + it.toString() + "]")
            }
            // ignore body parameters
            else if (it.in != "body") {
                def paramType = it.in == null ? "query" : it.in
                if (paramType == "path")
                    paramType = "template"
                else if (paramType == "formData")
                    paramType = "query"

                // path parameters are added at resource level
                RestParameter p = paramType == "template" ? method.resource.params.addProperty(paramName)
                        : method.params.addProperty(paramName)

                try {
                    p.style = ParameterStyle.valueOf(paramType.toUpperCase())
                }
                catch (IllegalArgumentException e) {
                    SoapUI.logError(e);
                }

                p.description = it.description
                p.required = it.required

                if (it.defaultValue != null) {
                    p.defaultValue = it.defaultValue
                }
            } else {

                BodyParameter bodyParam = it

                def consumes = operation.consumes
                if (consumes == null || consumes.empty) {
                    consumes = swagger.consumes
                }

                consumes.each {
                    def representation = method.addNewRepresentation(RestRepresentation.Type.REQUEST)
                    representation.mediaType = it

                    def request = method.addNewRequest("Request " + (method.requestList.size() + 1))
                    request.mediaType = it

                    if (bodyParam.schema != null) {
                        def op = new ObjectProperty(bodyParam.schema.properties)

                        if (bodyParam.schema instanceof RefModel) {
                            RefModel refModel = bodyParam.schema
                            def modelDefinition = swagger.definitions.get(refModel.simpleRef)
                            if (modelDefinition != null) {
                                op = new ObjectProperty(modelDefinition.properties)
                                op.name(refModel.simpleRef)
                            }
                        }

                        /*  Object output = ExampleBuilder.fromProperty(op, swagger.definitions);
                          if (output instanceof Example) {
                              request.requestContent = serializeExample(it, output)
                              representation.sampleContent = request.requestContent
                          }*/
                    }
                }
            }
        }

        if (method.requestList.isEmpty()) {
            method.addNewRequest("Request 1")
        }

        operation.responses?.each {
            def response = it
            Response responseElement = it.value;

            if (operation.produces == null || operation.produces.empty) {
                operation.produces = swagger.produces
            }

            if (operation.produces == null || operation.produces.empty) {
                def representation = method.addNewRepresentation(RestRepresentation.Type.RESPONSE)

                representation.status = response.key == "default" ? [] : [response.key]
                representation.mediaType = defaultMediaType

                // just take the first example
                if (response.value.examples != null && !response.value.examples.isEmpty()) {
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

                    if (representation.sampleContent == null) {
                        //TODO Waiting for inflector update
                       /* Object output = ExampleBuilder.fromProperty(responseElement.getSchema(), swagger.definitions);
                        if (output instanceof Example) {
                            representation.sampleContent = serializeExample(representation.mediaType, output)
                        }*/
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

        return method
    }

    public String serializeExample(String mediaType, Example output) {
        def sampleValue = null
        def mapper = null

        switch (mediaType) {
            case "application/xml": sampleValue = XmlUtils.prettyPrintXml(new XmlExampleSerializer().serialize(output)); break;
            case "application/yaml": mapper = yamlMapper; break;
            case "application/json": mapper = jsonMapper; break;
        }

        if (mapper != null) {
            sampleValue = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output)
        }
        return sampleValue
    }

    private RestService createRestService(Swagger swagger, String url) {
        String name = swagger.info && swagger.info.title ? swagger.info.title : null
        if (name == null) {
            if (url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")) {
                name = new URL(url).host
            } else {
                def ix = url.lastIndexOf('/')
                name = ix == -1 || ix == url.length() - 1 ? url : url.substring(ix + 1)
            }
        }

        def restService = project.addNewInterface(name, RestServiceFactory.REST_TYPE)
        restService.description = swagger.info?.description

        if (!StringUtils.isNullOrEmpty(swagger.host)) {
            if (swagger.schemes != null) {
                swagger.schemes.each { it ->
                    def scheme = it.toValue().toLowerCase()
                    if (scheme.startsWith("http")) {
                        restService.addEndpoint(scheme + "://" + swagger.host)
                    }
                }
            }

            if (restService.endpoints.length == 0) {
                if (url.toLowerCase().startsWith("http") && url.indexOf(':') > 0) {
                    restService.addEndpoint(url.substring(0, url.indexOf(':')).toLowerCase() + "://" + swagger.host)
                } else {
                    restService.addEndpoint("http://" + swagger.host)
                }
            }
        }

        if (swagger.basePath != null) {
            restService.basePath = swagger.basePath
            if (restService.basePath.endsWith('/')) {
                restService.basePath = restService.basePath.substring(0, restService.basePath.length() - 1)
            }
        }

        return restService
    }

    WsdlProject getProject() {
        return project
    }

    Swagger getSwagger() {
        return swagger
    }
}
