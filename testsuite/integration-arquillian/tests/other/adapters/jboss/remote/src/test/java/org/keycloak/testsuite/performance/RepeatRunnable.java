package org.keycloak.testsuite.performance;

import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public abstract class RepeatRunnable implements Runnable {

    private long sleepBetweenRepeatsMillis;
    private long repeatCounter;

    public RepeatRunnable() {
        this(0);
        this.repeatCounter = 0;
    }

    public RepeatRunnable(long sleepBetweenRepeatsMillis) {
        this.sleepBetweenRepeatsMillis = sleepBetweenRepeatsMillis;
    }

    public void setSleepBetweenRepeatsMillis(long sleepBetweenRepeatsMillis) {
        this.sleepBetweenRepeatsMillis = sleepBetweenRepeatsMillis;
    }

    public long getRepeatCounter() {
        return repeatCounter;
    }

    @Override
    public void run() {
        while (true) {
            repeat();
            repeatCounter++;
            pause(sleepBetweenRepeatsMillis);
        }
    }

    public abstract void repeat();

}
