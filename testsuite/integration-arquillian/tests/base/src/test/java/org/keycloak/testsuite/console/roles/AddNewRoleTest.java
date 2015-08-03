/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.console.roles;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.console.page.roles.RealmRoles;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.keycloak.representations.idm.RoleRepresentation;
import static org.openqa.selenium.By.id;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author Petr Mensik
 */
public class AddNewRoleTest extends AbstractRolesTest {

    @Page
    private RealmRoles roles;

    @Before
    public void beforeTestAddNewRole() {
        roles.navigateTo();
    }

    @Test
    public void testAddNewRole() {
        RoleRepresentation role = new RoleRepresentation("role1", "");
        roles.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        roles.navigateTo();
        assertEquals("role1", roles.findRole(role.getName()).getName());
        roles.deleteRole(role);
    }

    @Ignore
    @Test
    public void testAddNewRoleWithLongName() {
        String name = "hjewr89y1894yh98(*&*&$jhjkashd)*(&y8934h*&@#hjkahsdj";
        roles.addRole(new RoleRepresentation(name, ""));
        assertNotNull(roles.findRole(name));
        roles.navigateTo();
        roles.deleteRole(name);
    }

    @Test
    public void testAddExistingRole() {
        RoleRepresentation role = new RoleRepresentation("role2", "");
        roles.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        roles.navigateTo();
        roles.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        roles.navigateTo();
        roles.deleteRole(role);
    }

    @Test
    public void testRoleIsAvailableForUsers() {
        RoleRepresentation role = new RoleRepresentation("User role", "");
        roles.addRole(role);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        users.navigateTo();
        users.viewAllUsers();
        users.clickUser("admin");
        user.tabs().roleMappings();
        Select rolesSelect = new Select(driver.findElement(id("available")));
        assertEquals("User role should be present in admin role mapping",
                role.getName(), rolesSelect.getOptions().get(0).getText());
        roles.navigateTo();
        roles.deleteRole(role);
    }

}
