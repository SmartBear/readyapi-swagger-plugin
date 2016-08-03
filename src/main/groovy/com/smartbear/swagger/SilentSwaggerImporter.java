package com.smartbear.swagger;

import com.eviware.soapui.impl.WorkspaceImpl;
import com.eviware.soapui.impl.actions.SilentImportMethod;
import com.eviware.soapui.impl.actions.UnsupportedDefinitionException;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.support.action.SoapUIAction;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

public class SilentSwaggerImporter implements SilentImportMethod {
    public boolean acceptsURL(URL url) {
        return url.toString().toLowerCase().contains("swagger.");
    }

    public Collection<Interface> importApi(URL url, WsdlProject wsdlProject) throws UnsupportedDefinitionException {
        SwaggerImporter importer = SwaggerUtils.createSwaggerImporter(url.toString(), wsdlProject);
        Interface[] services = importer.importSwagger(url.toString());
        return Arrays.asList( services );
    }

    public SoapUIAction<WorkspaceImpl> getImportAction() {
        return new CreateSwaggerProjectAction();
    }

    public String getLabel() {
        return "Import Swagger Definition";
    }
}
