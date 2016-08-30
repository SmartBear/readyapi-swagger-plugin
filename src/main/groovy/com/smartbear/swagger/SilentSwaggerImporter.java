/**
 * Copyright 2013-2016 SmartBear Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartbear.swagger;

import com.eviware.soapui.impl.actions.SilentApiImporter;
import com.eviware.soapui.impl.actions.UnsupportedDefinitionException;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.plugins.auto.PluginSilentApiImporter;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("unused")
@PluginSilentApiImporter(formatName = "Swagger")
public class SilentSwaggerImporter implements SilentApiImporter {

    private CreateSwaggerProjectAction createSwaggerProjectAction = new CreateSwaggerProjectAction();

    public boolean acceptsURL(URL url) {
        String urlToLowerCase = url.toString().toLowerCase();
        return urlToLowerCase.endsWith(".yaml") || urlToLowerCase.endsWith(".json") || urlToLowerCase.endsWith(".xml");
    }

    public Collection<Interface> importApi(URL url, WsdlProject wsdlProject) throws UnsupportedDefinitionException {
        SwaggerImporter importer = SwaggerUtils.createSwaggerImporter(url.toString(), wsdlProject);
        Interface[] services = importer.importSwagger(url.toString());
        return Arrays.asList(services);
    }

}
