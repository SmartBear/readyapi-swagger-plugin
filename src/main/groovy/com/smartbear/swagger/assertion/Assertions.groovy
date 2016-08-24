package com.smartbear.swagger.assertion

import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import io.swagger.models.Response


class Assertions {
    static AssertionCreator[] assertionCreators = [new ValidStatusCodesAssertionCreator()]

    static void addAssertions(RestTestRequestStep restTestRequestStep, Map<String, Response> responseMap) {
        assertionCreators.each {
            it.createAssertion(restTestRequestStep, responseMap)
        }
    }
}
