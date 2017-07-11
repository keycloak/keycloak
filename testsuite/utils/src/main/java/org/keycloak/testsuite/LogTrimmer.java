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
        boolean testRunning = false;
        StringBuilder sb = new StringBuilder();
        for(String l = br.readLine(); l != null; l = br.readLine()) {
            if (!testRunning) {
                if (l.contains("STARTED")) {
                    testRunning = true;
                } else {
                    System.out.println(l);
                }
            } else {
                if (l.contains("FAILED")) {
                    System.out.println(l);
                    System.out.println("--------- FAILED TEST LOG OUTPUT START ---------");
                    testRunning = false;

                    System.out.println(sb.toString());
                    sb = new StringBuilder();
                    System.out.println("---------- FAILED TEST LOG OUTPUT END ----------");
                } else if (l.contains("FINISHED")) {
                    System.out.println(l);
                    testRunning = false;
                    sb = new StringBuilder();
                } else {
                    sb.append(l);
                }
            }
        }
    }

}
