import org.keycloak.it.TestProvider;
import org.keycloak.my.custom.providers.MyCustomRealmResourceProvider;
import org.keycloak.my.custom.providers.MyCustomRealmResourceProviderFactory;

import java.util.Collections;
import java.util.Map;

public class CustomTestProvider implements TestProvider {
    @Override
    public String getName() {
        return "my-custom-provider";
    }

    @Override
    public Class[] getClasses() {
        return new Class[] { MyCustomRealmResourceProvider.class, MyCustomRealmResourceProviderFactory.class } ;
    }

    @Override
    public Map<String, String> getManifestResources() {
        return Collections.singletonMap("org.keycloak.services.resource.RealmResourceProviderFactory", "services/org.keycloak.services.resource.RealmResourceProviderFactory");
    }
}
