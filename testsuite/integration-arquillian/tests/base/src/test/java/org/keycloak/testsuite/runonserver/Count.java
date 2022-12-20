package org.keycloak.testsuite.runonserver;

public class Count {

    public static int count = 0;

    public synchronized static int getAndIncrease() {
        int c = count;
        count++;
        return c;
    }

}
