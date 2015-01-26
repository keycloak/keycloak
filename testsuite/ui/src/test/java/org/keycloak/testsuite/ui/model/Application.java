package org.keycloak.testsuite.ui.model;

/**
 * Created by fkiss.
 */
public class Application {

    private String name;

    private boolean enabled;

    private String accessType;

    private String uri;

    public Application(String name, String uri) {
        this.uri = uri;
        this.enabled = true;
        this.name = name;
    }

    public Application() {
    }

    public Application(String name, String uri, String accessType, boolean enabled) {
        this.name = name;
        this.uri = uri;
        this.accessType = accessType;
        this.enabled = enabled;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAccessType() { return accessType; }

    public void setAccessType(String accessType) { this.accessType = accessType; }

    public String getUri() { return uri; }

    public void setUri(String uri) { this.uri = uri; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Application that = (Application) o;

        if (enabled != that.enabled) return false;
        if (accessType != null ? !accessType.equals(that.accessType) : that.accessType != null) return false;
        if (!name.equals(that.name)) return false;
        if (!uri.equals(that.uri)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (accessType != null ? accessType.hashCode() : 0);
        result = 31 * result + uri.hashCode();
        return result;
    }
}
