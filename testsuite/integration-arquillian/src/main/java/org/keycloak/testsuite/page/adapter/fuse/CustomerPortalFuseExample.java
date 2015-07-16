package org.keycloak.testsuite.page.adapter.fuse;

/**
 *
 * @author tkyjovsk
 */
public class CustomerPortalFuseExample extends AbstractFuseExample {

    public static final String DEPLOYMENT_NAME = "customer-portal-fuse-example";
    public static final String DEPLOYMENT_CONTEXT = "customer-portal";

    @Override
    public String getContext() {
        return DEPLOYMENT_CONTEXT;
    }

}
