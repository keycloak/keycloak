package org.keycloak.testsuite.tomcat7;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TomcatAdapterTest {
    public void startServer() throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        //tomcat.addWebapp()
        File base = new File(System.getProperty("java.io.tmpdir"));
        Context rootCtx = tomcat.addContext("/app", base.getAbsolutePath());
        //Tomcat.addServlet(rootCtx, "dateServlet", new DatePrintServlet());
        rootCtx.addServletMapping("/date", "dateServlet");
        tomcat.start();
        tomcat.getServer().await();
    }}
