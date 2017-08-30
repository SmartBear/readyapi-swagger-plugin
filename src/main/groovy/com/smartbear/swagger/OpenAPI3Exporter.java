package com.smartbear.swagger;

import com.eviware.soapui.config.AuthEntryTypeConfig;
import com.eviware.soapui.config.OAuth2FlowConfig;
import com.eviware.soapui.impl.AuthRepository.AuthEntries.BaseAuthEntry;
import com.eviware.soapui.impl.AuthRepository.AuthRepository;
import com.eviware.soapui.impl.rest.OAuth2Profile;
import com.eviware.soapui.impl.rest.RestMethod;
import com.eviware.soapui.impl.rest.RestRepresentation;
import com.eviware.soapui.impl.rest.RestRequest;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.rest.RestResource;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.support.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.oas.models.Components;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.Paths;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.oas.models.security.OAuthFlow;
import io.swagger.oas.models.security.OAuthFlows;
import io.swagger.oas.models.security.Scopes;
import io.swagger.oas.models.security.SecurityScheme;
import io.swagger.oas.models.servers.Server;
import io.swagger.parser.v3.ObjectMapperFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.DELETE;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.HEAD;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.OPTIONS;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.PATCH;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.POST;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.PUT;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.TRACE;
import static com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle.HEADER;
import static com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle.QUERY;
import static com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle.TEMPLATE;
import static com.smartbear.swagger.OpenAPIUtils.xmlSchemaTypesToJsonTypes;


public class OpenAPI3Exporter implements SwaggerExporter {
    private final WsdlProject project;

    public OpenAPI3Exporter(WsdlProject project) {
        this.project = project;
    }

    public String exportToFolder(String path, String apiVersion, String format, RestService[] services, String basePath) {
        OpenAPI openAPI = new OpenAPI();
        Info info = new Info();
        info.setVersion("1.0.0");
        info.setDescription(project.getDescription() == null ? "" : project.getDescription());
        info.setTitle(project.getName());
        openAPI.setInfo(info);

        Paths paths = new Paths();
        for (RestService restService : services) {
            copyEndpoints(openAPI, restService);
            for (RestResource restResource : restService.getResourceList()) {
                PathItem pathItem = new PathItem();
                for (RestMethod restMethod : restResource.getRestMethodList()) {
                    addMethodToPath(pathItem, restMethod);
                }
                copyParametersToPath(pathItem, restResource.getParams());
                pathItem.setDescription(restResource.getDescription() == null ? "" : restResource.getDescription());
                paths.addPathItem(restResource.getFullPath(false), pathItem);
            }
        }
        openAPI.setPaths(paths);
        createSecurityComponent(openAPI);

        ObjectMapper mapper = format.equals("yaml") ? ObjectMapperFactory.createYaml() : ObjectMapperFactory.createJson();

        try {
            mapper.writeValue(new FileWriter(path + File.separatorChar + "api-docs." + format), openAPI);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path;
    }


    private void addMethodToPath(PathItem pathItem, RestMethod restMethod) {
        if (pathItem != null && restMethod != null) {
            if (restMethod.getMethod().equals(RestRequestInterface.HttpMethod.GET)) {
                pathItem.setGet(createOperation(restMethod));
            } else if (restMethod.getMethod().equals(POST)) {
                pathItem.setPost(createOperation(restMethod));
            } else if (restMethod.getMethod().equals(DELETE)) {
                pathItem.setDelete(createOperation(restMethod));
            } else if (restMethod.getMethod().equals(HEAD)) {
                pathItem.setHead(createOperation(restMethod));
            } else if (restMethod.getMethod().equals(TRACE)) {
                pathItem.setTrace(createOperation(restMethod));
            } else if (restMethod.getMethod().equals(PUT)) {
                pathItem.setPut(createOperation(restMethod));
            } else if (restMethod.getMethod().equals(OPTIONS)) {
                pathItem.setOptions(createOperation(restMethod));
            } else if (restMethod.getMethod().equals(PATCH)) {
                pathItem.setPatch(createOperation(restMethod));
            }
        }
    }

    private Operation createOperation(RestMethod restMethod) {
        Operation operation = new Operation();
        operation.setDescription(restMethod.getDescription() == null ? "" : restMethod.getDescription());
        copyParametersToOperation(operation, restMethod.getParams());
        createResponses(operation, restMethod);
        createRequestBodies(operation, restMethod);
        return operation;
    }

    private void copyParametersToOperation(Operation operation, RestParamsPropertyHolder propertyHolder) {
        for (String propertyName : propertyHolder.getPropertyNames()) {
            RestParamProperty paramProperty = propertyHolder.getProperty(propertyName);
            Parameter parameter = new Parameter();
            parameter.setName(propertyName);
            parameter.setIn(convertReadyApiParameterToOpenApiParameterType(paramProperty));
            parameter.setSchema(xmlSchemaTypesToJsonTypes(paramProperty.getSchemaType()));
            parameter.setRequired(paramProperty.getRequired());
            operation.addParametersItem(parameter);
        }
    }

    private void copyParametersToPath(PathItem pathItem, RestParamsPropertyHolder propertyHolder) {
        for (String propertyName : propertyHolder.getPropertyNames()) {
            RestParamProperty paramProperty = propertyHolder.getProperty(propertyName);
            Parameter parameter = new Parameter();
            parameter.setName(propertyName);
            parameter.setIn(convertReadyApiParameterToOpenApiParameterType(paramProperty));
            pathItem.addParametersItem(parameter);
        }
    }

    private static String convertReadyApiParameterToOpenApiParameterType(RestParamProperty restParamProperty) {
        if (restParamProperty.getStyle().equals(HEADER)) {
            return "header";
        } else if (restParamProperty.getStyle().equals(QUERY)) {
            return "query";
        } else if (restParamProperty.getStyle().equals(TEMPLATE)) {
            return "path";
        } else {
            return "cookie";
        }
    }

    private static void createResponses(Operation operation, RestMethod restMethod) {
        ApiResponses apiResponses = new ApiResponses();
        for (RestRepresentation representation : restMethod.getRepresentations()) {
            ApiResponse response;
            if (apiResponses.containsKey(representation.getStatus())) {
                response = apiResponses.get(representation.getStatus());
            } else {
                response = new ApiResponse();
                response.setDescription(representation.getDescription() == null ? "" : representation.getDescription());
                response.setContent(new Content());
                if (org.apache.commons.lang3.StringUtils.isNumeric(representation.getStatus().toString())) {
                    apiResponses.put(representation.getStatus().toString(), response);
                }
            }
            if (!response.getContent().containsKey(representation.getMediaType()) && StringUtils.hasContent(representation.getMediaType())) {
                MediaType mediaType = new MediaType();
                mediaType.setExample(StringUtils.hasContent(representation.getDefaultContent()) ? representation.getDefaultContent() : "");
                response.getContent().put(representation.getMediaType(), mediaType);
            }
        }
        if (apiResponses.isEmpty()) {
            ApiResponse defaultResponse = new ApiResponse();
            defaultResponse.setDescription("Default response");
            apiResponses.setDefault(defaultResponse);
        }
        operation.setResponses(apiResponses);
    }

    private static void createRequestBodies(Operation operation, RestMethod restMethod) {
        RequestBody requestBody = new RequestBody();
        Content content = new Content();
        requestBody.setContent(content);
        for (RestRequest restRequest : restMethod.getRequestList()) {
            if (!content.containsKey(restRequest.getMediaType())) {
                MediaType mediaType = new MediaType();
                if (StringUtils.hasContent(restRequest.getRequestContent())) {
                    mediaType.setExample(restRequest.getRequestContent());
                }
                content.put(restRequest.getMediaType(), mediaType);
            }
        }
        operation.setRequestBody(requestBody);
    }

    private void copyEndpoints(OpenAPI openAPI, RestService restService) {
        List<Server> serverList = new ArrayList<Server>();
        for (String endpoint : restService.getEndpoints()) {
            Server server = new Server();
            server.setUrl(endpoint + restService.getBasePath());
            serverList.add(server);
        }
        openAPI.setServers(serverList);
    }

    private void createSecurityComponent(OpenAPI openAPI) {
        AuthRepository authRepository = project.getAuthRepository();
        HashMap<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();

        if (authRepository != null && !authRepository.getEntryList().isEmpty()) {
            for (BaseAuthEntry authEntry : project.getAuthRepository().getEntryList()) {
                if (authEntry.getType().equals(AuthEntryTypeConfig.O_AUTH_2_0)) {
                    copyOauthProfile(authEntry, securitySchemes);
                } else if (authEntry.getType().equals(AuthEntryTypeConfig.BASIC)) {
                    createBasicSecurity(authEntry, securitySchemes);
                }
            }
        }

        if (!securitySchemes.isEmpty()) {
            openAPI.setComponents(new Components());
            openAPI.getComponents().setSecuritySchemes(securitySchemes);
        }
    }

    private Scopes extractScopes(OAuth2Profile auth2Profile) {
        Scopes scopes = new Scopes();
        if (StringUtils.hasContent(auth2Profile.getScope())) {
            String[] extractedScopes = auth2Profile.getScope().split(" ");
            for (String scope : extractedScopes) {
                scopes.put(scope, "");
            }
        }
        return scopes;
    }

    private void copyOauthProfile(BaseAuthEntry authEntry, HashMap<String, SecurityScheme> securitySchemes) {
        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.setType(SecurityScheme.Type.OAUTH2);
        OAuthFlows oAuthFlows = new OAuthFlows();
        OAuth2Profile oAuth2Profile = (OAuth2Profile) authEntry;
        OAuthFlow oAuthFlow = new OAuthFlow();

        if (oAuth2Profile.getOAuth2Flow().equals(OAuth2FlowConfig.IMPLICIT_GRANT)) {
            oAuthFlow.setAuthorizationUrl(oAuth2Profile.getAuthorizationURI() == null ? "" : oAuth2Profile.getAuthorizationURI());
            oAuthFlow.setScopes(extractScopes(oAuth2Profile));
            oAuthFlows.setImplicit(oAuthFlow);
        } else if (oAuth2Profile.getOAuth2Flow().equals(OAuth2FlowConfig.AUTHORIZATION_CODE_GRANT)) {
            oAuthFlow.setAuthorizationUrl(oAuth2Profile.getAuthorizationURI() == null ? "" : oAuth2Profile.getAuthorizationURI());
            oAuthFlow.setTokenUrl(oAuth2Profile.getAccessTokenURI() == null ? "" : oAuth2Profile.getAccessTokenURI());
            oAuthFlow.setScopes(extractScopes(oAuth2Profile));
            oAuthFlows.setAuthorizationCode(oAuthFlow);
        } else if (oAuth2Profile.getOAuth2Flow().equals(OAuth2FlowConfig.CLIENT_CREDENTIALS_GRANT)) {
            oAuthFlow.setTokenUrl(oAuth2Profile.getAccessTokenURI() == null ? "" : oAuth2Profile.getAccessTokenURI());
            oAuthFlow.setScopes(extractScopes(oAuth2Profile));
            oAuthFlows.setClientCredentials(oAuthFlow);
        } else if (oAuth2Profile.getOAuth2Flow().equals(OAuth2FlowConfig.RESOURCE_OWNER_PASSWORD_CREDENTIALS)) {
            oAuthFlow.setTokenUrl(oAuth2Profile.getAccessTokenURI() == null ? "" : oAuth2Profile.getAccessTokenURI());
            oAuthFlow.setScopes(extractScopes(oAuth2Profile));
            oAuthFlows.setPassword(oAuthFlow);
        }
        securityScheme.setFlows(oAuthFlows);
        securitySchemes.put(authEntry.getName(), securityScheme);
    }

    private void createBasicSecurity(BaseAuthEntry authEntry, HashMap<String, SecurityScheme> securitySchemes) {
        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.setType(SecurityScheme.Type.HTTP);
        securityScheme.setScheme("basic");
        securitySchemes.put(authEntry.getName(), securityScheme);
    }
}
