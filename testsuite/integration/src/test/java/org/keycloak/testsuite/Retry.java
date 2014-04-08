package org.keycloak.testsuite;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Retry {

    public static void execute(Runnable runnable, int retry, long interval) throws InterruptedException {
        while (true) {
            try {
                runnable.run();
                return;
            } catch (RuntimeException e) {
                retry--;
                if (retry > 0) {
                   Thread.sleep(interval);
                } else {
                    throw e;
                }
            }
        }
    }

}
