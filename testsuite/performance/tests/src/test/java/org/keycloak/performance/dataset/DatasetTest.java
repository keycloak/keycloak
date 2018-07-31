package org.keycloak.performance.dataset;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.performance.dataset.idm.Credential;
import org.keycloak.performance.dataset.idm.authorization.Resource;
import org.keycloak.performance.dataset.idm.authorization.ResourcePermission;
import org.keycloak.performance.templates.DatasetTemplate;
import org.keycloak.performance.util.Loggable;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.util.JsonSerialization.writeValueAsString;

/**
 *
 * @author tkyjovsk
 */
public class DatasetTest extends EntityTest<Dataset> implements Loggable {

    DatasetTemplate dt = new DatasetTemplate();
    Dataset dataset = dt.produce();

    @Test
    @Ignore
    public void toJSON() throws IOException {
        logger().info("REALM JSON: \n" + dataset.getRealms().get(0).toJSON());
        logger().info("CLIENT JSON: \n" + dataset.getRealms().get(0).getClients().get(0).toJSON());
        logger().info("USER JSON: \n" + dataset.getRealms().get(0).getUsers().get(0).toJSON());
        logger().info("CREDENTIAL JSON: \n" + dataset.getRealms().get(0).getUsers().get(0).getCredentials().get(0).toJSON());
        logger().info("REALM ROLE MAPPINGS: \n" + dataset.getRealms().get(0).getUsers().get(0).getRealmRoleMappings().toJSON());

        if (dataset.getRealms().get(0).getResourceServers().isEmpty()) {
        } else {
            logger().info("RESOURCE SERVER: \n" + dataset.getRealms().get(0).getResourceServers().get(0).toJSON());
            logger().info("RESOURCE: \n" + dataset.getRealms().get(0).getResourceServers().get(0).getResources().get(0).toJSON());
        }

    }

    @Test
    @Ignore
    public void pojoToMap() throws IOException {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm("realm_0");
        realm.setEnabled(true);

        logger().info("REP JSON:");
        logger().info(writeValueAsString(realm));

        TypeReference typeRef = new TypeReference<Map<String, Object>>() {
        };
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        Map<String, Object> map = mapper.convertValue(realm, typeRef);
        map.put("index", 1000);

        logger().info("MAP:");
        logger().info(map);

        logger().info("MAP JSON:");
        logger().info(writeValueAsString(map));

    }

    @Test
    @Ignore
    public void testStreams() throws IOException {

        dataset.realms().forEach(r -> logger().info(r.toString()));
        dataset.realmRoles().forEach(rr -> logger().info(rr.toString()));
        dataset.clients().forEach(c -> logger().info(c.toString()));
        dataset.clientRoles().forEach(cr -> logger().info(cr.toString()));
        dataset.users().forEach(u -> logger().info(u.toString()));
        for (Credential c : dataset.credentials().collect(toList())) {
            logger().info(c.toJSON());
        }

        dataset.resourceServers().forEach(rs -> logger().info(rs.toString()));
        dataset.resources().forEach(r -> logger().info(r.toString()));
        for (Resource r : dataset.resources().collect(toList())) {
            logger().info(r.toJSON());
        }
        for (ResourcePermission rp : dataset.resourcePermissions().collect(toList())) {
            logger().info(rp.toString());
            logger().info(rp.toJSON());
        }

    }

    @Override
    public void testHashCode() {
        String d1sn = getD1().getClass().getSimpleName();
        String d2sn = getD2().getClass().getSimpleName();
        logger().info(String.format("'%s' - '%s'    '%s' - '%s'", d1sn, d2sn, d1sn.hashCode(), d2sn.hashCode()));
        super.testHashCode();
    }

    @Override
    public Stream<Dataset> entityStream(Dataset dataset) {
        return Stream.of(dataset);
    }

}
