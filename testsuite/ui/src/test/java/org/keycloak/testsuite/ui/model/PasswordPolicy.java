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
public enum PasswordPolicy {

	HASH_ITERATIONS("Hash Iterations"), LENGTH("Length"), DIGITS("Digits"), LOWER_CASE("Lower Case"), 
	UPPER_CASE("Upper Case"), SPECIAL_CHARS("Special Chars");
	
	private String name;

	private PasswordPolicy(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
