package com.smartbear.restplugin;

import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.wsdl.WsdlProject

/**
 * Basic tests that use the examples at wordnik - if they change these tests will probably break
 * 
 * @author Ole Lensmar
 */

class SwaggerImporterTest  extends GroovyTestCase {
	void testImportResourceListing() {
		def project = new WsdlProject();

		SwaggerImporter importer = new SwaggerImporter( project )

		RestService [] result = importer.importSwagger( "http://petstore.swagger.wordnik.com/api/api-docs.json" )

		assertEquals( 2, result.length )

		RestService service = result[0]
		assertEquals( "http://petstore.swagger.wordnik.com", service.endpoints[0])
		assertEquals( "/api", service.basePath, )
		assertEquals( 6, service.resourceList.size())

		service = result[1]
		assertEquals( "http://petstore.swagger.wordnik.com", service.endpoints[0])
		assertEquals( "/api", service.basePath, )
		assertEquals( 3, service.resourceList.size())
	}

	void testImportApi() {
		def project = new WsdlProject();
		SwaggerImporter importer = new SwaggerImporter( project )

		RestService [] result = importer.importSwagger( "http://petstore.swagger.wordnik.com/api/api-docs.json/user" )

		assertEquals( 1, result.length )

		RestService service = result[0]
		assertEquals( "http://petstore.swagger.wordnik.com", service.endpoints[0])
		assertEquals( "/api", service.basePath, )
		assertEquals( 6, service.resourceList.size())
	}
}
