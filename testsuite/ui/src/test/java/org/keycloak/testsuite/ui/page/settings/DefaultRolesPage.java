/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.page.settings;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.testsuite.ui.fragment.PickList;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.keycloak.testsuite.ui.model.Role;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author pmensik
 */
public class DefaultRolesPage extends AbstractPage {
    
    @FindBy(id = "")
    private PickList realmDefaultRoles;
    
    @FindBy(id = "")
    private PickList applicationDefaultRoles;
    
    @FindBy(id = "applications")
    private Select applicationsSelect;
    
    public void addDefaultRealmRoles(String... roles) {
        realmDefaultRoles.addItems(roles);
    }
    
    public void addDefaultRealmRoles(Role... roles) {
        List<String> roleList = new ArrayList<String>();
        for(Role role : roles) {
            roleList.add(role.getName());
        }
        addDefaultRealmRoles(((String []) roleList.toArray()));
    }
}
