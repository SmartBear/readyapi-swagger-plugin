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
import com.eviware.soapui.support.UISupport
import com.eviware.x.dialogs.Worker
import com.eviware.x.dialogs.XProgressDialog
import com.eviware.x.dialogs.XProgressMonitor
import groovy.json.JsonSlurper

/**
 * Created by ole on 16/09/14.
 */
class SwaggerUtils {
    public static final String DEFAULT_MEDIA_TYPE = "application/json";
    public static final boolean DEFAULT_FOR_CREATE_TEST_CASE = false;

    /**
     * Selects the appropriate SwaggerImporter for the specified URL. For .yaml urls the Swagger2Importer
     * is returned. For .xml urls the Swagger1Importer is returned. For other urls the Swagger2Importer will be
     * returned if the file is json and contains a root attribute named "swagger" or "swaggerVersion" with the
     * value of 2.0.
     *
     * @param url
     * @param project
     * @param defaulMediaType
     * @return the corresponding SwaggerImporter based on the described "algorithm"
     */

    static SwaggerImporter createSwaggerImporter(String url, WsdlProject project, String defaulMediaType,
                                                 boolean generateTestCase) {

        if (url.endsWith(".yaml"))
            return new Swagger2Importer(project, defaulMediaType, generateTestCase)

        if (url.endsWith(".xml"))
            return new Swagger1XResourceListingImporter(project, defaulMediaType)

        def conn = new URL(url).openConnection()
        conn.addRequestProperty("Accept", "*/*")

        def json = new JsonSlurper().parseText(conn.inputStream.text)

        if (String.valueOf(json?.swagger) == "2.0" || String.valueOf(json?.swaggerVersion) == "2.0")
            return new Swagger2Importer(project, defaulMediaType)
        else {
            def version = json?.swaggerVersion
            // in 1.2 only api-declarations have a basePath, see
            // https://github.com/OAI/OpenAPI-Specification/blob/master/versions/1.2.md#52-api-declaration
            if (version == "1.1") {
                if (json?.models != null || json?.resourcePath != null) {
                    return new Swagger1XApiDeclarationImporter(project, defaulMediaType)
                } else {
                    return new Swagger1XResourceListingImporter(project, defaulMediaType)
                }
            } else {
                if (json?.basePath != null) {
                    return new Swagger1XApiDeclarationImporter(project, defaulMediaType)
                } else {
                    return new Swagger1XResourceListingImporter(project, defaulMediaType)
                }
            }
        }
    }


    static SwaggerImporter createSwaggerImporter(String url, WsdlProject project, String defaulMediaType) {
        return createSwaggerImporter(url, project, defaulMediaType, DEFAULT_FOR_CREATE_TEST_CASE)
    }

    static SwaggerImporter createSwaggerImporter(String url, WsdlProject project) {
        return createSwaggerImporter(url, project, DEFAULT_MEDIA_TYPE,
                DEFAULT_FOR_CREATE_TEST_CASE)
    }

    @Deprecated
    static SwaggerImporter createSwaggerImporter(String url, WsdlProject project, boolean removedParameterForRefactoring) {
        return createSwaggerImporter(url, project)
    }

    static SwaggerImporter importSwaggerFromUrl(
            final WsdlProject project, final String finalExpUrl) throws Exception {
        return importSwaggerFromUrl(project, finalExpUrl, "application/json");
    }

    static SwaggerImporter importSwaggerFromUrl(final WsdlProject project,
                                                final String finalExpUrl,
                                                final String defaultMediaType) throws Exception {

        final SwaggerImporter importer = SwaggerUtils.createSwaggerImporter(finalExpUrl, project, defaultMediaType);

        XProgressDialog dlg = UISupport.getDialogs().createProgressDialog("Importing Swagger", 0, "", false);
        dlg.run(new Worker.WorkerAdapter() {
            @Override
            public Object construct(XProgressMonitor xProgressMonitor) {

                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(SwaggerUtils.class.getClassLoader());

                // create the importer and import!
                List<RestService> result = new ArrayList<RestService>();

                try {
                    result.addAll(Arrays.asList(importer.importSwagger(finalExpUrl)));

                    // select the first imported REST Service (since a swagger definition can
                    // define multiple APIs
                    if (!result.isEmpty()) {
                        UISupport.selectAndShow(result.get(0));
                    }
                }
                catch (Throwable t) {
                    UISupport.showErrorMessage(t);
                }
                finally {
                    Thread.currentThread().setContextClassLoader(contextClassLoader);
                }

                return null;
            }
        });

        return importer;
    }
}
