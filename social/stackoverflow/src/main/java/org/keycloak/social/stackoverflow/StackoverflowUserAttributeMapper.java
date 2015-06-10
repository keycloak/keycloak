/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.keycloak.social.stackoverflow;

import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;

/**
 * User attribute mapper.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class StackoverflowUserAttributeMapper extends AbstractJsonUserAttributeMapper {

	private static final String[] cp = new String[] { StackoverflowIdentityProviderFactory.PROVIDER_ID };

	@Override
	public String[] getCompatibleProviders() {
		return cp;
	}

	@Override
	public String getId() {
		return "stackoverflow-user-attribute-mapper";
	}

}
