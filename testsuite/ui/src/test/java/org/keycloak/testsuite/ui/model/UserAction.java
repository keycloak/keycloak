/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.model;

/**
 *
 * @author pmensik
 */
public enum UserAction {

	UPDATE_PASSWORD("Update Password"), VERIFY_EMAIL("Verify Email"), UPDATE_PROFILE("Update Profile"), CONFIGURE_TOTP("Configure Totp");
	
	private final String actionName;

	private UserAction(String actionName) {
		this.actionName = actionName;
	}

	public String getActionName() {
		return actionName;
	}
	
	
}
