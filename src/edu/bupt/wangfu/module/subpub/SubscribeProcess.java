package edu.bupt.wangfu.module.subpub;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 *  Created by HanB on 2017/1/10.
 */
@WebService(name = "SubscribeProcessService", targetNamespace = "http://subscribe.wangfu.bupt.edu/")
@XmlSeeAlso({})
public interface SubscribeProcess {
    /**
     * @param arg0
     * @return returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "subscribeProcess", targetNamespace = "http://subscribe.wangfu.bupt.edu/", className = "subscribeProcess")
    @ResponseWrapper(localName = "subscribeProcessResponse", targetNamespace = "http://subscribe.wangfu.bupt.edu/", className = "subscribeProcessResponse")
    public String subscribeProcess(
		    @WebParam(name = "arg0", targetNamespace = "")
				    String arg0);

}
