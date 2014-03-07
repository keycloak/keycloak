package org.keycloak.models.mongo.keycloak.config;

/**
 * Provides {@link MongoClientProvider} instance
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoClientProviderHolder {

    // Just use static object for now. Default impl is SystemPropsMongoClientProvider
    private static MongoClientProvider instance = new SystemPropertiesMongoClientProvider();

    public static MongoClientProvider getInstance() {
        return instance;
    }

    public static void setInstance(MongoClientProvider instance) {
        MongoClientProviderHolder.instance = instance;
    }
}
