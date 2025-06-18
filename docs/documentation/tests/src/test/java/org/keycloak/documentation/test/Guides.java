package org.keycloak.documentation.test;

import java.util.LinkedList;
import java.util.List;

public class Guides {

    private static String[] guides;
    static {
        boolean product = System.getProperties().containsKey("product");

        List<String> g = new LinkedList<>();
        g.add("authorization_services");
        g.add("release_notes");
        g.add("server_admin");
        g.add("server_development");
        g.add("upgrading");

        guides = g.toArray(new String[g.size()]);
    }

    public static String[] guides() {
        return guides;
    }

}
