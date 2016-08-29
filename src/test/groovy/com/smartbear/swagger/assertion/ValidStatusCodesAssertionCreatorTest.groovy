package com.smartbear.swagger.assertion

import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion
import com.eviware.soapui.security.assertion.ValidHttpStatusCodesAssertion
import com.eviware.soapui.utils.ModelItemFactory
import io.swagger.models.Response
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.instanceOf
import static org.junit.Assert.assertThat

class ValidStatusCodesAssertionCreatorTest {
    private ValidStatusCodesAssertionCreator validStatusCodesAssertionCreator
    private RestTestRequestStep restTestRequestStep

    @Before
    public void setUp() throws Exception {
        validStatusCodesAssertionCreator = new ValidStatusCodesAssertionCreator()
        restTestRequestStep = ModelItemFactory.makeRestTestRequestStep()
    }

    @Test
    public void createsValidStatusCodesAssertion() throws Exception {
        Map<String, Response> responseMap = ['200': new Response(), '201': new Response()]
        validStatusCodesAssertionCreator.createAssertion(restTestRequestStep, responseMap, [:])
        WsdlMessageAssertion assertion = restTestRequestStep.getAssertionAt(0)
        assertThat(assertion, CoreMatchers.is(instanceOf(ValidHttpStatusCodesAssertion.class)))
        assertThat(((ValidHttpStatusCodesAssertion) assertion).getCodes(), CoreMatchers.is('200,201'))
    }

    @Test
    public void doesNotCreateAssertionIfDefaultResponseIsSpecified() throws Exception {
        Map<String, Response> responseMap = ['200': new Response(), 'default': new Response()]
        validStatusCodesAssertionCreator.createAssertion(restTestRequestStep, responseMap, [:])
        assertThat(restTestRequestStep.getAssertionCount(), CoreMatchers.is(0))
    }
}
