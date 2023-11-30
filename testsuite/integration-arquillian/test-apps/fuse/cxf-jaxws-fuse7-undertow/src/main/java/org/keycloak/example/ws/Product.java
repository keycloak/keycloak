/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.example.ws;

import org.keycloak.example.ws.types.ObjectFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService
@XmlSeeAlso({ObjectFactory.class})
public interface Product {

    @RequestWrapper(localName = "GetProduct", className = "GetProduct")
    @ResponseWrapper(localName = "GetProductResponse", className = "GetProductResponse")
    @WebMethod(operationName = "GetProduct")
    public void getProduct(
            @WebParam(mode = WebParam.Mode.INOUT, name = "productId")
            javax.xml.ws.Holder<String> productId,
            @WebParam(mode = WebParam.Mode.OUT, name = "name")
            javax.xml.ws.Holder<String> name
    ) throws UnknownProductFault;
}
