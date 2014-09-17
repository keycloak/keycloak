package org.keycloak.util;

import java.util.Date;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Time {

    private static int offset;

    public static int currentTime() {
        return ((int) (System.currentTimeMillis() / 1000)) + offset;
    }

    public static Date toDate(int time) {
        return new Date(((long) time ) * 1000);
    }

    public static void setOffset(int offset) {
        Time.offset = offset;
    }

}
