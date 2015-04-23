/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.demo.customerPortal;

import java.io.File;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.keycloak.testsuite.ui.AbstractTest;
import org.keycloak.testsuite.ui.fragment.CreateRealm;
import org.openqa.selenium.By;

/**
 *
 * @author pmensik
 */
public abstract class CustomerPortalAbstractTest extends AbstractTest {
	
	protected static final String JBOSS_HOME = System.getProperty("jbossHome");
	
	protected static final String DEMO_APP = "demo";
	protected static final String APP_BASE_PATH = JBOSS_HOME + "../examples/preconfigured-demo/customer-app/";
	
	protected By customerListingLoc = By.linkText("Customer Listing");
	protected By customerAdminLoc = By.linkText("Customer Admin Interface");
	protected By customerSessionLoc = By.linkText("Customer Session");
	
	private static boolean realmSettingsUploaded = false;
	
	@ArquillianResource
	private URL deploymentUrl;
	
	@Page
	private CreateRealm createRealm;
	
	@Before
	public void before() {
		if(!realmSettingsUploaded) {
			driver.get(getKeycloakConsoleUrl());
			loginPage.loginAsAdmin();
			navigation.addRealm();
			createRealm.importRealm(APP_BASE_PATH + "../testrealm.json");
			realmSettingsUploaded = true;
			logOut();
		}
		driver.get(getUrl());
	}
	
	@Deployment(name = DEMO_APP, testable = false)
	public static WebArchive createDemoDeployment() {
		return ShrinkWrap.createFromZipFile(WebArchive.class, 
				new File(APP_BASE_PATH + "target/customer-portal.war"));
	}
	
	protected void logOutFromDemo() {
		driver.findElement(By.linkText("logout")).click();
	}
	
	protected String getUrl() {
		return deploymentUrl.toExternalForm();
	}
	
	protected String getKeycloakConsoleUrl() {
		return deploymentUrl.getProtocol() + "://" + deploymentUrl.getHost() +
				":" + deploymentUrl.getPort() + "/auth/admin";
	}
	
	
}
