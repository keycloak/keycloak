package org.keycloak.services.resources.admin.info;

public class MemoryInfoRepresentation {

    protected long total;
    protected long used;

    public static MemoryInfoRepresentation create() {
        MemoryInfoRepresentation rep = new MemoryInfoRepresentation();
        Runtime runtime = Runtime.getRuntime();
        rep.total = runtime.maxMemory();
        rep.used = runtime.totalMemory() - runtime.freeMemory();
        return rep;
    }

    public long getTotal() {
        return total;
    }

    public String getTotalFormated() {
        return formatMemory(getTotal());
    }

    public long getFree() {
        return getTotal() - getUsed();
    }

    public String getFreeFormated() {
        return formatMemory(getFree());
    }

    public long getUsed() {
        return used;
    }

    public String getUsedFormated() {
        return formatMemory(getUsed());
    }

    public long getFreePercentage() {
        return getFree() * 100 / getTotal();
    }

    private String formatMemory(long bytes) {
        if (bytes > 1024L * 1024L) {
            return bytes / (1024L * 1024L) + " MB";
        } else if (bytes > 1024L) {
            return bytes / (1024L) + " kB";
        } else {
            return bytes + " B";
        }
    }

}
