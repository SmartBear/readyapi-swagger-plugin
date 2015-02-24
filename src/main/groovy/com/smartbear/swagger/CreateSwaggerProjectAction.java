/**
 *  Copyright 2013 SmartBear Software, Inc.
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

package com.smartbear.swagger;

import com.eviware.soapui.analytics.Analytics;
import com.eviware.soapui.impl.WorkspaceImpl;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.plugins.auto.PluginImportMethod;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldListener;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AField.AFieldType;
import com.eviware.x.form.support.AForm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shows a simple dialog for specifying the swagger definition and performs the
 * import
 *
 * @author Ole Lensmar
 */

@PluginImportMethod(label = "Swagger Definition (REST)")
public class CreateSwaggerProjectAction extends AbstractSoapUIAction<WorkspaceImpl> {
    public static final String RESOURCE_LISTING_TYPE = "Resource Listing";
    public static final String API_DECLARATION_TYPE = "API Declaration (Swagger 1.X only)";

    private XFormDialog dialog;

    public CreateSwaggerProjectAction() {
        super("Create Swagger Project", "Creates a new SoapUI Project from a Swagger definition");
    }

    public void perform(WorkspaceImpl workspace, Object param) {
        // initialize form
        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(Form.class);
            dialog.setValue(Form.TYPE, RESOURCE_LISTING_TYPE);
            dialog.getFormField(Form.SWAGGERURL).addFormFieldListener(new XFormFieldListener() {
                @Override
                public void valueChanged(XFormField sourceField, String newValue, String oldValue) {
                    initProjectName(newValue);
                }
            });
        } else {
            dialog.setValue(Form.SWAGGERURL, "");
            dialog.setValue(Form.PROJECT_NAME, "");
        }

        WsdlProject project = null;

        while (dialog.show()) {
            try {
                // get the specified URL
                String url = dialog.getValue(Form.SWAGGERURL).trim();
                if (StringUtils.hasContent(url)) {
                    project = workspace.createProject(dialog.getValue(Form.PROJECT_NAME));

                    // expand any property-expansions
                    String expUrl = PathUtils.expandPath(url, project);

                    // if this is a file - convert it to a file URL
                    if (new File(expUrl).exists()) {
                        expUrl = new File(expUrl).toURI().toURL().toString();
                    }

                    // create the importer and import!
                    SwaggerImporter importer = SwaggerUtils.createSwaggerImporter(expUrl, project);
                    List<RestService> result = new ArrayList<RestService>();

                    if (dialog.getValue(Form.TYPE).equals(RESOURCE_LISTING_TYPE)) {
                        result.addAll(Arrays.asList(importer.importSwagger(expUrl)));
                    } else {
                        result.add(importer.importApiDeclaration(expUrl));
                    }

                    // select the first imported REST Service (since a swagger definition can
                    // define multiple APIs
                    if (!result.isEmpty()) {
                        UISupport.select(result.get(0));
                    }

                    Analytics.trackAction("CreateSwaggerProject");

                    break;
                }
            } catch (Exception ex) {
                UISupport.showErrorMessage(ex);
            }
        }

        if (project != null && project.getInterfaceCount() == 0) {
            workspace.removeProject(project);
        }
    }

    public void initProjectName(String newValue) {
        if (StringUtils.isNullOrEmpty(dialog.getValue(Form.PROJECT_NAME)) && StringUtils.hasContent(newValue)) {
            int ix = newValue.lastIndexOf('.');
            if (ix > 0) {
                newValue = newValue.substring(0, ix);
            }

            ix = newValue.lastIndexOf('/');
            if (ix == -1) {
                ix = newValue.lastIndexOf('\\');
            }

            if (ix != -1) {
                dialog.setValue(Form.PROJECT_NAME, newValue.substring(ix + 1));
            }
        }
    }

    @AForm(name = "Create Swagger Project", description = "Creates a SoapUI Project from the specified Swagger definition")
    public interface Form {
        @AField(name = "Project Name", description = "Name of the project", type = AField.AFieldType.STRING)
        public final static String PROJECT_NAME = "Project Name";

        @AField(name = "Swagger Definition", description = "Location or URL of Swagger definition", type = AFieldType.FILE)
        public final static String SWAGGERURL = "Swagger Definition";

        @AField(name = "Definition Type", description = "Resource Listing or API Declaration",
                type = AFieldType.RADIOGROUP, values = {RESOURCE_LISTING_TYPE, API_DECLARATION_TYPE})
        public final static String TYPE = "Definition Type";
    }

}
