/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.springsecurity.authentication;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * To return the forbidden code with the corresponding message.
 * 
 * @author emilienbondu
 *
 */
public class KeycloakAuthenticationFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
		// Check that the response was not committed yet (this may happen when another
		// part of the Keycloak adapter sends a challenge or a redirect).
		if (!response.isCommitted()) {
			if (KeycloakCookieBasedRedirect.getRedirectUrlFromCookie(request) != null) {
				response.addCookie(KeycloakCookieBasedRedirect.createCookieFromRedirectUrl(null));
			}
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unable to authenticate using the Authorization header");
		} else {
			if (200 <= response.getStatus() && response.getStatus() < 300) {
				throw new RuntimeException("Success response was committed while authentication failed!", exception);
			}
		}
	}
}
