/**
 *  Copyright 2013-2017 SmartBear Software, Inc.
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

        RestService[] result = importer.importSwagger("src/test/resources/petstore-2.0.json")

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
