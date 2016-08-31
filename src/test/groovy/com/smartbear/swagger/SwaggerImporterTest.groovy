/**
 *  Copyright 2013-2016 SmartBear Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.smartbear.swagger

import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.eviware.soapui.impl.wsdl.WsdlTestSuite
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionRegistry
import com.eviware.soapui.plugins.auto.PluginTestAssertion
import com.eviware.soapui.plugins.auto.factories.AutoTestAssertionFactory
import com.eviware.soapui.security.assertion.ValidHttpStatusCodesAssertion
import org.hamcrest.CoreMatchers

import static org.junit.Assert.assertThat

/**
 * Basic tests that use the examples at wordnik - if they change these tests will probably break
 *
 * @author Ole Lensmar
 */

class SwaggerImporterTest extends GroovyTestCase {
    void testImportResourceListing() {
        def project = new WsdlProject();

        SwaggerImporter importer = new Swagger2Importer(project)

        RestService[] result = importer.importSwagger("http://petstore.swagger.io/v2/swagger.json")

        RestService service = result[0]
        assertTrue(service.endpoints.length > 0)

        assertEquals("http://petstore.swagger.io", service.endpoints[0])
        assertEquals("/v2", service.basePath,)
    }

    void testImportApiDeclaration() {
        def project = new WsdlProject();
        Swagger1XApiDeclarationImporter importer = new Swagger1XApiDeclarationImporter(project)
        importer.importSwagger(new File("src/test/resources/api-docs").toURI().toString());
    }

    void testImportSwagger2() {
        def project = new WsdlProject();
        SwaggerImporter importer = new Swagger2Importer(project)
        def url = new File("src/test/resources/default swagger.json").toURI().toURL().toString()

        def restService = importer.importSwagger(url)[0]
        assertEquals(2, restService.endpoints.length)

        importer.importSwagger("src/test/resources/default swagger.yaml")[0]
        importer.importSwagger("https://api.rocrooster.net/api-docs.json")[0]
    }

    void testTestCaseGeneration() {
        TestAssertionRegistry.getInstance().addAssertion(new AutoTestAssertionFactory(SwaggerComplianceAssertion.getAnnotation(PluginTestAssertion.class), SwaggerComplianceAssertion.class));
        WsdlProject project = new WsdlProject()
        project.name = 'Rest Project From Swagger'
        Swagger2Importer swagger2Importer = new Swagger2Importer(project, "application/json", true)
        String swaggerUrl = new File("src/test/resources/petstore-2.0.json").toURI().toString()
        swagger2Importer.importSwagger(swaggerUrl)

        //assert test suite it created and number of Test Case is same as number of resources/paths
        assertThat(project.getTestSuiteCount(), CoreMatchers.is(1))
        WsdlTestSuite testSuite = project.getTestSuiteAt(0)
        assertThat(testSuite.getTestCaseCount(), CoreMatchers.is(project.getInterfaceAt(0).getOperationCount()))

        //assert parameters with default value
        RestTestRequestStep testStep = (RestTestRequestStep) testSuite.getTestCaseByName('/pet/findByStatus-TestCase').getTestStepAt(0)
        assertThat(testStep.getTestRequest().getParams().getProperty('status').getValue(), CoreMatchers.is('available'))

        testStep = (RestTestRequestStep) testSuite.getTestCaseByName('/pet/{petId}-TestCase').getTestStepAt(0)
        assertThat(testStep.getTestRequest().getParams().getProperty('petId').getValue(), CoreMatchers.is('0'))

        //valid status codes assertion
        assertThat(testStep.getAssertionAt(0).label, CoreMatchers.is(ValidHttpStatusCodesAssertion.LABEL))

        //valid status codes assertion
        SwaggerComplianceAssertion swaggerComplianceAssertion = (SwaggerComplianceAssertion) testStep.getAssertionAt(1)
        assertThat(swaggerComplianceAssertion.label, CoreMatchers.is("Swagger Compliance Assertion"))
        assertThat(swaggerComplianceAssertion.swaggerUrl, CoreMatchers.is(swaggerUrl))
    }
}
