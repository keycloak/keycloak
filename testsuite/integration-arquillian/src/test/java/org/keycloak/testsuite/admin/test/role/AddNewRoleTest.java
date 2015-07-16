/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.admin.test.role;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.admin.page.settings.RolesPage;
import org.keycloak.testsuite.admin.model.Role;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.keycloak.testsuite.admin.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.fragment.FlashMessage;
import org.keycloak.testsuite.admin.page.settings.user.UserPage;
import static org.openqa.selenium.By.id;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author Petr Mensik
 */
public class AddNewRoleTest extends AbstractKeycloakTest<RolesPage> {

    @Page
    private UserPage userPage;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeTestAddNewRole() {
        navigation.roles();
    }

    @Test
    public void testAddNewRole() {
        Role role = new Role("role1");
        page.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.roles();
        assertEquals("role1", page.findRole(role.getName()).getName());
        page.deleteRole(role);
    }

    @Ignore
    @Test
    public void testAddNewRoleWithLongName() {
        String name = "hjewr89y1894yh98(*&*&$jhjkashd)*(&y8934h*&@#hjkahsdj";
        page.addRole(new Role(name));
        assertNotNull(page.findRole(name));
        navigation.roles();
        page.deleteRole(name);
    }

    @Test
    public void testAddExistingRole() {
        Role role = new Role("role2");
        page.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.roles();
        page.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        navigation.roles();
        page.deleteRole(role);
    }

    @Test
    public void testRoleIsAvailableForUsers() {
        Role role = new Role("User role");
        page.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        userPage.showAllUsers();
        userPage.goToUser("admin");
        navigation.roleMappings("Admin");
        Select rolesSelect = new Select(driver.findElement(id("available")));
        assertEquals("User role should be present in admin role mapping",
                role.getName(), rolesSelect.getOptions().get(0).getText());
        navigation.roles();
        page.deleteRole(role);
    }

}
