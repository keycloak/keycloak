package org.keycloak.testsuite;

import java.util.concurrent.TimeUnit;

/**
 * Simple waiting utility.
 */
public class Waiter {

    /**
     * Condition evaluated while waiting.
     */
    public interface Condition {

        /**
         * @return <code>true</code> to stop waiting.
         */
        boolean check();
    }

    /**
     * Waits for condition.
     *
     * @param condition Wait condition.
     * @param timeout Max timeout for waiting.
     * @param unit Time units for waiting.
     */
    public static void waitForCondition(Condition condition, long timeout, TimeUnit unit) {
        long timeoutCondition = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, unit);
        do {
            if(System.currentTimeMillis() > timeoutCondition) {
                throw new AssertionError("Timeout!");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new AssertionError("Interruption occurred", e);
            }
        } while(!condition.check());
    }

}
