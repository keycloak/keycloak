package org.keycloak.testutils;

import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.TimeBasedOTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class TotpGenerator {

    public static void main(String[] args) throws IOException {

        Timer timer = new Timer();
        TotpTask task = null;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Insert secret: ");
        for (String l = br.readLine(); true; l = br.readLine()) {
            if (task != null) {
                task.cancel();
            }

            System.out.println("Secret: " + l);
            task = new TotpTask(l);
            timer.schedule(task, 0, TimeUnit.SECONDS.toMillis(TimeBasedOTP.DEFAULT_INTERVAL_SECONDS));
        }
    }

    private static class TotpTask extends TimerTask {
        private String secret;

        private TotpTask(String secret) {
            this.secret = secret;
        }

        @Override
        public void run() {
            String google = new String(Base32.decode(secret));
            TimeBasedOTP otp = new TimeBasedOTP();
            System.out.println(otp.generate(google));
        }
    }

}
