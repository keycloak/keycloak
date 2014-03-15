package org.keycloak.util;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Time {

    public static int currentTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }

}
