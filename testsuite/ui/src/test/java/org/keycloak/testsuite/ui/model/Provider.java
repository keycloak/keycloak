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
public class Provider {

	public SocialProvider providerName;
	public String key;
	public String secret;

	public Provider() {
	}

	public Provider(SocialProvider providerName, String key, String secret) {
		this.providerName = providerName;
		this.key = key;
		this.secret = secret;
	}
	
}
