package com.smartbear.swagger;

import com.eviware.soapui.impl.rest.RestService;

/**
 * Created by ole on 16/09/14.
 */
public interface SwaggerImporter {
    RestService[] importSwagger(String url);

    RestService importApiDeclaration(String expUrl);
}
