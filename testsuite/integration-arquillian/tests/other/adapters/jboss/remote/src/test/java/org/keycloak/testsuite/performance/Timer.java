package org.keycloak.testsuite.performance;

import java.util.Date;

/**
 *
 * @author tkyjovsk
 */
public final class Timer {
 
    private long time;

    public Timer() {
        reset();
    }

    public long reset() {
        time = new Date().getTime();
        return time;
    }

    public long getElapsedTime() {
        return new Date().getTime() - time;
    }
    
}
