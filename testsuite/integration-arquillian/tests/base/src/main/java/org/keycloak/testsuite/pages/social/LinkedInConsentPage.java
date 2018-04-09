/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.keycloak.testsuite.pages.social;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class LinkedInConsentPage extends AbstractSocialConsentPage {
    
    @FindBy(name = "action")
    private WebElement authorizeButton;

    @Override
    public void authorize() {
        authorizeButton.click();
    }

}
