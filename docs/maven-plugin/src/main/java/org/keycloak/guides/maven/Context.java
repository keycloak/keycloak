package org.keycloak.guides.maven;

import java.io.File;

public class Context {

    private File srcDir;
    private Options options;
    private Features features;
    private String[] serverGuides;

    public Context(File srcDir) {
        this.srcDir = srcDir;
        this.options = new Options();
        this.features = new Features(options);
        this.serverGuides = new File(srcDir, "server").list((dir, f) -> f.endsWith(".adoc") && !f.equals("index.adoc"));
    }

    public Options getOptions() {
        return options;
    }

    public Features getFeatures() {
        return features;
    }

    public String[] getServerGuides() {
        return new File(srcDir, "server").list((dir, f) -> f.endsWith(".adoc") && !f.equals("index.adoc"));
    }

}
