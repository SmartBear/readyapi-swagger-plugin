package com.smartbear.swagger;

import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.smartbear.swagger4j.Swagger
import com.smartbear.swagger4j.SwaggerFormat

/**
 * Basic tests that use the examples at wordnik - if they change these tests will probably break
 *
 * @author Ole Lensmar
 */

class SwaggerExporterTest extends GroovyTestCase {
    void testExportResourceListing() {
        def project = new WsdlProject();

        SwaggerImporter importer = new Swagger2Importer(project)

        RestService[] result = importer.importSwagger("http://petstore.swagger.io/v2/swagger.json")

        assertEquals(1, result.length)

        SwaggerExporter exporter = new Swagger1XExporter(project);
        def listing = exporter.generateResourceListing(result, "1.0", SwaggerFormat.json, "");
        listing.setBasePath(".");

        def path = exporter.exportResourceListing(SwaggerFormat.json, listing, "target/test-export");
        assertNotNull(path)

        def file = new File(path)

        assertTrue(file.exists());

        def resourceListing = Swagger.createReader().readResourceListing(file.toURI())
        assertEquals(resourceListing.apiVersion, listing.apiVersion)
        assertEquals(resourceListing.apiList.size(), listing.apiList.size())
    }
}
