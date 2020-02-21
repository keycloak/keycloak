package org.keycloak.admin.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

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

    public static ResteasyClientBuilder create() {
        try {
            return (ResteasyClientBuilder) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
