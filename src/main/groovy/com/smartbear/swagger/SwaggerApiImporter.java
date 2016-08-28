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
