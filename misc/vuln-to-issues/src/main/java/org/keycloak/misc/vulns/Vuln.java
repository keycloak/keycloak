package org.keycloak.misc.vulns;

import java.util.Objects;
import java.util.Set;

public final class Vuln {
    private final String id;
    private Severity severity;
    private final String title;
    private final String packageName;
    private final String packageManager;
    private final Set<String> installedVersions;
    private final Set<String> fixedVersions;
    private final Set<String> targets;
    private final Set<String> affectedStreams;
    private final Set<Tool> tools;

    public Vuln(
            String id,
            Severity severity,
            String title,
            String packageName,
            String packageManager,
            Set<String> installedVersions,
            Set<String> fixedVersions,
            Set<String> targets,
            Set<String> affectedStreams,
            Set<Tool> tools
    ) {
        this.id = id;
        this.severity = severity;
        this.title = title;
        this.packageName = packageName;
        this.packageManager = packageManager;
        this.installedVersions = installedVersions;
        this.fixedVersions = fixedVersions;
        this.targets = targets;
        this.affectedStreams = affectedStreams;
        this.tools = tools;
    }

    public String id() {
        return id;
    }

    public Severity severity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String title() {
        return title;
    }

    public String packageName() {
        return packageName;
    }

    public String packageManager() {
        return packageManager;
    }

    public Set<String> installedVersions() {
        return installedVersions;
    }

    public Set<String> fixedVersions() {
        return fixedVersions;
    }

    public Set<String> targets() {
        return targets;
    }

    public Set<String> affectedStreams() {
        return affectedStreams;
    }

    public Set<Tool> tools() {
        return tools;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Vuln) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.severity, that.severity) &&
                Objects.equals(this.title, that.title) &&
                Objects.equals(this.packageName, that.packageName) &&
                Objects.equals(this.packageManager, that.packageManager) &&
                Objects.equals(this.installedVersions, that.installedVersions) &&
                Objects.equals(this.fixedVersions, that.fixedVersions) &&
                Objects.equals(this.targets, that.targets) &&
                Objects.equals(this.affectedStreams, that.affectedStreams) &&
                Objects.equals(this.tools, that.tools);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, severity, title, packageName, packageManager, installedVersions, fixedVersions, targets, affectedStreams, tools);
    }

    @Override
    public String toString() {
        return "Vuln[" +
                "cveId=" + id + ", " +
                "severity=" + severity + ", " +
                "title=" + title + ", " +
                "packageName=" + packageName + ", " +
                "packageManager=" + packageManager + ", " +
                "installedVersions=" + installedVersions + ", " +
                "fixedVersions=" + fixedVersions + ", " +
                "targets=" + targets + ", " +
                "affectedStreams=" + affectedStreams + ", " +
                "tools=" + tools + ']';
    }

}
