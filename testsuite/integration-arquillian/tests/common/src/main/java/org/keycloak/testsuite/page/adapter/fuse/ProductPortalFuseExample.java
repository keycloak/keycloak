package org.keycloak.testsuite.page.adapter.fuse;

/**
 *
 * @author tkyjovsk
 */
public class ProductPortalFuseExample extends AbstractFuseExample {

    public static final String DEPLOYMENT_NAME = "product-portal-fuse-example";
    public static final String DEPLOYMENT_CONTEXT = "product-portal";

    @Override
    public String getContext() {
        return DEPLOYMENT_CONTEXT;
    }

}
