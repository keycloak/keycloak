package org.keycloak.forms.login.freemarker.model;

public class ScriptBean {

    private final String url;
    private final boolean async;
    private final boolean defer;

    public ScriptBean(String url, boolean async, boolean defer) {
        this.url = url;
        this.async = async;
        this.defer = defer;
    }

    public String getUrl() {
        return url;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isDefer() {
        return defer;
    }

    /*
     * This is a workaround: When an old theme template copies the mechanism of loading these scripts:
     * <script src="${script}" type="text/javascript"></script>
     * They assume the "script" is the URL. By returning URL in toString, Freemarker will call toString on the object and
     * return the url. This way, we don't break backwards compatibility
     */
    @Override
    public String toString() {
        return url;
    }
}
