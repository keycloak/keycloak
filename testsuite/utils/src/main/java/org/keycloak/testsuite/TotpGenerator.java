/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.TimeBasedOTP;


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
            System.out.println(otp.generateTOTP(google));
        }
    }

}
