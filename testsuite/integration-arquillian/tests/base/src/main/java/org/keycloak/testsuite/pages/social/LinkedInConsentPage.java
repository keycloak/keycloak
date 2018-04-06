/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.keycloak.testsuite.pages.social;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.logging.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class LinkedInConsentPage {
    
    @Drone
    protected WebDriver driver;
    protected Logger log = Logger.getLogger(this.getClass());

    @FindBy(name = "action")
    private WebElement allowButton;

    public void allow() {
        allowButton.click();
    }

}
