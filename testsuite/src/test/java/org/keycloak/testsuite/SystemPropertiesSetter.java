package org.keycloak.testsuite;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class SystemPropertiesSetter implements Servlet {

    @Override
    public void destroy() {
        for (Entry<String, String> e : original.entrySet()) {
            System.setProperty(e.getKey(), e.getValue());
            System.out.println("RESET " + e.getKey() + "=" + e.getValue());
        }
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    private Map<String, String> original = new HashMap<String, String>();

    @Override
    public void init(ServletConfig arg0) throws ServletException {
        Enumeration<String> n = arg0.getInitParameterNames();
        while (n.hasMoreElements()) {
            String k = n.nextElement();
            String v = arg0.getInitParameter(k);
            
            original.put(k, v);
            
            System.setProperty(k, v);
            System.out.println("SET " + k + "=" + v);
        }
    }

    @Override
    public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
    }

}
