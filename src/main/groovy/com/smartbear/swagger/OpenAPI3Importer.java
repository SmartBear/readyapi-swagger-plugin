package com.smartbear.swagger;

import com.eviware.soapui.config.AuthEntryTypeConfig;
import com.eviware.soapui.config.OAuth2FlowConfig;
import com.eviware.soapui.impl.AuthRepository.AuthRepository;
import com.eviware.soapui.impl.rest.OAuth2Profile;
import com.eviware.soapui.impl.rest.RestMethod;
import com.eviware.soapui.impl.rest.RestRepresentation;
import com.eviware.soapui.impl.rest.RestRequest;
import com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod;
import com.eviware.soapui.impl.rest.RestResource;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.rest.RestServiceFactory;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.support.ModelItemNamer;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.examples.Example;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.security.OAuthFlows;
import io.swagger.oas.models.security.Scopes;
import io.swagger.oas.models.security.SecurityScheme;
import io.swagger.oas.models.servers.Server;
import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import org.apache.commons.io.IOUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAPI3Importer implements SwaggerImporter {
    private WsdlProject project;
    private HashMap context = new HashMap();
    private OpenAPI3TestCaseGenerator testCaseGenerator = new OpenAPI3TestCaseGenerator();
    private boolean generateTestCase;

    public OpenAPI3Importer(WsdlProject project) {
        this(project, "application/json");
    }

    public OpenAPI3Importer(WsdlProject project, String defaultMediaType) {
        this(project, defaultMediaType, false);
    }

    public OpenAPI3Importer(WsdlProject project, String defaultMediaType, boolean generateTestCase) {
        this.project = project;
        this.generateTestCase = generateTestCase;
    }


    public RestService[] importSwagger(String swaggerUrl) {
        RestService restService;
        OpenAPIParser parser = new OpenAPIParser();
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolve(true);
        SwaggerParseResult result = parser.readLocation(swaggerUrl, null, options);
        restService = createRestService(result, swaggerUrl);

        context.put("swaggerUrl", swaggerUrl);

        return new RestService[]{restService};
    }

    private RestService createRestService(SwaggerParseResult parseResult, String url) {
        if (parseResult == null || parseResult.getOpenAPI() == null) {
            return null;
        }
        OpenAPI openAPI = parseResult.getOpenAPI();
        String name = null;
        if (openAPI.getInfo() != null && openAPI.getInfo().getTitle() != null) {
            name = openAPI.getInfo().getTitle();
        }

        if (name == null) {
            if (url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")) {
                try {
                    name = new URL(url).getHost();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                int ix = url.lastIndexOf('/');
                name = ix == -1 || ix == url.length() - 1 ? url : url.substring(ix + 1);
            }
        }

        RestService restService = (RestService) project.addNewInterface(name, RestServiceFactory.REST_TYPE);
        if (openAPI.getInfo().getDescription() != null) {
            restService.setDescription(openAPI.getInfo().getDescription());
        }
        if (openAPI.getServers() != null) {
            addEndpoints(openAPI.getServers(), restService);
        }
        createResources(openAPI, restService);
        createAuthProfiles(openAPI);

        return restService;
    }

    private void addEndpoints(List<Server> servers, RestService restService) {
        final String DEFAULT_ENDPOINT = "/";
        if (servers != null) {
            for (Server server : servers) {
                String url = server.getUrl();
                if (server.getVariables() != null) {
                    for (String variable : server.getVariables().keySet()) {
                        String defaultValue = server.getVariables().get(variable).getDefault();
                        if (defaultValue == null) {
                            continue;
                        }
                        String variableTemplate = "{" + variable + "}";
                        if (defaultValue != null && url.contains(variableTemplate)) {
                            url = url.replace(variableTemplate, defaultValue);
                        }
                    }
                }
                if (!Arrays.asList(restService.getEndpoints()).contains(url)) {
                    restService.addEndpoint(url);
                }
            }
        }
        if (Arrays.asList(restService.getEndpoints()).contains(DEFAULT_ENDPOINT) && restService.getEndpoints().length > 1) {
            restService.removeEndpoint(DEFAULT_ENDPOINT);
        } else if (restService.getEndpoints().length == 0) {
            restService.addEndpoint(DEFAULT_ENDPOINT);
        }
    }

    private void createResources(OpenAPI openAPI, RestService restService) {
        if (openAPI == null && openAPI.getPaths() == null) {
            return;
        }
        for (String pathKey : openAPI.getPaths().keySet()) {
            PathItem path = openAPI.getPaths().get(pathKey);
            String resourceName = path.getSummary() != null ? path.getSummary() : pathKey;
            RestResource restResource = restService.addNewResource(resourceName, pathKey);
            addEndpoints(path.getServers(), restService);
            addResourceLevelParameters(restResource, path.getParameters());
            createResourceMethods(restResource, path);
        }
    }

    private void addResourceLevelParameters(RestResource restResource, List<Parameter> parameters) {
        if (restResource == null || parameters == null) {
            return;
        }
        for (Parameter parameter : parameters) {
            if (parameter == null && parameter.getDeprecated() != null && parameter.getDeprecated()) {
                continue;
            }
            createParameter(restResource.getParams(), parameter);
        }
    }

    private void createParameter(RestParamsPropertyHolder propertyHolder, Parameter parameter) {
        RestParamProperty paramProperty = propertyHolder.addProperty(parameter.getName());
        if (parameter.getIn().equalsIgnoreCase("query")) {
            paramProperty.setStyle(RestParamsPropertyHolder.ParameterStyle.QUERY);
        } else if (parameter.getIn().equalsIgnoreCase("header")) {
            paramProperty.setStyle(RestParamsPropertyHolder.ParameterStyle.HEADER);
        } else if (parameter.getIn().equalsIgnoreCase("path")) {
            paramProperty.setStyle(RestParamsPropertyHolder.ParameterStyle.TEMPLATE);
        }
    }

    private void createResourceMethods(RestResource restResource, PathItem pathItem) {
        if (restResource == null || pathItem == null) {
            return;
        }
        if (pathItem.getGet() != null) {
            createMethod(restResource, HttpMethod.GET, pathItem.getGet());
        }
        if (pathItem.getPost() != null) {
            createMethod(restResource, HttpMethod.POST, pathItem.getPost());
        }
        if (pathItem.getDelete() != null) {
            createMethod(restResource, HttpMethod.DELETE, pathItem.getDelete());
        }
        if (pathItem.getPut() != null) {
            createMethod(restResource, HttpMethod.PUT, pathItem.getPut());
        }
        if (pathItem.getOptions() != null) {
            createMethod(restResource, HttpMethod.OPTIONS, pathItem.getOptions());
        }
        if (pathItem.getTrace() != null) {
            createMethod(restResource, HttpMethod.TRACE, pathItem.getTrace());
        }
        if (pathItem.getHead() != null) {
            createMethod(restResource, HttpMethod.HEAD, pathItem.getHead());
        }
    }

    private void createMethod(RestResource restResource, HttpMethod method, Operation operation) {
        if (operation == null || (operation.getDeprecated() != null && operation.getDeprecated())) {
            return;
        }
        String name = "";
        if (operation.getOperationId() != null) {
            name = operation.getOperationId();
        } else {
            method.toString();
        }
        RestMethod restMethod = restResource.addNewMethod(name);
        restMethod.setMethod(method);
        if (operation.getDescription() != null) {
            restMethod.setDescription(operation.getDescription());
        } else if (operation.getSummary() != null) {
            restMethod.setDescription(operation.getSummary());
        }
        addMethodLevelParameters(restMethod, operation.getParameters());
        createRepresentations(restMethod, operation);
        addEndpoints(operation.getServers(), restResource.getService());
        createRequest(restMethod, operation);

        if (operation.getResponses() != null && project != null && generateTestCase) {
            testCaseGenerator.generateTestCase(project, restMethod, context, operation.getResponses());
        }
    }

    private void addMethodLevelParameters(RestMethod restMethod, List<Parameter> parameters) {
        if (restMethod == null || parameters == null) {
            return;
        }
        for (Parameter parameter : parameters) {
            if (parameter.getDeprecated() != null && parameter.getDeprecated()) {
                continue;
            }
            createParameter(restMethod.getParams(), parameter);
        }
    }

    private void createRequest(RestMethod restMethod, Operation operation) {
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            Content content = operation.getRequestBody().getContent();
            for (String mediaTypeName : content.keySet()) {
                MediaType mediaType = content.get(mediaTypeName);
                if (mediaType.getExamples() != null) {
                    for (String exampleName : mediaType.getExamples().keySet()) {
                        RestRequest request = restMethod.addNewRequest(exampleName);
                        request.setMediaType(mediaTypeName);
                        Example example = mediaType.getExamples().get(exampleName);
                        if (example.getExternalValue() != null) {
                            try {
                                String externalContent = IOUtils.toString(new URL(example.getExternalValue()), "UTF-8");
                                request.setRequestContent(externalContent);
                            } catch (Exception e) {
                            }
                        } else if (example.getValue() != null) {
                            request.setRequestContent(example.getValue().toString());
                        }
                    }
                } else if (mediaType.getExample() != null) {
                    RestRequest request = restMethod.addNewRequest(ModelItemNamer.getUniqueName("Request", restMethod));
                    request.setRequestContent(mediaType.getExample());
                    request.setMediaType(mediaTypeName);
                }
            }
        }
        restMethod.addNewRequest(ModelItemNamer.getUniqueName("Request", restMethod));
    }

    private void createRepresentations(RestMethod restMethod, Operation operation) {
        if (operation != null && operation.getResponses() != null) {
            for (String httpStatusCode : operation.getResponses().keySet()) {
                if (httpStatusCode.equals("default")) {
                    continue;
                }
                ApiResponse response = operation.getResponses().get(httpStatusCode);
                if (response.getContent() != null) {
                    for (String mediaTypeValue : response.getContent().keySet()) {
                        RestRepresentation representation = restMethod.addNewRepresentation(RestRepresentation.Type.RESPONSE);
                        representation.setMediaType(mediaTypeValue);
                        representation.setStatus(Arrays.asList(httpStatusCode));
                        representation.setDescription(response.getDescription() != null ? response.getDescription() : "");

                        MediaType mediaType = response.getContent().get(mediaTypeValue);
                        if (mediaType.getExample() != null) {
                            representation.setSampleContent(mediaType.getExample());
                        }
                    }
                }
            }
        }
    }

    private void createAuthProfiles(OpenAPI openAPI) {
        if (openAPI.getComponents() != null && openAPI.getComponents().getSecuritySchemes() != null) {
            Map<String, SecurityScheme> securitySchemeMap = openAPI.getComponents().getSecuritySchemes();
            for (String securityName : securitySchemeMap.keySet()) {
                SecurityScheme securityScheme = securitySchemeMap.get(securityName);
                if (securityScheme != null && securityScheme.getType() != null) {
                    if (securityScheme.getType().toString().equals("oauth2")) {
                        createOauth2Profile(securityName, securityScheme);
                    } else if (securityScheme.getType().toString().equals("apikey")) {
                        createApiKey(securityScheme);
                    } else if (securityScheme.getType().toString().equals("http")) {
                        createHttpAuth(securityName, securityScheme);
                    }
                }
            }
        }
    }

    private void createOauth2Profile(String securitySchemeName, SecurityScheme securityScheme) {
        AuthRepository authRepository = project.getAuthRepository();
        if (authRepository != null) {
            OAuthFlows oAuthFlows = securityScheme.getFlows();
            if (oAuthFlows != null) {
                if (oAuthFlows.getImplicit() != null) {
                    OAuth2Profile oAuth2Profile = (OAuth2Profile) authRepository.createEntry(AuthEntryTypeConfig.O_AUTH_2_0, securitySchemeName + " IMPLICIT");
                    oAuth2Profile.setOAuth2Flow(OAuth2FlowConfig.IMPLICIT_GRANT);
                    oAuth2Profile.setAuthorizationURI(oAuthFlows.getImplicit().getAuthorizationUrl() == null ? "" : (oAuthFlows.getImplicit().getAuthorizationUrl()));
                    addScopeToProfile(oAuth2Profile, oAuthFlows.getImplicit().getScopes());
                }

                if (oAuthFlows.getAuthorizationCode() != null) {
                    OAuth2Profile oAuth2Profile = (OAuth2Profile) authRepository.createEntry(AuthEntryTypeConfig.O_AUTH_2_0, securitySchemeName + " AUTHORIZATION");
                    oAuth2Profile.setOAuth2Flow(OAuth2FlowConfig.AUTHORIZATION_CODE_GRANT);
                    oAuth2Profile.setAuthorizationURI(oAuthFlows.getAuthorizationCode().getAuthorizationUrl() == null ? "" : (oAuthFlows.getAuthorizationCode().getAuthorizationUrl()));
                    oAuth2Profile.setAccessTokenURI(oAuthFlows.getAuthorizationCode().getTokenUrl() == null ? "" : oAuthFlows.getAuthorizationCode().getTokenUrl());
                    addScopeToProfile(oAuth2Profile, oAuthFlows.getAuthorizationCode().getScopes());
                }

                if (oAuthFlows.getClientCredentials() != null) {
                    OAuth2Profile oAuth2Profile = (OAuth2Profile) authRepository.createEntry(AuthEntryTypeConfig.O_AUTH_2_0, securitySchemeName + " CLIENT CRED");
                    oAuth2Profile.setOAuth2Flow(OAuth2FlowConfig.CLIENT_CREDENTIALS_GRANT);
                    oAuth2Profile.setAccessTokenURI(oAuthFlows.getClientCredentials().getTokenUrl() == null ? "" : oAuthFlows.getClientCredentials().getTokenUrl());
                    addScopeToProfile(oAuth2Profile, oAuthFlows.getClientCredentials().getScopes());
                }

                if (oAuthFlows.getPassword() != null) {
                    OAuth2Profile oAuth2Profile = (OAuth2Profile) authRepository.createEntry(AuthEntryTypeConfig.O_AUTH_2_0, securitySchemeName + " CLIENT PASS");
                    oAuth2Profile.setOAuth2Flow(OAuth2FlowConfig.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                    oAuth2Profile.setAccessTokenURI(oAuthFlows.getPassword().getTokenUrl() == null ? "" : oAuthFlows.getPassword().getTokenUrl());
                    addScopeToProfile(oAuth2Profile, oAuthFlows.getPassword().getScopes());
                }
            }
        }
    }

    private void addScopeToProfile(OAuth2Profile oAuth2Profile, Scopes scopes) {
        if (scopes != null && oAuth2Profile != null) {
            String scopesSummaryString = "";
            for (String scope : scopes.keySet()) {
                scopesSummaryString = scopesSummaryString + scope + " ";
            }
            oAuth2Profile.setScope(scopesSummaryString.trim());
        }
    }

    private void createApiKey(SecurityScheme securityScheme) {
        if (securityScheme != null && securityScheme.getIn() != null) {
            String type = securityScheme.getIn().toString();
            String name = securityScheme.getName();
            //TODO ADD TO REQUESTS PARAMETERS
            if (type.equalsIgnoreCase("query")) {

            } else if (type.equalsIgnoreCase("header")) {

            }
        }
    }

    private void createHttpAuth(String securitySchemeName, SecurityScheme securityScheme) {
        if (securityScheme != null) {
            String scheme = securityScheme.getScheme();
            AuthRepository authRepository = project.getAuthRepository();
            if (scheme != null) {
                if (scheme.equalsIgnoreCase("basic")) {
                    authRepository.createEntry(AuthEntryTypeConfig.BASIC, securitySchemeName + " BASIC");
                }
            }
        }
    }
}
