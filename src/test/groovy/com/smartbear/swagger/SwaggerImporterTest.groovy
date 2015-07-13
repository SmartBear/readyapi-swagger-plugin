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

package com.smartbear.swagger

import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.wsdl.WsdlProject

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
        Swagger1XImporter importer = new Swagger1XImporter(project)
        importer.importApiDeclaration(new File("src/test/resources/api-docs").toURL().toString());
    }

    void testImportSwagger2() {
        def project = new WsdlProject();
        SwaggerImporter importer = new Swagger2Importer(project)
        def url = new File("src/test/resources/default swagger.json").toURI().toURL().toString()

        def restService = importer.importSwagger(url)[0]
        assertEquals(2, restService.endpoints.length)

        importer.importSwagger("src/test/resources/default swagger.yaml")[0]
    }
}
