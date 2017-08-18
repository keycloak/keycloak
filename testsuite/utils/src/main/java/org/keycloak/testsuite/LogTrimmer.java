package org.keycloak.testsuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by st on 03/07/17.
 */
public class LogTrimmer {

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String testRunning = null;
        StringBuilder sb = new StringBuilder();
        for(String l = br.readLine(); l != null; l = br.readLine()) {
            if (testRunning == null) {
                if (l.startsWith("Running")) {
                    testRunning = l.split(" ")[1];
                    System.out.println(l);
                } else {
                    System.out.println("-- " + l);
                }
            } else {
                if (l.contains("Tests run:")) {
                    if (!(l.contains("Failures: 0") && l.contains("Errors: 0"))) {
                        System.out.println("--------- " + testRunning + " output start ---------");
                        System.out.println(sb.toString());
                        System.out.println("--------- " + testRunning + " output end  ---------");
                    }
                    System.out.println(l);


                    testRunning = null;
                    sb = new StringBuilder();
                } else {
                    sb.append(testRunning.substring(testRunning.lastIndexOf('.') + 1) + " ++ " + l);
                    sb.append("\n");
                }
            }
        }
    }

}
