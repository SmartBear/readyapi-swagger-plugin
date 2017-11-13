/**
 * Copyright 2013-2017 SmartBear Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartbear.swagger;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.submit.HttpMessageExchange;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestInterface;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.ResponseAssertion;
import com.eviware.soapui.plugins.auto.PluginTestAssertion;
import com.eviware.soapui.plugins.recipe.PluginProvidedAssertion;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormDialogBuilder;
import com.eviware.x.form.XFormFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.smartbear.swagger.utils.OpenAPIUtils;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.oas.models.servers.Server;
import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.util.ClasspathHelper;
import io.swagger.util.Json;
import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.XmlObject;
import v2.io.swagger.models.Model;
import v2.io.swagger.models.Operation;
import v2.io.swagger.models.Response;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.models.properties.Property;
import v2.io.swagger.models.properties.RefProperty;
import v2.io.swagger.parser.SwaggerParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginTestAssertion(id = "SwaggerComplianceAssertion", label = "Swagger Compliance Assertion",
        category = AssertionCategoryMapping.STATUS_CATEGORY,
        description = "Asserts that the request and response messages are compliant with a Swagger definition")
public class SwaggerComplianceAssertion extends WsdlMessageAssertion implements ResponseAssertion, PluginProvidedAssertion {
    private static final String SWAGGER_URL = "swaggerUrl";
    private static final String STRICT_MODE = "strictMode";
    private static final String SWAGGER_URL_FIELD = "Swagger URL";
    private static final String STRICT_MODE_FIELD = "Strict Mode";
    private boolean strictMode;
    private String swaggerUrl;
    private Swagger swagger;
    private JsonSchema swaggerSchema;
    private XFormDialog dialog;
    private OpenAPI openAPI;
    private Boolean isOpenAPI;

    /**
     * Assertions need to have a constructor that takes a TestAssertionConfig and the ModelItem to be asserted
     */

    public SwaggerComplianceAssertion(TestAssertionConfig assertionConfig, Assertable modelItem) {
        super(assertionConfig, modelItem, true, false, false, true);
        readValuesFromConfig();
    }

    @Override
    public void setConfiguration(XmlObject configuration) {
        super.setConfiguration(configuration);
        readValuesFromConfig();
    }

    private void readValuesFromConfig() {
        XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(getConfiguration());
        swaggerUrl = reader.readString(SWAGGER_URL, null);
        strictMode = reader.readBoolean(STRICT_MODE, true);
    }

    public String getSwaggerUrl() {
        return swaggerUrl;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    public boolean configure() {
        if (dialog == null) {
            buildDialog();
        }

        StringToStringMap values = new StringToStringMap();
        values.put(SWAGGER_URL_FIELD, swaggerUrl);
        values.put(STRICT_MODE_FIELD, strictMode);

        values = dialog.show(values);
        if (dialog.getReturnValue() == XFormDialog.OK_OPTION) {
            setSwaggerUrl(values.get(SWAGGER_URL_FIELD));
            strictMode = values.getBoolean(STRICT_MODE_FIELD);
        }

        setConfiguration(createConfiguration());
        return true;
    }

    private void buildDialog() {
        XFormDialogBuilder builder = XFormFactory.createDialogBuilder("Swagger Compliance Assertion");
        XForm mainForm = builder.createForm("Basic");

        mainForm.addTextField(SWAGGER_URL_FIELD, "Swagger Definition URL", XForm.FieldType.URL).setWidth(40);
        mainForm.addCheckBox(STRICT_MODE_FIELD, "Enables strict validation (fails for undefined responses)");

        dialog = builder.buildDialog(builder.buildOkCancelActions(),
                "Specify Swagger URL and validation mode below", UISupport.OPTIONS_ICON);
    }

    public void setSwaggerUrl(String endpoint) {
        swaggerUrl = endpoint;
        swagger = null;
        openAPI = null;
        swaggerSchema = null;
        isOpenAPI = null;
    }

    protected XmlObject createConfiguration() {
        XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
        return builder.add(SWAGGER_URL, swaggerUrl).add(STRICT_MODE, strictMode).finish();
    }

    @Override
    protected String internalAssertResponse(MessageExchange messageExchange, SubmitContext submitContext) throws AssertionException {

        try {
            if (swaggerUrl != null && messageExchange instanceof HttpMessageExchange) {
                if (!messageExchange.hasResponse() || ((HttpMessageExchange) messageExchange).getResponseStatusCode() == 0) {
                    throw new AssertionException(new AssertionError("Missing response to validate"));
                }

                if (validateMessage((HttpMessageExchange) messageExchange, submitContext)) {
                    return "Response is compliant with Swagger Definition";
                }
            }
        } catch (AssertionException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionException(new AssertionError("Swagger Compliance Check failed; [" + e.toString() + "]"));
        }

        return "Response is compliant with Swagger definition";
    }

    private boolean validateMessage(HttpMessageExchange messageExchange, SubmitContext submitContext) throws MalformedURLException, AssertionException {
        if (isOpenAPI == null) {
            isOpenAPI = SwaggerUtils.isOpenApi(swaggerUrl);
        }
        if (isOpenAPI) {
            return validateOpenAPIOperation(messageExchange, submitContext);
        } else {
            return validateSwaggerResponse(messageExchange, submitContext);
        }
    }

    private boolean validateSwaggerResponse(HttpMessageExchange messageExchange, SubmitContext submitContext) throws AssertionException, MalformedURLException {
        Swagger swagger = getSwagger(submitContext);

        HttpTestRequestInterface<?> testRequest = ((HttpTestRequestInterface) messageExchange.getModelItem());
        RestRequestInterface.HttpMethod method = testRequest.getMethod();

        URL endpoint = new URL(messageExchange.getEndpoint());
        String path = endpoint.getPath();
        if (path != null) {
            String basePath = swagger.getBasePath();
            if (basePath != null && !basePath.equals("/") && path.startsWith(basePath)) {
                path = path.substring(basePath.length());
            }

            for (String swaggerPath : swagger.getPaths().keySet()) {

                if (matchesPath(path, swaggerPath)) {

                    Operation operation = findOperation(swagger.getPath(swaggerPath), method);
                    if (operation != null) {
                        validateOperation(swagger, operation, String.valueOf(messageExchange.getResponseStatusCode()),
                                messageExchange.getResponseContent(), messageExchange.getResponseContentType()
                        );

                        return true;
                    } else {
                        throw new AssertionException(new AssertionError(
                                "No resource matching [" + method + " " + path + "] in Swagger definition"));
                    }
                }
            }

            throw new AssertionException(new AssertionError("Failed to find resource for [" + path + "] in Swagger definition"));
        }

        return false;
    }

    private boolean validateOpenAPIOperation(HttpMessageExchange messageExchange, SubmitContext submitContext) throws AssertionException, MalformedURLException {
        getOpenAPI(submitContext);

        HttpTestRequestInterface<?> testRequest = ((HttpTestRequestInterface) messageExchange.getModelItem());
        RestRequestInterface.HttpMethod method = testRequest.getMethod();

        String endpoint = messageExchange.getEndpoint();

        if (endpoint != null) {
            PathItem path = findPathItem(endpoint);

            if (path == null) {
                throw new AssertionException(new AssertionError("No resource matching in OpenAPI definition"));
            }
            io.swagger.oas.models.Operation operation = OpenAPIUtils.extractOperation(path, method);

            if (operation == null) {
                throw new AssertionException(new AssertionError("No method matching [" + method + " " + path + "] in OpenAPI definition"));
            }

            validateOpenAPIOperation(operation, String.valueOf(messageExchange.getResponseStatusCode()), messageExchange.getResponseContent(), messageExchange.getResponseContentType());
            return true;
        }

        return false;
    }

    private Operation findOperation(v2.io.swagger.models.Path path, RestRequestInterface.HttpMethod method) {
        switch (method) {
            case GET:
                return path.getGet();
            case POST:
                return path.getPost();
            case DELETE:
                return path.getDelete();
            case PUT:
                return path.getPut();
            case PATCH:
                return path.getPatch();
            case OPTIONS:
                return path.getOptions();
        }

        return null;
    }

    void validateOperation(Swagger swagger, Operation operation, String responseCode, String contentAsString, String contentType) throws AssertionException {

        Response responseSchema = operation.getResponses().get(responseCode);
        if (responseSchema == null) {
            responseSchema = operation.getResponses().get("default");
        }

        if (responseSchema != null) {
            validateResponse(contentAsString, contentType, swagger, responseSchema);
        } else if (strictMode) {
            throw new AssertionException(new AssertionError(
                    "Missing response definition for " + responseCode + " response in operation " + operation.getOperationId()));
        }
    }

    void validateOpenAPIOperation(io.swagger.oas.models.Operation operation, String responseCode, String contentAsString, String contentType) throws AssertionException {
        if (operation.getResponses() != null) {
            ApiResponses apiResponses = operation.getResponses();
            ApiResponse apiResponse = apiResponses.get(responseCode);
            if (apiResponse == null) {
                apiResponse = apiResponses.get("default");
            }

            if (apiResponse != null) {
                validateOpenAPIResponse(contentAsString, contentType, apiResponse);
            } else if (strictMode) {
                throw new AssertionException(new AssertionError("Missing response definition for " + responseCode + " response in operation " + operation.getOperationId()));
            }
        }

    }

    void validateResponse(String contentAsString, String contentType, Swagger swagger, Response responseSchema) throws AssertionException {
        if (responseSchema.getSchema() != null) {
            Property schema = responseSchema.getSchema();
            if (schema instanceof RefProperty) {
                Model model = swagger.getDefinitions().get(((RefProperty) schema).getSimpleRef());
                if (model != null) {
                    validatePayload(contentAsString, null, contentType);
                }
            } else {
                validatePayload(contentAsString, Json.pretty(schema), contentType);
            }
        }
    }

    void validateOpenAPIResponse(String contentAsString, String contentType, ApiResponse apiResponse) throws AssertionException {
        contentType = OpenAPIUtils.extractMediaType(contentType);
        if (apiResponse.getContent() != null) {
            MediaType mediaType = apiResponse.getContent().get(contentType);
            if (mediaType != null) {
                if (mediaType.getSchema() != null) {
                    validateOpenApiPayload(contentAsString, Json.pretty(mediaType.getSchema()), contentType);
                }
            }
        }
    }

    private boolean matchesPath(String path, String swaggerPath) {

        String[] pathSegments = path.split("\\/");
        String[] swaggerPathSegments = swaggerPath.split("\\/");

        if (pathSegments.length != swaggerPathSegments.length) {
            return false;
        }

        for (int c = 0; c < pathSegments.length; c++) {
            String pathSegment = pathSegments[c];
            String swaggerPathSegment = swaggerPathSegments[c];

            if (swaggerPathSegment.startsWith("{") && swaggerPathSegment.endsWith("}")) {
                continue;
            } else if (!swaggerPathSegment.equalsIgnoreCase(pathSegment)) {
                return false;
            }
        }

        return true;
    }

    Swagger getSwagger(SubmitContext submitContext) throws AssertionException {
        if (swagger == null && swaggerUrl != null) {
            if (swaggerUrl.startsWith("file:/")) {
                swagger = parseFileContent();
            } else {
                swagger = new SwaggerParser().read(submitContext.expand(swaggerUrl));
            }
            if (swagger == null) {
                throw new AssertionException(new AssertionError("Failed to load Swagger definition from [" + swaggerUrl + "]"));
            }
            swaggerSchema = null;
        }
        return swagger;
    }

    private Swagger parseFileContent() throws AssertionException {
        try {
            SwaggerParser parser = new SwaggerParser();
            Path path = Paths.get(URI.create(swaggerUrl));
            if (Files.exists(path)) {
                return parser.parse(FileUtils.readFileToString(path.toFile(), "UTF-8"));
            } else {
                return parser.parse(ClasspathHelper.loadFileFromClasspath(swaggerUrl));
            }
        } catch (IOException e) {
            throw new AssertionException(new AssertionError("Failed to load Swagger definition from [" + swaggerUrl + "]"));
        }
    }

    public void validateOpenApiPayload(String payload, String schema, String contentType) throws AssertionException {
        try {
            JsonSchema jsonSchema;

            if (schema != null) {
                // make local refs absolute to match existing schema
                schema = schema.replaceAll("\"#\\/definitions\\/", "\"" + swaggerUrl + "#/definitions/");
                JsonNode schemaObject = Json.mapper().readTree(schema);

                // build custom schema factory that preloads existing schema
                JsonSchemaFactory factory = JsonSchemaFactory.newBuilder().setLoadingConfiguration(
                        LoadingConfiguration.newBuilder().preloadSchema(swaggerUrl,
                                Json.mapper().readTree(Json.pretty(openAPI))).freeze()
                ).freeze();
                jsonSchema = factory.getJsonSchema(schemaObject);
            } else {
                jsonSchema = getOpenApiSchema();
            }

            JsonNode contentObject;

            if (contentType.equalsIgnoreCase("application/json")) {
                contentObject = Json.mapper().readTree(payload);
            } else if (contentType.equalsIgnoreCase("application/xml")) {
                final XmlMapper xmlMapper = new XmlMapper();
                contentObject = xmlMapper.readTree(payload);
            } else {
                throw new AssertionException(new AssertionError("Swagger Compliance testing failed. Invalid content type: " + contentType));
            }

            ValidationSupport.validateMessage(jsonSchema, contentObject);
        } catch (AssertionException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionException(new AssertionError("Swagger Compliance testing failed; [" + e.toString() + "]"));
        }
    }


    public void validatePayload(String payload, String schema, String contentType) throws AssertionException {
        try {
            JsonSchema jsonSchema;

            if (schema != null) {
                // make local refs absolute to match existing schema
                schema = schema.replaceAll("\"#\\/definitions\\/", "\"" + swaggerUrl + "#/definitions/");
                JsonNode schemaObject = Json.mapper().readTree(schema);

                // build custom schema factory that preloads existing schema
                JsonSchemaFactory factory = JsonSchemaFactory.newBuilder().setLoadingConfiguration(
                        LoadingConfiguration.newBuilder().preloadSchema(swaggerUrl,
                                Json.mapper().readTree(Json.pretty(swagger))).freeze()
                ).freeze();
                jsonSchema = factory.getJsonSchema(schemaObject);
            } else {
                jsonSchema = getSwaggerSchema();
            }

            JsonNode contentObject;

            if (contentType.equalsIgnoreCase("application/json")) {
                contentObject = Json.mapper().readTree(payload);
            } else if (contentType.equalsIgnoreCase("application/xml")) {
                final XmlMapper xmlMapper = new XmlMapper();
                contentObject = xmlMapper.readTree(payload);
            } else {
                throw new AssertionException(new AssertionError("Swagger Compliance testing failed. Invalid content type: " + contentType));
            }

            ValidationSupport.validateMessage(jsonSchema, contentObject);
        } catch (AssertionException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionException(new AssertionError("Swagger Compliance testing failed; [" + e.toString() + "]"));
        }
    }

    private JsonSchema getSwaggerSchema() throws IOException, ProcessingException {
        if (swaggerSchema == null) {
            JsonNode schemaObject = Json.mapper().readTree(Json.pretty(swagger));
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            swaggerSchema = factory.getJsonSchema(schemaObject);
        }

        return swaggerSchema;
    }

    private JsonSchema getOpenApiSchema() throws IOException, ProcessingException {
        if (swaggerSchema == null) {
            JsonNode schemaObject = Json.mapper().readTree(Json.pretty(openAPI));
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            swaggerSchema = factory.getJsonSchema(schemaObject);
        }

        return swaggerSchema;
    }

    @Override
    protected String internalAssertProperty(TestPropertyHolder source, String propertyName, MessageExchange messageExchange, SubmitContext context) throws AssertionException {
        return null;
    }

    public void configureAssertion(Map<String, Object> configMap) {
        swaggerUrl = (String) configMap.get(SWAGGER_URL);
        strictMode = Boolean.valueOf((String) configMap.get(STRICT_MODE));
    }

    private OpenAPI getOpenAPI(SubmitContext submitContext) throws AssertionException {
        if (openAPI == null) {
            OpenAPIParser parser = new OpenAPIParser();
            ParseOptions options = new ParseOptions();
            options.setResolveFully(true);
            options.setResolve(true);
            SwaggerParseResult result = parser.readLocation(submitContext.expand(swaggerUrl), null, options);
            if (result.getOpenAPI() == null) {
                throw new AssertionException(new AssertionError("Failed to load OpenAPI definition from [" + swaggerUrl + "]"));
            } else {
                openAPI = result.getOpenAPI();
            }
        }
        return openAPI;
    }

    private PathItem findPathItem(String endpoint) {
        for (Server server : openAPI.getServers()) {
            String regex = OpenAPIUtils.generateRegexForServerUrl(server);
            if (regex != null) {
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(endpoint);
                String resourcePath = m.replaceAll("/");
                return findPathInDefinition(resourcePath);
            } else {
                if (endpoint.startsWith(server.getUrl())) {
                    String resourcePath = endpoint.substring(server.getUrl().length());
                    if (resourcePath.contains("?")) {
                        resourcePath = resourcePath.substring(0, resourcePath.indexOf("?"));
                    }
                    return findPathInDefinition(resourcePath);
                }
            }
        }
        return null;
    }

    private boolean isPathContainsTemplateSymbols(String path) {
        return path.contains("{") && path.contains("}");
    }

    private PathItem findPathInDefinition(String resourcePath) {
        if (openAPI.getPaths().keySet().contains(resourcePath)) {
            return openAPI.getPaths().get(resourcePath);
        } else {
            for (String path : openAPI.getPaths().keySet()) {
                if (isPathContainsTemplateSymbols(resourcePath) && OpenAPIUtils.compareTemplateWithPath(resourcePath, path)) {
                    return openAPI.getPaths().get(path);
                }
            }
        }
        return null;
    }
}

