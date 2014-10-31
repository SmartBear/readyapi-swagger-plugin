package com.smartbear.swagger

import com.eviware.soapui.impl.wsdl.WsdlProject

/**
 * Created by ole on 16/09/14.
 */
class SwaggerUtilsTest extends GroovyTestCase {
    public void testSwaggerUtils() {
        WsdlProject project = new WsdlProject()

        def url = new File("src/test/resources/petstore-2.0.yaml").toURL().toString()
        assertTrue(SwaggerUtils.createSwaggerImporter(url, project) instanceof Swagger2Importer)

        url = new File("src/test/resources/petstore-2.0.json").toURL().toString()
        assertTrue(SwaggerUtils.createSwaggerImporter(url, project) instanceof Swagger2Importer)

        url = new File("src/test/resources/api-docs").toURL().toString()
        assertTrue(SwaggerUtils.createSwaggerImporter(url, project) instanceof Swagger1XImporter)

        url = "https://developer.similarweb.com/api_docs/services/43832.json"
        assertTrue(SwaggerUtils.createSwaggerImporter(url, project) instanceof Swagger2Importer)
    }
}
