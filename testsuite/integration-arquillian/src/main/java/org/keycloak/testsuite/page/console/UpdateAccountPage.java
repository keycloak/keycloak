/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.page.console;

import org.keycloak.testsuite.model.User;
import org.keycloak.testsuite.page.console.AdminConsole;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class UpdateAccountPage extends AdminConsole {
	
	@FindBy(id = "email")
	private WebElement email;
	
	@FindBy(id = "firstName")
	private WebElement firstName;
	
	@FindBy(id = "lastName")
	private WebElement lastName;
	
	public void updateAccountInfo(User user) {
		email.clear();
		email.sendKeys(user.getEmail());
		firstName.clear();
		firstName.sendKeys(user.getFirstName());
		lastName.clear();
		lastName.sendKeys(user.getLastName());
		primaryButton.click();
	}
	
	public void updateAccountInfo(String email, String firstName, String lastName) {
		User u = new User("", "", email, firstName, lastName);
		updateAccountInfo(u);
	}

}
