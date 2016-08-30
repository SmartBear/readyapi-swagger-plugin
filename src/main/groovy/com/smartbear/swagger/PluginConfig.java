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

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;

/**
 * Created by ole on 08/06/14.
 */

@PluginConfiguration(groupId = "com.smartbear.soapui.plugins", name = "Swagger Plugin", version = "2.5.0-SNAPSHOT",
        autoDetect = true, description = "Provides Swagger 1.X/2.0 import/export functionality for REST APIs",
        infoUrl = "https://github.com/SmartBear/readyapi-swagger-plugin")
public class PluginConfig extends PluginAdapter {
    @Override
    public void initialize() {
        super.initialize();
    }
}
