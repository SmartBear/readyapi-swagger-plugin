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
        assertTrue(SwaggerUtils.createSwaggerImporter(url, project) instanceof Swagger1XApiDeclarationImporter)

        url = "https://developer.similarweb.com/api_docs/services/43832.json"
        assertTrue(SwaggerUtils.createSwaggerImporter(url, project) instanceof Swagger2Importer)

        url = "https://pili-forrester.3scale.net/api_docs/services/42764.json"
        assertTrue(SwaggerUtils.createSwaggerImporter(url, project) instanceof Swagger1XApiDeclarationImporter)
    }
}
