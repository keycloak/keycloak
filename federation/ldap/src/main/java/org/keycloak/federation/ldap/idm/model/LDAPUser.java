package org.keycloak.federation.ldap.idm.model;


import org.keycloak.federation.ldap.idm.query.QueryParameter;

/**
 * This class represents a User; a human agent that may authenticate with the application
 *
 * @author Shane Bryzak
 */
public class LDAPUser extends AbstractIdentityType {

    private static final long serialVersionUID = 4117586097100398485L;

    public static final QueryParameter LOGIN_NAME = AttributedType.QUERY_ATTRIBUTE.byName("loginName");

    /**
     * A query parameter used to set the firstName value.
     */
    public static final QueryParameter FIRST_NAME = QUERY_ATTRIBUTE.byName("firstName");

    /**
     * A query parameter used to set the lastName value.
     */
    public static final QueryParameter LAST_NAME = QUERY_ATTRIBUTE.byName("lastName");

    /**
     * A query parameter used to set the email value.
     */
    public static final QueryParameter EMAIL = QUERY_ATTRIBUTE.byName("email");

    @AttributeProperty
    private String loginName;

    @AttributeProperty
    private String firstName;

    @AttributeProperty
    private String lastName;

    @AttributeProperty
    private String email;

    public LDAPUser() {

    }

    public LDAPUser(String loginName) {
        this.loginName = loginName;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}

