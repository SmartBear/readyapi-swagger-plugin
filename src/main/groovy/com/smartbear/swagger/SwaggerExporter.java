package com.smartbear.swagger;

import com.eviware.soapui.impl.rest.RestService;

/**
 * Created by ole on 16/09/14.
 */
public interface SwaggerExporter {

    String exportToFolder(String path, String apiVersion, String format, RestService[] services, String basePath);
}
