package org.keycloak.guides.maven;

import java.io.File;

public class Context {

    private File srcDir;
    private Options options;
    private String[] serverGuides;

    public Context(File srcDir) {
        this.srcDir = srcDir;
        this.options = new Options();
        this.serverGuides = new File(srcDir, "server").list((dir, f) -> f.endsWith(".adoc") && !f.equals("index.adoc"));
    }

    public String getAnchor(String title) {
        return title.toLowerCase().replace(' ', '_');
    }

    public Options getOptions() {
        return options;
    }

    public String[] getServerGuides() {
        return new File(srcDir, "server").list((dir, f) -> f.endsWith(".adoc") && !f.equals("index.adoc"));
    }

}
