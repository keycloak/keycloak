/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.social;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.keycloak.social.util.UriBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderCallback {

	private String application;
	
	private HttpHeaders headers;
	
	private String providerKey;
	
	private String providerSecret;
	
	private IdentityProviderState providerState;

	private UriInfo uriInfo;

	public boolean containsQueryParam(String key) {
		return uriInfo.getQueryParameters().containsKey(key);
	}

	public boolean containsState(String key) {
		return providerState.contains(key);
	}

	public UriBuilder createUri(String path) {
		return new UriBuilder(headers, uriInfo, path);
	}

	public URI getProviderCallbackUrl() {
        return createUri("social/" + application + "/callback").build();
	}

	public String getProviderKey() {
		return providerKey;
	}
	
	public String getProviderSecret() {
		return providerSecret;
	}

	public String getQueryParam(String key) {
		List<String> values = uriInfo.getQueryParameters().get(key);
		if (!values.isEmpty()) {
			return values.get(0);
		}
		return null;
	}

	public <T> T getState(String key) {
		return providerState.remove(key);
	}

	public void putState(String key, Object value) {
		providerState.put(key, value);
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public void setHeaders(HttpHeaders headers) {
		this.headers = headers;
	}

	public void setProviderKey(String providerKey) {
		this.providerKey = providerKey;
	}

	public void setProviderSecret(String providerSecret) {
		this.providerSecret = providerSecret;
	}

	public void setProviderState(IdentityProviderState providerState) {
		this.providerState = providerState;
	}

	public void setUriInfo(UriInfo uriInfo) {
		this.uriInfo = uriInfo;
	}

}
