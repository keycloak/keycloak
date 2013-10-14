package org.keycloak.testutils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.picketlink.common.util.Base32;
import org.picketlink.idm.credential.util.TimeBasedOTP;

public class TotpGenerator {

    public static void main(String[] args) {
        String totp = "";
        for (String a : args) {
            totp += a.trim();
        }
        totp = totp.replace(" ", "");

        final String google = new String(Base32.decode(totp));
        final TimeBasedOTP otp = new TimeBasedOTP();

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println(otp.generate(google));
            }
        }, 0, TimeUnit.SECONDS.toMillis(TimeBasedOTP.DEFAULT_INTERVAL_SECONDS));
    }

}
