package org.keycloak.requestfilter;

import io.netty.handler.ipfilter.IpFilterRuleType;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.Config;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Implements a {@linkplain ContainerRequestFilter} based on given access control lists.
 *
 * @author <a href="mailto:magnus.niemann@bosch.com">Magnus Niemann</a>
 */
public class IpAccessFilter implements ContainerRequestFilter {

    private static final Response RESPONSE_ACCESS_FORBIDDEN = Response.status(Response.Status.FORBIDDEN).build();

    public static final String CFG_ALLOW = "allow";
    public static final String CFG_DENY = "deny";

    private final AccessRules allowAccessRules;

    private final AccessRules denyAccessRules;

    private static final Logger log = Logger.getLogger(IpAccessFilter.class);

    @Context
    private HttpRequest request;

    public IpAccessFilter() {

        Config.Scope scope = Config.scope(RequestFilterSpi.SPI_ID, IpAccessFilterProviderFactory.PROVIDER_ID);

        String contextPath = System.getProperty("quarkus.http.root-path", "");

        this.allowAccessRules = AccessRules.parse(
                scope.get(CFG_ALLOW),
                contextPath, IpFilterRuleType.ACCEPT);

        this.denyAccessRules = AccessRules.parse(
                scope.get(CFG_DENY),
                contextPath, IpFilterRuleType.REJECT);
    }

    protected IpAccessFilter(AccessRules allowAccessRules, AccessRules denyAccessRules) {
        this.allowAccessRules = allowAccessRules;
        this.denyAccessRules = denyAccessRules;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (allowAccessRules == null && denyAccessRules == null) {
            // no configuration, allow access
            return;
        }

        // if we are behind a proxy, use IP address from header
        String remoteIP = request.getHttpHeaders().getHeaderString("x-forwarded-for");
        if (StringUtil.isBlank(remoteIP)) {
            remoteIP = request.getRemoteAddress();
        } else {
            // workaround for some proxies which add a port to the address
            remoteIP = stripPort(remoteIP);
        }

        URI requestUri = requestContext.getUriInfo().getRequestUri();
        log.tracef("Processing request: %s", requestUri);

        if (!accessIsAllowed(remoteIP, requestUri.getPath())) {
            requestContext.abortWith(RESPONSE_ACCESS_FORBIDDEN);
        }
    }

    private static String stripPort(String remoteIP) {
        // strip the port
        int portIndex = remoteIP.indexOf(':');
        if (portIndex > -1) {
            remoteIP = remoteIP.substring(0, portIndex);
        }
        return remoteIP;
    }

    public boolean accessIsAllowed(String remoteIP, String requestPath) {
        InetSocketAddress address = new InetSocketAddress(remoteIP, 80);
        return matchesAllowRule(requestPath, address) || (! matchesDenyRule(requestPath, address));
    }

    private boolean matchesDenyRule(String requestPath, InetSocketAddress address) {
        return (denyAccessRules != null) && (denyAccessRules.matches(address, requestPath));
    }

    private boolean matchesAllowRule(String requestPath, InetSocketAddress address) {
        return (allowAccessRules != null) && (allowAccessRules.matches(address, requestPath));
    }

}