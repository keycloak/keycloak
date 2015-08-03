package org.keycloak.testsuite.console.page.users;

import org.jboss.arquillian.graphene.page.Page;

/**
 *
 * @author tkyjovsk
 */
public class UserAttributes extends User {

    @Page
    private UserAttributesForm form;

    public UserAttributesForm form() {
        return form;
    }

    public UserAttributes() {
        setUserId(form.getId());
    }

}
