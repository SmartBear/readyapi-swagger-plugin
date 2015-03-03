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

    /**
     * Selects the appropriate SwaggerImporter for the specified URL. For .yaml urls the Swagger2Importer
     * is returned. For .xml urls the Swagger1Importer is returned. For other urls the Swagger2Importer will be
     * returned if the file is json and contains a root attribute named "swagger" or "swaggerVersion" with the
     * value of 2.0.
     *
     * @param url
     * @param project
     * @return the corresponding SwaggerImporter based on the described "algorithm"
     */

    static SwaggerImporter createSwaggerImporter(String url, WsdlProject project) {

        if (url.endsWith(".yaml"))
            return new Swagger2Importer(project)

        if (url.endsWith(".xml"))
            return new Swagger1XImporter(project)

        def json = new JsonSlurper().parseText(new URL(url).text)

        if (String.valueOf(json?.swagger) == "2.0" || String.valueOf(json?.swaggerVersion) == "2.0")
            return new Swagger2Importer(project)
        else
            return new Swagger1XImporter(project)
    }

    static SwaggerImporter importSwaggerFromUrl(
            final WsdlProject project, final String finalExpUrl, final boolean isResourceListing) throws Exception {

        final SwaggerImporter importer = SwaggerUtils.createSwaggerImporter(finalExpUrl, project);

        XProgressDialog dlg = UISupport.getDialogs().createProgressDialog("Importing Swagger", 0, "", false);
        dlg.run(new Worker.WorkerAdapter() {
            @Override
            public Object construct(XProgressMonitor xProgressMonitor) {
                // create the importer and import!
                List<RestService> result = new ArrayList<RestService>();

                try {
                    if (isResourceListing) {
                        result.addAll(Arrays.asList(importer.importSwagger(finalExpUrl)));
                    } else {
                        result.add(importer.importApiDeclaration(finalExpUrl));
                    }

                    // select the first imported REST Service (since a swagger definition can
                    // define multiple APIs
                    if (!result.isEmpty()) {
                        UISupport.selectAndShow(result.get(0));
                    }
                }
                catch (Throwable t) {
                    UISupport.showErrorMessage(t);
                }

                return null;
            }
        });

        return importer;
    }
}
