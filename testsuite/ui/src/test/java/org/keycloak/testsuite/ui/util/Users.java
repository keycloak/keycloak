/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.util;

import org.keycloak.testsuite.ui.model.User;

/**
 *
 * @author pmensik
 */
public final class Users {

	private Users() {
	}
	
	public static final User ADMIN = new User("admin", "admin");
	public static final User EMPTY_USER = new User();
	public static final User TEST_USER1 = new User("user", "password", "user@redhat.com", "user", "test");
	
}
