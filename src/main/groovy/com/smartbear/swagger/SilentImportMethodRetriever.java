package com.smartbear.swagger;

/**
 * The getImportMethod() must be defined in an interface, or it will not be accessible to the Ready! API plugin framework.
 */
public interface SilentImportMethodRetriever {

    /**
     *  Used by the Ready! API plugin framework to get an instance of SilentSwaggerImporter.
     */
    @SuppressWarnings("unused")
    Object getImportMethod();
}
