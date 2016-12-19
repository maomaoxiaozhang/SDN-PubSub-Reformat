package edu.bupt.wangfu.subscribe;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * @ Created by HanB on 2016/12/7.
 */

@WebService(name = "WsnSPRegisterService", targetNamespace = "http://ws.subpub.module.wangfu.bupt.edu/")
@XmlSeeAlso({})
public interface WsnSPRegister {
    /**
     *
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "wsnServerMethod", targetNamespace = "http://ws.subpub.module.wangfu.bupt.edu/", className = "wsnServerMethod")
    @ResponseWrapper(localName = "wsnServerMethodResponse", targetNamespace = "http://ws.subpub.module.wangfu.bupt.edu/", className = "wsnServerMethodResponse")
    public String wsnServerMethod(
            @WebParam(name = "arg0", targetNamespace = "")
                    String arg0);

}
