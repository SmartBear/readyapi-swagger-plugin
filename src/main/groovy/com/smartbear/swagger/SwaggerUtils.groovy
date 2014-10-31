package com.smartbear.swagger

import com.eviware.soapui.impl.wsdl.WsdlProject
import groovy.json.JsonSlurper

/**
 * Created by ole on 16/09/14.
 */
class SwaggerUtils {
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
}
