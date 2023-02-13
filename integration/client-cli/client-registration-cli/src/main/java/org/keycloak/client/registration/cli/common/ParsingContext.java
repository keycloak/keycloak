package org.keycloak.client.registration.cli.common;


/**
 * An iterator wrapping command line
 *
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ParsingContext {

    private int offset;
    private int pos = -1;
    private String [] args;

    public ParsingContext(String [] args) {
        this(args, 0, -1);
    }

    public ParsingContext(String [] args, int offset) {
        this(args, offset, -1);
    }

    public ParsingContext(String [] args, int offset, int pos) {
        this.args = args.clone();
        this.offset = offset;
        this.pos = pos;
    }

    public boolean hasNext() {
        return pos < args.length-1;
    }


    public boolean hasNext(int count) {
        return pos < args.length - count;
    }

    public boolean hasPrevious() {
        return pos > 0;
    }

    /**
     * Get next argument
     *
     * @return Next argument or null if beyond the end of arguments
     */
    public String next() {
        if (hasNext()) {
            return args[++pos];
        } else {
            pos = args.length;
            return null;
        }
    }

    /**
     * Check that a next argument is available
     *
     * @return Next argument or RuntimeException if next argument is not available
     */
    public String nextRequired() {
        if (!hasNext()) {
            throw new RuntimeException("Option " + current() + " requires a value");
        }
        return next();
    }

    /**
     * Get next n-th argument
     *
     * @return Next n-th argument or null if beyond the end of arguments
     */
    public String next(int n) {
        if (hasNext(n)) {
            pos += n;
            return args[pos];
        } else {
            pos = args.length;
            return null;
        }
    }

    /**
     * Get previous argument
     *
     * @return Previous argument or null if previous call was at the beginning of the arguments (pos == 0)
     */
    public String previous() {
        if (hasPrevious()) {
            return args[--pos];
        } else {
            pos = -1;
            return null;
        }
    }

    /**
     * Get current argument
     *
     * @return Current argument or null if current parsing position is beyond end, or before start
     */
    public String current() {
        if (pos < 0 || pos >= args.length) {
            return null;
        } else {
            return args[pos];
        }
    }

    public String [] getArgs() {
        return args;
    }
}
