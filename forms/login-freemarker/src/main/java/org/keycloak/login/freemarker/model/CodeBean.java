package org.keycloak.login.freemarker.model;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CodeBean {

    private final String code;
    private final String error;

    public CodeBean(String code, String error) {
        this.code = code;
        this.error = error;
    }

    public boolean isSuccess() {
        return code != null && error == null;
    }

    public String getCode() {
        return code;
    }

    public String getError() {
        return error;
    }
}
