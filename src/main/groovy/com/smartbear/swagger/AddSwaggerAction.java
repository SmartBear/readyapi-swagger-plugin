/**
 * Copyright 2013-2017 SmartBear Software, Inc.
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

import com.eviware.soapui.analytics.Analytics;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AField.AFieldType;
import com.eviware.x.form.support.AForm;

import java.io.File;

/**
 * Shows a simple dialog for specifying the swagger definition and performs the
 * import
 *
 * @author Ole Lensmar
 */

@ActionConfiguration(actionGroup = "EnabledWsdlProjectActions", afterAction = "AddWadlAction", separatorBefore = true)
public class AddSwaggerAction extends AbstractSoapUIAction<WsdlProject> {

    private XFormDialog dialog;

    public AddSwaggerAction() {
        super("Import Swagger/OpenAPI Definition", "Imports a Swagger/OpenAPI definition into SoapUI");
    }

    public void perform(final WsdlProject project, Object param) {
        // initialize form
        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(Form.class);
            dialog.setValue(Form.DEFAULT_MEDIA_TYPE, SwaggerUtils.DEFAULT_MEDIA_TYPE);
        } else {
            dialog.setValue(Form.SWAGGER_URL, "");
        }

        while (dialog.show()) {
            try {
                // get the specified URL
                String url = dialog.getValue(Form.SWAGGER_URL).trim();
                if (StringUtils.hasContent(url)) {
                    // expand any property-expansions
                    String expUrl = PathUtils.expandPath(url, project);

                    // if this is a file - convert it to a file URL
                    if (new File(expUrl).exists()) {
                        expUrl = new File(expUrl).toURI().toURL().toString();
                    }

                    importSwaggerDefinition(project, expUrl,
                            dialog.getValue(Form.DEFAULT_MEDIA_TYPE));
                    break;
                }
            } catch (Exception ex) {
                UISupport.showErrorMessage(ex);
            }
        }
    }

    public SwaggerImporter importSwaggerDefinition(final WsdlProject project,
                                                   final String definitionUrl,
                                                   final String defaultMediaType) throws Exception {
        SwaggerImporter importer = SwaggerUtils.importSwaggerFromUrl(
                project, definitionUrl, defaultMediaType);
        Analytics.trackAction("ImportSwagger", "Importer", importer.getClass().getSimpleName());
        return importer;
    }

    @AForm(name = "Add Swagger/OpenAPI Definition", description = "Creates a REST API from the specified Swagger/OpenAPI definition")
    public interface Form {
        @AField(name = "Swagger/OpenAPI Definition", description = "Location or URL of Swagger/OpenAPI definition", type = AFieldType.FILE)
        String SWAGGER_URL = "Swagger/OpenAPI Definition";

        @AField(name = "Default Media Type", description = "Default Media Type of the responses", type = AFieldType.STRING)
        String DEFAULT_MEDIA_TYPE = "Default Media Type";
    }
}
