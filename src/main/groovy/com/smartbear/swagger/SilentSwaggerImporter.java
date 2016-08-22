package com.smartbear.swagger;

import com.eviware.soapui.impl.WorkspaceImpl;
import com.eviware.soapui.impl.actions.SilentApiImporter;
import com.eviware.soapui.impl.actions.UnsupportedDefinitionException;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.plugins.auto.PluginSilentApiImporter;
import com.eviware.soapui.support.action.SoapUIAction;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("unused")
@PluginSilentApiImporter
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

    public String getFormatName() {
        return "Swagger";
    }

}
