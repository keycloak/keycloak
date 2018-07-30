package org.keycloak.documentation.test;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

public class Failures extends AssertionError {

    private Set<String> failures;

    public Failures(String error, Set<String> failures) {
        super(error);
        this.failures = failures;
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.out);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        for (String f : failures) {
            s.println("* " + f);
        }
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        for (String f : failures) {
            s.println("* " + f);
        }
    }

}
