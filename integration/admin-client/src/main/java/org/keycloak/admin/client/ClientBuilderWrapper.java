package org.keycloak.admin.client;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;

public class ClientBuilderWrapper {

    static Class clazz;
    static {
        try {
            clazz = Class.forName("org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl");
        } catch (ClassNotFoundException e) {
            try {
                clazz = Class.forName("org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder");
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("RestEasy 3 or 4 not found on classpath");
            }
        }
    }

    public static ClientBuilder create(SSLContext sslContext, boolean disableTrustManager) {
        try {
            Object o = clazz.newInstance();
            clazz.getMethod("sslContext", SSLContext.class).invoke(o, sslContext);
            clazz.getMethod("connectionPoolSize", int.class).invoke(o, 10);
            if (disableTrustManager) {
                clazz.getMethod("disableTrustManager").invoke(o);
            }
            return (ClientBuilder) o;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
