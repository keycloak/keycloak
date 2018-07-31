package org.keycloak.gatling.log;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
class LogLine {

    private final String rawLine;
    private Type type;
    private String simulationClass;
    private String simulationId;
    private String description;
    private String scenario;
    private String userId;
    private String request;
    private long start = -1;
    private long end = -1;
    private long time2 = -1;
    private long time3 = -1;
    private boolean ok;
    private String status;

    LogLine(String line) {
        rawLine = line;
    }

    String rawLine() {
        return rawLine;
    }

    Type type() {
        return type != null ? type : parse().type;
    }

    String simulationClass() {
        return simulationClass;
    }

    public String simulationId() {
        return simulationId;
    }

    public String description() {
        return description;
    }

    long startTime() {
        return type != null ? start : parse().start;
    }

    long endTime() {
        return type != null ? end : parse().end;
    }

    String scenario() {
        return type != null ? scenario : parse().scenario;
    }

    String userId() {
        return type != null ? userId : parse().userId;
    }

    String request() {
        return type != null ? request : parse().request;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    long logTime() {
        if (type == null) {
            parse();
        }
        return type == Type.RUN || type == Type.USER_START ? start : end;
    }

    boolean ok() {
        if (type == null) {
            parse();
        }
        return type != null ? ok : parse().ok;
    }

    LogLine parse() {
        String[] cols = rawLine.split("\\t");

        if ("ASSERTION".equals(cols[0])) {
            type = Type.ASSERTION;
        } else if ("RUN".equals(cols[2])) {
            type = Type.RUN;
            simulationClass = cols[0];
            simulationId = cols[1];
            start = Long.parseLong(cols[3]);
            description = cols[4];
        } else if ("REQUEST".equals(cols[2])) {
            type = Type.REQUEST;
            scenario = cols[0];
            userId = cols[1];
            request = cols[4];
            start = Long.parseLong(cols[5]);
            time2 = Long.parseLong(cols[6]);
            time3 = Long.parseLong(cols[7]);
            end = Long.parseLong(cols[8]);
            ok = "OK".equals(cols[9]);
            if (cols.length > 10) {
                StringBuilder sb = new StringBuilder();
                for (int i = 10; i < cols.length; i++) {
                    if (sb.length() > 0) {
                        sb.append("\t");
                    }
                    sb.append(cols[i]);
                }
                status = sb.toString();
            }
        } else if ("USER".equals(cols[2])) {
            if ("START".equals(cols[3])) {
                type = Type.USER_START;
            } else if ("END".equals(cols[3])) {
                type = Type.USER_END;
            } else {
                throw new RuntimeException("Unknown log entry type: USER " + cols[3]);
            }
            scenario = cols[0];
            userId = cols[1];
            start = Long.parseLong(cols[4]);
            end = Long.parseLong(cols[5]);
        } else {
            throw new RuntimeException("Unknow log entry type: " + cols[2]);
        }

        return this;
    }

    /**
     * @return Reconstructed LogLine
     */
    public String compose() {
        switch (type()) {
            case ASSERTION: {
                return rawLine;
            }
            case RUN: {
                return simulationClass + "\t" + simulationId + "\t" + type.caption() + "\t" + start + "\t"+ description +"\t2.0\t";
            }
            case REQUEST: {
                return scenario + "\t" + userId + "\t" + type.caption() + "\t" + request + "\t" + start + "\t" +
                        time2 + "\t" + time3 + "\t" + end + "\t" + (ok ? "OK" : "KO") + "\t" + (status != null ? status : "");
            }
            case USER_START: {
                return scenario + "\t" + userId + "\t" + type.caption() + "\t" + start + "\t0";

            }
            case USER_END: {
                return scenario + "\t" + userId + "\t" + type.caption() + "\t" + start + "\t" + end;

            }
        }
        throw new IllegalStateException("Unhandled type: " + type());
    }


    enum Type {
        ASSERTION("ASSERTION"),
        RUN("RUN"),
        REQUEST("REQUEST\t"),
        USER_START("USER\tSTART"),
        USER_END("USER\tEND");

        private String caption;

        Type(String caption) {
            this.caption = caption;
        }

        public String caption() {
            return caption;
        }
    }
}
