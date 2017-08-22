package com.smartbear.swagger;

import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.rest.mock.RestMockAction;
import com.eviware.soapui.impl.rest.mock.RestMockResponse;
import com.eviware.soapui.model.mock.MockOperation;
import com.eviware.soapui.support.ModelItemNamer;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.servers.Server;
import io.swagger.oas.models.servers.ServerVariable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.DELETE;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.HEAD;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.OPTIONS;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.PATCH;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.POST;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.PUT;
import static com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod.TRACE;

public class OpenAPIUtils {

    public static String generateRegexForServerUrl(Server server) {
        String url = server.getUrl();
        HashMap<String, String> map = new HashMap();
        if (server.getVariables() != null) {
            int variableNumber = 0;
            for (String variableName : server.getVariables().keySet()) {
                String variableTemplate = "{" + variableName + "}";
                String variableRegex = "";
                ServerVariable serverVariable = server.getVariables().get(variableName);
                if (serverVariable.getEnum() != null) {
                    variableRegex = "(";
                    for (int i = 0; i < serverVariable.getEnum().size(); i++) {
                        variableRegex = variableRegex + serverVariable.getEnum().get(i);
                        if (i != serverVariable.getEnum().size() - 1) {
                            variableRegex = variableRegex + "|";
                        } else {
                            variableRegex = variableRegex + ")";
                        }
                    }
                } else {
                    variableRegex = ".*";
                }
                variableNumber++;

                String replacedVariableName = "READY" + variableNumber + "REG";
                map.put(replacedVariableName, variableRegex);
                url = url.replace(variableTemplate, replacedVariableName);
            }
            String regex = replaceUnescaped(url);
            regex = regex + "\\/";

            for (String replaced : map.keySet()) {
                regex = regex.replace(replaced, map.get(replaced));
            }
            return regex;
        } else {
            return null;
        }
    }

    private static String replaceUnescaped(String url) {
        return url.replaceAll("([\\/\\\\\\.\\[\\{\\(\\*\\+\\?\\^\\$\\|??])", "\\\\$1");
    }

    public static Operation extractOperation(PathItem pathItem, RestRequestInterface.HttpMethod method) {
        if (method.equals(RestRequestInterface.HttpMethod.GET) && pathItem.getGet() != null) {
            return pathItem.getGet();
        } else if (method.equals(POST) && pathItem.getPost() != null) {
            return pathItem.getPost();
        } else if (method.equals(DELETE) && pathItem.getDelete() != null) {
            pathItem.getDelete();
        } else if (method.equals(HEAD) && pathItem.getHead() != null) {
            return pathItem.getHead();
        } else if (method.equals(TRACE) && pathItem.getTrace() != null) {
            return pathItem.getTrace();
        } else if (method.equals(PUT) && pathItem.getPut() != null) {
            return pathItem.getPut();
        } else if (method.equals(OPTIONS) && pathItem.getOptions() != null) {
            return pathItem.getOptions();
        } else if (method.equals(PATCH) && pathItem.getPatch() != null) {
            return pathItem.getPatch();
        }
        return null;
    }

    public static List<Operation> extractOperationList(PathItem pathItem) {
        List<Operation> operationList = new ArrayList();
        if (pathItem.getGet() != null) {
            operationList.add(pathItem.getGet());
        }
        if (pathItem.getPost() != null) {
            operationList.add(pathItem.getPost());
        }
        if (pathItem.getDelete() != null) {
            pathItem.getDelete();
        }
        if (pathItem.getHead() != null) {
            operationList.add(pathItem.getHead());
        }
        if (pathItem.getTrace() != null) {
            operationList.add(pathItem.getTrace());
        }
        if (pathItem.getPut() != null) {
            operationList.add(pathItem.getPut());
        }
        if (pathItem.getOptions() != null) {
            operationList.add(pathItem.getOptions());
        }
        if (pathItem.getPatch() != null) {
            operationList.add(pathItem.getPatch());
        }
        return operationList;
    }

    public static boolean compareTemplateWithPath(String path, String templatedPath) {
        String regex = templatedPath.replaceAll("\\{.*?\\}", ".*");
        return path.matches(regex);

    }

    public static String extractMediaType(String mediaType) {
        Pattern pattern = Pattern.compile("\\w+/[-+.\\w]+");
        Matcher m = pattern.matcher(mediaType);
        if (m.find()) {
            return m.group();
        }
        return mediaType;
    }

    public static void createMockResponses(MockOperation mockOperation, OpenAPI openAPI) {
        if (!(mockOperation instanceof RestMockAction) || openAPI == null) {
            return;
        }
        RestMockAction restMockAction = (RestMockAction) mockOperation;
        PathItem pathItem = openAPI.getPaths().get(((RestMockAction) mockOperation).getResourcePath());
        if (pathItem != null) {
            for (Operation operation : extractOperationList(pathItem)) {
                if (operation.getResponses() != null) {
                    for (String responseCode : operation.getResponses().keySet()) {
                        ApiResponse apiResponse = operation.getResponses().get(responseCode);
                        if (apiResponse.getContent() != null) {
                            for (String contentType : apiResponse.getContent().keySet()) {
                                if (isOperationHasSameResponse(restMockAction, contentType, responseCode)) {
                                    continue;
                                }
                                MediaType mediaType = apiResponse.getContent().get(contentType);
                                RestMockResponse restMockResponse = restMockAction.addNewMockResponse(ModelItemNamer.getUniqueName("Response ", restMockAction));
                                restMockResponse.setContentType(contentType);
                                if (StringUtils.isNumeric(responseCode)) {
                                    restMockResponse.setResponseHttpStatus(NumberUtils.toInt(responseCode));
                                }
                                if (mediaType.getExample() != null) {
                                    restMockResponse.setResponseContent(mediaType.getExample());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isOperationHasSameResponse(RestMockAction restMockAction, String contentType, String responseCode) {
        for (RestMockResponse mockResponse : restMockAction.getMockResponses()) {
            if (mockResponse.getResponseHttpStatus() == NumberUtils.toInt(responseCode) && mockResponse.getContentType().equals(contentType)) {
                return true;
            }
        }
        return false;
    }
}
