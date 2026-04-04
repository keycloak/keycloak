package org.keycloak.representations.info;

public class CpuInfoRepresentation {

    protected long processorCount;

    public static CpuInfoRepresentation create() {
        Runtime runtime = Runtime.getRuntime();
        CpuInfoRepresentation rep = new CpuInfoRepresentation();
        rep.setProcessorCount(runtime.availableProcessors());
        return rep;
    }

    public long getProcessorCount() {
        return processorCount;
    }

    public void setProcessorCount(long processorCount) {
        this.processorCount = processorCount;
    }
}
