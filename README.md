# Ready! API Swagger Plugin

Provider the following swagger-related features in Ready! API:
* Possibility to create projects, Virts and REST APIs from an existing swagger definition
* Possibility to export a swagger definition for any REST API defined in Ready! API 
* Automatically generates a swagger 2.0 definition for REST Virts - available at &lt;Virt endpoint&gt;/api-docs.json
* Supports both Swagger 1.X and 2.0 for imports and exports

Download and install via the Plugin Manager / Repository Browser from inside Ready! API. Older versions are available for SoapUI open-source as well (see below)

Build with 

```
mvn clean install assembly:single
```

### Release History

* Sept 2016 - Version 2.5.1 - Fixed autodetection of Swagger 1.X files and added Testcase generation from Swagger
* July 2016 - Version 2.4.0 - Bug fixes for R!A 1.8.0 release bundling
* June 2016 - Version 2.3.1 - Dependency updates for improved message creation
* Feb 2016 - Version 2.3.0 - Creates sample messages for POST/PUT requests / responses, dependency updates, bugfixes 
* March 2015 - Version 2.1 - Bug-fix release which ties into the updated plugin system in Ready! API and adds dynamic swagger generation for REST Virts 
* September 2014 - Version 2.0 - Initial release for Ready! API with Swagger 2.0 support

The latest above version is available from inside the Ready! API Plugin Manager. 

Previous releases were on sourceforge (and are still available there) 

* Version 2.0 (aligned with version 2.0 of Swagger) - now only available via the Plugin Repository from inside Ready! API and SoapUI Pro - adds Swagger 2.0 support
* Version 0.3.1 - Dependency update to latest swagger4j library - and some internal refactoring
* Version 0.3 - Adds Swagger 1.2 support - uses updated swagger4j library, See [http://olensmar.blogspot.se/2013/11/updated-swagger-support-for-soapui.html](http://olensmar.blogspot.se/2013/11/updated-swagger-support-for-soapui.html)
* Version 0.2 - See [http://olensmar.blogspot.se/2013/05/soapui-swagger-true.html](http://olensmar.blogspot.se/2013/05/soapui-swagger-true.html)
* Version 0.1 - See [http://olensmar.blogspot.se/2012/12/testing-swagger-apis-with-soapui-groovy.html](http://olensmar.blogspot.se/2012/12/testing-swagger-apis-with-soapui-groovy.html)

Download old versions from sourceforge: [https://sourceforge.net/projects/soapui-plugins/files](https://sourceforge.net/projects/soapui-plugins/files)

Please post issues here at GitHub - you can find me on twitter at @olensmar

Thanks for any feedback!

/Ole
