package org.keycloak.testsuite.util;

import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

/**
 * A simple wrapper for the HTTP Client Engine to follow redirects.
 *
 * <p>
 *     When hitting a Servlet deployed on Jetty without trailing slash, we get a <code>302</code> in return.
 *     Our testsuite doesn't work well with this. This engine solves this problem.
 * </p>
 */
public class FollowRedirectsEngine extends ApacheHttpClient43Engine {
    public FollowRedirectsEngine() {
        this.followRedirects = true;
    }
}
