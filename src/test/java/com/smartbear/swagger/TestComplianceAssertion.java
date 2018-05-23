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
import com.eviware.soapui.impl.wsdl.WsdlProjectPro;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep;
import com.eviware.soapui.impl.wsdl.teststeps.registry.HttpRequestStepFactory;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.junit.Test;
import v2.io.swagger.models.Swagger;

import java.io.File;
import java.util.HashMap;

import static junit.framework.TestCase.assertTrue;

public class TestComplianceAssertion {

    @Test
    public void testComplianceAssertion() throws Exception {

        // create the project
        WsdlProjectPro project = new WsdlProjectPro();
        WsdlTestCase wsdlTestCase = project.addNewTestSuite("TestSuite").addNewTestCase("TestCase");
        HttpTestRequestStep testStep =
                (HttpTestRequestStep) wsdlTestCase.addTestStep(HttpRequestStepFactory.HTTPREQUEST_TYPE,
                        "Request", "http://petstore.swagger.io/v2/pet/findByTags", "GET");

        testStep.getTestRequest().getParams().addProperty("tags");
        testStep.getTestRequest().setSendEmptyParameters(true);

        // create the assertion
        TestAssertionConfig config = TestAssertionConfig.Factory.newInstance();
        XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
        builder.add("swaggerUrl", new File("src/test/resources/petstore-2.0.json").toURI().toString());
        config.setConfiguration(builder.finish());

        SwaggerComplianceAssertion assertion = new SwaggerComplianceAssertion(config, testStep);
        Swagger swagger = assertion.getSwagger(new WsdlSubmitContext(project));

        assertion.validateOperation(swagger, swagger.getPath("/pet/findByTags").getGet(), "200",
                "[{\"id\":1500,\"category\":{\"id\":0,\"name\":\"\"},\"name\":\"butch\",\"photoUrls\":[\"\"],\"tags\":[{\"id\":0,\"name\":\"\"}],\"status\":\"available\"}]",
                "application/json");

        assertion.validateOpenAPIOperation(createOperation(), "200", "{\"message\":\"hello world\"}", "application/json");

        try {
            assertion.validateOperation(swagger, swagger.getPath("/pet/findByTags").getGet(), "200",
                    "[{\"id\":1500,\"category\":{\"id\":0,\"name\":\"\"},\"name\":\"butch\",\"photoUrdls\":[\"\"],\"tags\":[{\"id\":0,\"name\":\"\"}],\"status\":\"available\"}]",
                    "application/json");

            assertTrue("Validation should have failed", false);


        } catch (Exception e) {
        }

        try {
            assertion.validateOpenAPIOperation(createOperation(), "200", "{\"message\": 123456", "application/json");
            assertTrue("Validation should have failed", false);
        } catch (Exception e) {
        }
    }

    public Operation createOperation() {
        Operation operation = new Operation();
        ApiResponses apiResponses = new ApiResponses();
        ApiResponse apiResponse = new ApiResponse();
        apiResponses.put("200", apiResponse);

        MediaType mediaType = new MediaType();
        Content responseContent = new Content();
        apiResponse.setContent(responseContent);

        Schema schema = new Schema();
        schema.setType("object");
        HashMap properties = new HashMap();
        schema.setProperties(properties);
        mediaType.setSchema(schema);

        Schema subSchema = new Schema();
        subSchema.setType("string");
        properties.put("message", subSchema);

        responseContent.put("application/json", mediaType);
        operation.setResponses(apiResponses);
        return operation;
    }
}
