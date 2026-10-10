package org.keycloak.misc.vulns;

import org.keycloak.misc.vulns.utils.FindDependency;

import java.io.File;

public class Config {

    public static File VULN_SCRIPT_HOME = new File(System.getenv("VULN_SCRIPT_HOME")).getAbsoluteFile();
    public static String REPOSITORY;
    public static File OUTPUT;
    public static File REPORTS;
    public static String STREAM;

    public static boolean UPDATE_ISSUES = false;

    public static void init(String[] args) {
        if (VULN_SCRIPT_HOME == null || !VULN_SCRIPT_HOME.isDirectory()) {
            throw new RuntimeException("VULN_SCRIPT_HOME environment variable not set, or does not exist");
        }

        File file = new File(FindDependency.lookupFindDependencyScript());
        if (!file.isFile()) {
            throw new RuntimeException(file + " script not found");
        }

        for (String a : args) {
            if (a.equals("--update-issues")) {
                UPDATE_ISSUES = true;
            } else if (a.startsWith("--repository=")) {
                REPOSITORY = a.split("=")[1];
            } else if (a.startsWith("--output")) {
                OUTPUT = new File(a.split("=")[1]).getAbsoluteFile();
            } else if (a.startsWith("--stream")) {
                STREAM = a.split("=")[1];
            } else if (!a.startsWith("--")) {
                REPORTS = new File(a).getAbsoluteFile();
            } else {
                throw new RuntimeException("Unknown option " + a);
            }
        }

        if (REPOSITORY == null && System.getenv().containsKey("GITHUB_REPOSITORY")) {
            REPOSITORY = System.getenv("GITHUB_REPOSITORY");
        }
        if (OUTPUT == null && System.getenv().containsKey("GITHUB_STEP_SUMMARY")) {
            OUTPUT = new File(System.getenv("GITHUB_STEP_SUMMARY")).getAbsoluteFile();
        }

        if (REPORTS == null) {
            throw new RuntimeException("Reports path not set");
        }
        if (!REPORTS.exists()) {
            throw new RuntimeException("Reports path does not exist");
        }
    }

}
