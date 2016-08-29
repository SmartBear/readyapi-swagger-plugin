package com.smartbear.swagger.assertion

import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import io.swagger.models.Response


class Assertions {
    static AssertionCreator[] assertionCreators = [new ValidStatusCodesAssertionCreator(),
                                                   new SwaggerComplianceAssertionCreator()]

    static void addAssertions(RestTestRequestStep restTestRequestStep, Map<String, Response> responseMap,
                              Map<String, Object> context) {
        assertionCreators.each {
            it.createAssertion(restTestRequestStep, responseMap, context)
        }
    }
}
