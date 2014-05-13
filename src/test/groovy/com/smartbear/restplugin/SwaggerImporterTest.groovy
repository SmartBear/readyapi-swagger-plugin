/**
 *  Copyright 2013 SmartBear Software, Inc.
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

		RestService [] result = importer.importSwagger( "http://petstore.swagger.wordnik.com/api/api-docs" )

        RestService service = result[0]
        assertTrue( service.endpoints.length > 0 )

		assertEquals( "http://petstore.swagger.wordnik.com", service.endpoints[0])
		assertEquals( "/api", service.basePath, )

		service = result[1]
		assertEquals( "http://petstore.swagger.wordnik.com", service.endpoints[0])
		assertEquals( "/api", service.basePath, )
	}

    void testImportApiDeclaration()
    {
        def project = new WsdlProject();

        SwaggerImporter importer = new SwaggerImporter( project )
        def service = importer.importApiDeclaration("http://www.apihub.com/apihub/swagger-api/9528/commits")

        assertEquals( 3, service.resourceList.size() )

        importer.importApiDeclaration(new File( "src/test/resources/api-docs").toURL().toString());
    }
}
