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

import com.eviware.soapui.impl.rest.mock.RestMockRequest;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockRunContext;
import com.eviware.soapui.model.mock.MockResult;
import com.eviware.soapui.model.mock.MockRunner;
import com.eviware.soapui.model.support.MockRunListenerAdapter;
import com.eviware.soapui.plugins.ListenerConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ListenerConfiguration()
public class SwaggerGeneratorMockRunListener extends MockRunListenerAdapter {
    @Override
    public MockResult onMockRequest(MockRunner runner, HttpServletRequest request, HttpServletResponse response) {
        if (request.getMethod().toLowerCase().equals("get") && request.getPathInfo().equals("/api-docs.json")) {
            try {
                return new Swagger2FromVirtGenerator(new RestMockRequest(request, response, (WsdlMockRunContext) runner.getMockContext())).generate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
