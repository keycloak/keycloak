package org.keycloak.federation.ldap.idm.query.internal;

import java.util.Date;

import org.keycloak.federation.ldap.idm.store.ldap.LDAPUtil;
import org.keycloak.models.LDAPConstants;

/**
 * @author Pedro Igor
 */
public class EqualCondition extends NamedParameterCondition {

    private final Object value;

    public EqualCondition(String name, Object value) {
        super(name);
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    public void applyCondition(StringBuilder filter) {
        Object parameterValue = value;
        if (Date.class.isInstance(value)) {
            parameterValue = LDAPUtil.formatDate((Date) parameterValue);
        }

        filter.append("(").append(getParameterName()).append(LDAPConstants.EQUAL).append(parameterValue).append(")");
    }

    @Override
    public String toString() {
        return "EqualCondition{" +
                "paramName=" + getParameterName() +
                ", value=" + value +
                '}';
    }
}
