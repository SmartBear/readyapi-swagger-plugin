/**
 * Copyright 2013-2016 SmartBear Software, Inc.
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
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.ClasspathHelper;
import io.swagger.util.Json;
import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.XmlObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@PluginTestAssertion(id = "SwaggerComplianceAssertion", label = "Swagger Compliance Assertion",
    category = AssertionCategoryMapping.STATUS_CATEGORY,
    description = "Asserts that the response message is compliant with a Swagger definition")
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

    /**
     * Assertions need to have a constructor that takes a TestAssertionConfig and the ModelItem to be asserted
     */

    public SwaggerComplianceAssertion(TestAssertionConfig assertionConfig, Assertable modelItem) {
        super(assertionConfig, modelItem, true, false, false, true);

        XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(getConfiguration());
        swaggerUrl = reader.readString(SWAGGER_URL, null);
        strictMode = reader.readBoolean(STRICT_MODE, true);
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
        swaggerSchema = null;
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
        Swagger swagger = getSwagger(submitContext);

        HttpTestRequestInterface<?> testRequest = ((HttpTestRequestInterface) messageExchange.getModelItem());
        RestRequestInterface.HttpMethod method = testRequest.getMethod();

        URL endpoint = new URL(messageExchange.getEndpoint());
        String path = endpoint.getPath();
        if (path != null) {
            if (swagger.getBasePath() != null && path.startsWith(swagger.getBasePath())) {
                path = path.substring(swagger.getBasePath().length());
            }

            for (String swaggerPath : swagger.getPaths().keySet()) {

                if (matchesPath(path, swaggerPath)) {

                    Operation operation = findOperation(swagger.getPath(swaggerPath), method);
                    if (operation != null) {
                        validateOperation(swagger, operation, String.valueOf(messageExchange.getResponseStatusCode()),
                            messageExchange.getResponseContent()
                        );

                        return true;
                    } else {
                        throw new AssertionException(new AssertionError(
                            "Failed to find " + method + " method for path [" + path + "] in Swagger definition"));
                    }
                }
            }

            throw new AssertionException(new AssertionError("Failed to find matching path for [" + path + "] in Swagger definition"));
        }

        return false;
    }

    private Operation findOperation(Path path, RestRequestInterface.HttpMethod method) {
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

    void validateOperation(Swagger swagger, Operation operation, String responseCode, String contentAsString) throws AssertionException {

        Response responseSchema = operation.getResponses().get(responseCode);
        if (responseSchema == null) {
            responseSchema = operation.getResponses().get("default");
        }

        if (responseSchema != null) {
            validateResponse(contentAsString, swagger, responseSchema);
        } else if (strictMode) {
            throw new AssertionException(new AssertionError(
                "Missing response for a " + responseCode + " response for operation " + operation.toString() + " in Swagger definition"));
        }
    }

    void validateResponse(String contentAsString, Swagger swagger, Response responseSchema) throws AssertionException {
        if (responseSchema.getSchema() != null) {
            Property schema = responseSchema.getSchema();
            if (schema instanceof RefProperty) {
                Model model = swagger.getDefinitions().get(((RefProperty) schema).getSimpleRef());
                if (model != null) {
                    validatePayload(contentAsString, null);
                }
            } else {
                validatePayload(contentAsString, Json.pretty(schema));
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
            java.nio.file.Path path = Paths.get(URI.create(swaggerUrl));
            if (Files.exists(path)) {
                return parser.parse(FileUtils.readFileToString(path.toFile(), "UTF-8"));
            } else {
                return parser.parse(ClasspathHelper.loadFileFromClasspath(swaggerUrl));
            }
        } catch (IOException e) {
            throw new AssertionException(new AssertionError("Failed to load Swagger definition from [" + swaggerUrl + "]"));
        }
    }

    public void validatePayload(String payload, String schema) throws AssertionException {
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

            JsonNode contentObject = Json.mapper().readTree(payload);

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

    @Override
    protected String internalAssertProperty(TestPropertyHolder source, String propertyName, MessageExchange messageExchange, SubmitContext context) throws AssertionException {
        return null;
    }

    public void configureAssertion(Map<String, Object> configMap) {
        swaggerUrl = (String) configMap.get(SWAGGER_URL);
        strictMode = Boolean.valueOf((String) configMap.get(STRICT_MODE));
    }
}

