package org.keycloak.admin.client.http.methods;

import org.apache.http.client.methods.HttpPost;

import java.net.URI;

/**
 * This class was created because the Apache HTTP Client
 * does not have a DELETE method that supports a body
 *
 * @author rodrigo.sasaki@icarros.com.br
 */
class HttpDeleteWithBody extends HttpPost {

    public static final String METHOD_NAME = "DELETE";

    public String getMethod() {
        return METHOD_NAME;
    }

    public HttpDeleteWithBody(final String uri) {
        super();
        setURI(URI.create(uri));
    }

}

