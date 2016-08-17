package com.smartbear.swagger

import com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod
import com.eviware.soapui.impl.rest.RestResource
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.eviware.soapui.impl.wsdl.WsdlTestSuite
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import com.eviware.soapui.impl.wsdl.teststeps.registry.RestRequestStepFactory
import com.eviware.soapui.model.testsuite.TestProperty
import io.swagger.models.Operation
import io.swagger.models.Path
import io.swagger.models.parameters.AbstractSerializableParameter

class Swagger2TestCaseGenerator {
    static void generateTestCases(WsdlProject project, RestResource resource, Path path) {
        WsdlTestSuite testSuite = createTestSuiteIfNotPresent(project)
        WsdlTestCase testCase = testSuite.addNewTestCase("$resource.name-TestCase")
        resource.restMethodList.each {
            method ->
                method.requestList.each {
                    request ->
                        RestTestRequestStep testStep = (RestTestRequestStep) testCase.addTestStep(RestRequestStepFactory.createConfig(request, request.name))
                        testStep.testRequest.params.each {
                            setParameterValue(it.value, request.method, path, resource.path)
                        }
                }
        }
    }

    private static void setParameterValue(TestProperty parameter, HttpMethod httpMethod, Path swaggerPath, String path) {
        Operation operation = getSwaggerOperation(httpMethod, swaggerPath, path)
        AbstractSerializableParameter swaggerParam = (AbstractSerializableParameter) operation.parameters.find {
            it.name == parameter.name
        }

        if (swaggerParam.default) {
            parameter.setValue(String.valueOf(swaggerParam.default))
        } else if (swaggerParam.example) {
            parameter.setValue(String.valueOf(swaggerParam.example))
        }
    }

    private static Operation getSwaggerOperation(HttpMethod httpMethod, Path swaggerPath, String path) {
        switch (httpMethod) {
            case HttpMethod.GET: return swaggerPath.get
            case HttpMethod.POST: return swaggerPath.post
            case HttpMethod.PUT: return swaggerPath.put
            case HttpMethod.DELETE: return swaggerPath.delete
            case HttpMethod.HEAD: return swaggerPath.head
            case HttpMethod.OPTIONS: return swaggerPath.options
            case HttpMethod.PATCH: return swaggerPath.patch
            default: throw new IllegalStateException("No operation found with HTTP method $httpMethod for path: $path")
        }
    }

    private static WsdlTestSuite createTestSuiteIfNotPresent(WsdlProject project) {
        return project.getTestSuiteCount() > 0 ? project.getTestSuiteAt(0) : project.addNewTestSuite("TestSuite1")
    }
}
