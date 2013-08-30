package org.keycloak.services.models.nosql.api.types;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class ConverterKey {

    private final Class<?> applicationObjectType;
    private final Class<?> dbObjectType;

    public ConverterKey(Class<?> applicationObjectType, Class<?> dbObjectType) {
        this.applicationObjectType = applicationObjectType;
        this.dbObjectType = dbObjectType;
    }

    @Override
    public int hashCode() {
        return applicationObjectType.hashCode() * 13 + dbObjectType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }

        ConverterKey tc = (ConverterKey)obj;
        return tc.applicationObjectType.equals(this.applicationObjectType) && tc.dbObjectType.equals(this.dbObjectType);
    }
}
