package org.keycloak.performance.dataset.idm.authorization;

import java.util.AbstractList;
import java.util.List;
import static java.util.stream.Collectors.toList;
import org.keycloak.performance.dataset.idm.Client;

/**
 *
 * @author tkyjovsk
 */
public class ResourceServerList extends AbstractList<ResourceServer> {

    List<Client> clients;
    List<ResourceServer> resourceServers;

    public ResourceServerList(List<Client> clients) {
        this.clients = clients;
    }

    public void update() {
        resourceServers = clients.stream()
                .filter(c -> c.getRepresentation().getAuthorizationServicesEnabled())
                .map(c -> c.getResourceServer())
                .collect(toList());
    }

    public void updateIfNull() {
        if (resourceServers == null) {
            update();
        }
    }

    @Override
    public ResourceServer get(int index) {
        updateIfNull();
        return resourceServers.get(index);
    }

    @Override
    public int size() {
        updateIfNull();
        return resourceServers.size();
    }

}
