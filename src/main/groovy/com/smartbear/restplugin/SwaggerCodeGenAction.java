package com.smartbear.restplugin;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;

public class SwaggerCodeGenAction extends AbstractSoapUIAction<WsdlProject> {

    public SwaggerCodeGenAction()
    {
        super( "Swagger CodeGen", "Generates code from Swagger definitions" );
    }
    @Override
    public void perform(WsdlProject wsdlProject, Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
