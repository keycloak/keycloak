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

import javax.jws.WebService;
import javax.xml.ws.Holder;

@WebService(serviceName = "ProductService", endpointInterface = "org.keycloak.example.ws.Product")
public class ProductImpl implements Product {

    public void getProduct(Holder<String> productId, Holder<String> name)
        throws UnknownProductFault
    {
        if (productId.value == null || productId.value.length() == 0) {
            org.keycloak.example.ws.types.UnknownProductFault fault = new org.keycloak.example.ws.types.UnknownProductFault();
            fault.setProductId(productId.value);
            throw new UnknownProductFault(null,fault);
        } else if (productId.value.trim().equals("1")) {
            name.value = "IPad";
        } else if (productId.value.trim().equals("2")) {
            name.value = "IPhone";
        } else {
            org.keycloak.example.ws.types.UnknownProductFault fault = new org.keycloak.example.ws.types.UnknownProductFault();
            fault.setProductId(productId.value);
            throw new UnknownProductFault(null,fault);
        }
    }

}
