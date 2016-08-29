package com.smartbear.swagger.assertion

import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionRegistry
import com.eviware.soapui.plugins.auto.PluginTestAssertion
import com.eviware.soapui.plugins.auto.factories.AutoTestAssertionFactory
import com.eviware.soapui.utils.ModelItemFactory
import com.smartbear.swagger.SwaggerComplianceAssertion
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertThat

class SwaggerComplianceAssertionCreatorTest {
    private SwaggerComplianceAssertionCreator complianceAssertionCreator
    private RestTestRequestStep restTestRequestStep

    @Before
    public void setUp() throws Exception {
        TestAssertionRegistry.getInstance().addAssertion(new AutoTestAssertionFactory(
                SwaggerComplianceAssertion.getAnnotation(PluginTestAssertion.class), SwaggerComplianceAssertion.class));

        complianceAssertionCreator = new SwaggerComplianceAssertionCreator()
        restTestRequestStep = ModelItemFactory.makeRestTestRequestStep()
    }

    @Test
    public void addsSwaggerComplianceAssertion() throws Exception {
        String swaggerUrl = new File("src/test/resources/petstore-2.0.json").toURI().toString()
        Map<String, Object> context = [swaggerUrl: swaggerUrl] as Map
        complianceAssertionCreator.createAssertion(restTestRequestStep, [:], context)
        SwaggerComplianceAssertion assertion = (SwaggerComplianceAssertion) restTestRequestStep.getAssertionAt(0)
        assertThat(assertion.swaggerUrl, CoreMatchers.is(swaggerUrl))
    }
}
