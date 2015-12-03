package org.keycloak.representations.info;

public class MemoryInfoRepresentation {

    protected long total;
    protected String totalFormated;
    protected long used;
    protected String usedFormated;
    protected long free;
    protected long freePercentage;
    protected String freeFormated;

    public static MemoryInfoRepresentation create() {
        MemoryInfoRepresentation rep = new MemoryInfoRepresentation();
        Runtime runtime = Runtime.getRuntime();
        rep.total = runtime.maxMemory();
        rep.totalFormated = formatMemory(rep.total);
        rep.used = runtime.totalMemory() - runtime.freeMemory();
        rep.usedFormated = formatMemory(rep.used);
        rep.free = rep.total - rep.used;
        rep.freeFormated = formatMemory(rep.free);
        rep.freePercentage = rep.free * 100 / rep.total;
        return rep;
    }

    public long getTotal() {
        return total;
    }

    public String getTotalFormated() {
        return totalFormated;
    }

    public long getFree() {
        return free;
    }

    public String getFreeFormated() {
        return freeFormated;
    }

    public long getUsed() {
        return used;
    }

    public String getUsedFormated() {
        return usedFormated;
    }

    public long getFreePercentage() {
        return freePercentage;
    }

    private static String formatMemory(long bytes) {
        if (bytes > 1024L * 1024L) {
            return bytes / (1024L * 1024L) + " MB";
        } else if (bytes > 1024L) {
            return bytes / (1024L) + " kB";
        } else {
            return bytes + " B";
        }
    }

}
