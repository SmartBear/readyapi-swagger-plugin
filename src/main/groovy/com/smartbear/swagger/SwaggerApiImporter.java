package com.smartbear.swagger;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.plugins.ApiImporter;
import com.eviware.soapui.plugins.PluginApiImporter;

import java.util.ArrayList;
import java.util.List;

@PluginApiImporter(label = "Swagger 1.X/2.0")
public class SwaggerApiImporter implements ApiImporter {
    public List<Interface> importApis(Project project) {

        List<Interface> result = new ArrayList<Interface>();
        int cnt = project.getInterfaceCount();

        AddSwaggerAction importSwaggerAction = new AddSwaggerAction();
        importSwaggerAction.perform((WsdlProject) project, null);

        for (int c = cnt; c < project.getInterfaceCount(); c++) {
            result.add(project.getInterfaceAt(c));
        }

        return result;
    }
}
