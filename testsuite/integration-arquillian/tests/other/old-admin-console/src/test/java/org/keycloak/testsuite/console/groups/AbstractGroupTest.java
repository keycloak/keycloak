package org.keycloak.testsuite.console.groups;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.groups.CreateGroup;
import org.keycloak.testsuite.console.page.groups.Groups;

import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author clementcur
 */
public abstract class AbstractGroupTest extends AbstractConsoleTest {

    @Page
    protected Groups groupsPage;
    @Page
    protected CreateGroup createGroupPage;

    protected GroupRepresentation newTestRealmGroup;
    
    @Before
    public void beforeGroupTest() {
        newTestRealmGroup = new GroupRepresentation();
    }

    public void createGroup(GroupRepresentation group) {
        assertCurrentUrlEquals(groupsPage);
        groupsPage.table().addGroup();
        assertCurrentUrlStartsWith(createGroupPage);
        createGroupPage.form().setValues(group);
        createGroupPage.form().save();
    }
}
