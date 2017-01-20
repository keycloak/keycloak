package org.keycloak.testsuite.performance;

import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public abstract class LoopingRunnable implements Runnable {

    private long sleepBetweenLoopsMillis;
    private long loopCounter;

    public LoopingRunnable() {
        this(0);
        this.loopCounter = 0;
    }

    public LoopingRunnable(long sleepBetweenLoopsMillis) {
        this.sleepBetweenLoopsMillis = sleepBetweenLoopsMillis;
    }

    public void setSleepBetweenLoopsMillis(long sleepBetweenLoopsMillis) {
        this.sleepBetweenLoopsMillis = sleepBetweenLoopsMillis;
    }

    public long getLoopCounter() {
        return loopCounter;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            loop();
            loopCounter++;
            pause(sleepBetweenLoopsMillis);
        }
    }

    public abstract void loop();

}
