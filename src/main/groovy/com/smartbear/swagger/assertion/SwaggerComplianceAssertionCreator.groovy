package com.smartbear.swagger.assertion

import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import com.smartbear.swagger.SwaggerComplianceAssertion
import io.swagger.models.Response

class SwaggerComplianceAssertionCreator implements AssertionCreator {
    void createAssertion(RestTestRequestStep restTestRequestStep, Map<String, Response> responseMap,
                         Map<String, Object> context) {
        SwaggerComplianceAssertion assertion = (SwaggerComplianceAssertion) restTestRequestStep.addAssertion("Swagger Compliance Assertion")
        assertion.configureAssertion(context)
    }
}
