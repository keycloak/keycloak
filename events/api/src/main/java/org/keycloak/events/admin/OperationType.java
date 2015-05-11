package org.keycloak.events.admin;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum OperationType {

    VIEW(false),
    CREATE(true),
    UPDATE(true),
    DELETE(true),
    ACTION(false);

    private boolean saveByDefault;

    OperationType(boolean saveByDefault) {
        this.saveByDefault = saveByDefault;
    }

    public boolean isSaveByDefault() {
        return saveByDefault;
    }

}
