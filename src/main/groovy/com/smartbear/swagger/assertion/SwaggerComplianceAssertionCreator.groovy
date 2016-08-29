package com.smartbear.swagger.assertion

import com.eviware.soapui.config.TestAssertionConfig
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder
import io.swagger.models.Response

class SwaggerComplianceAssertionCreator implements AssertionCreator {

    public static final String SWAGGER_URL = "swaggerUrl"

    void createAssertion(RestTestRequestStep testStep, Map<String, Response> responseMap,
                         Map<String, Object> context) {
        //Don't type cast to SwaggerComplianceAssertion, it will fail in TestServer - since the class is loaded by two different class loaders (one for plugin and another as direct dependency)
        WsdlMessageAssertion assertion = (WsdlMessageAssertion) testStep.addAssertion("Swagger Compliance Assertion")
        if (assertion != null) { //will be null if plugin not installed
            TestAssertionConfig config = TestAssertionConfig.Factory.newInstance();
            XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
            builder.add(SWAGGER_URL, (String) context.get(SWAGGER_URL));
            config.setConfiguration(builder.finish());
            assertion.setConfiguration(config.getConfiguration())
        }
    }
}
