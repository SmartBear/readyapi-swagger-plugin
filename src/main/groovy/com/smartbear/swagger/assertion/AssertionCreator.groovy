package com.smartbear.swagger.assertion

import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import v2.io.swagger.models.Response

interface AssertionCreator {
    void createAssertion(RestTestRequestStep restTestRequestStep, Map<String, Response> responseMap,
                         Map<String, Object> context)
}
