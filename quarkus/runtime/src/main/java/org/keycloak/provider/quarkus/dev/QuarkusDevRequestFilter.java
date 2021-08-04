package org.keycloak.provider.quarkus.dev;

import io.vertx.ext.web.RoutingContext;
import org.keycloak.provider.quarkus.QuarkusRequestFilter;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class QuarkusDevRequestFilter extends QuarkusRequestFilter {

    @Override
    public void handle(RoutingContext context) {
        if (context.request().uri().startsWith("/q/")) {
            // do not go through Keycloak request filter if serving Quarkus resources such as dev console
            context.next();
            return;
        }
        super.handle(context);
    }
}
