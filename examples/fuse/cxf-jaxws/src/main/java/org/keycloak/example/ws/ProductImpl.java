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
