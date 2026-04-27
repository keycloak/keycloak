package org.keycloak.testsuite.cli.exec;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class ExecutionException extends RuntimeException {

    private int exitCode = -1;

    public ExecutionException(int exitCode) {
        this.exitCode = exitCode;
    }

    public ExecutionException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }

    @Override
    public String toString() {
        return super.toString() + ", exitCode: " + exitCode;
    }
}
