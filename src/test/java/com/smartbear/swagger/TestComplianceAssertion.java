package com.smartbear.swagger;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.WsdlProjectPro;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep;
import com.eviware.soapui.impl.wsdl.teststeps.registry.HttpRequestStepFactory;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import io.swagger.models.Swagger;
import org.junit.Test;

import java.io.File;

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
        config.setConfiguration( builder.finish() );

        SwaggerComplianceAssertion assertion = new SwaggerComplianceAssertion(config, testStep);
        Swagger swagger = assertion.getSwagger(new WsdlSubmitContext(project));

        assertion.validateOperation(swagger, swagger.getPath("/pet/findByTags").getGet(), "200", "[{\"id\":1500,\"category\":{\"id\":0,\"name\":\"\"},\"name\":\"butch\",\"photoUrls\":[\"\"],\"tags\":[{\"id\":0,\"name\":\"\"}],\"status\":\"available\"}]");

        try {
            assertion.validateOperation(swagger, swagger.getPath("/pet/findByTags").getGet(), "200", "[{\"id\":1500,\"category\":{\"id\":0,\"name\":\"\"},\"name\":\"butch\",\"photoUrdls\":[\"\"],\"tags\":[{\"id\":0,\"name\":\"\"}],\"status\":\"available\"}]");
            assertTrue("Validation should have failed", false);
        } catch (Exception e) {
        }
    }
}
