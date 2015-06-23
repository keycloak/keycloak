/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.arquillian.jira;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class IssueCache {

	private static Map<String, Boolean> cache = new HashMap<String, Boolean>();

	public static boolean containsIssue(String name) {
		return cache.containsKey(name);
	}

	public static void put(String name, boolean execute) {
		cache.put(name, execute);
	}

	public static boolean shouldExecute(String name) {
		return cache.get(name);
	}
}
