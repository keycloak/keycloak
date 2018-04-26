package org.keycloak.performance.templates;

import java.io.File;
import java.util.List;
import org.apache.commons.configuration.CombinedConfiguration;
import org.keycloak.performance.templates.idm.RealmTemplate;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.Validate;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.dataset.DatasetRepresentation;
import org.keycloak.performance.dataset.idm.Client;
import org.keycloak.performance.dataset.idm.Realm;
import org.keycloak.performance.dataset.idm.User;
import org.keycloak.performance.iteration.Flattened2DList;
import org.keycloak.performance.util.CombinedConfigurationNoInterpolation;
import static org.keycloak.performance.util.ConfigurationUtil.loadFromFile;

/**
 *
 * @author tkyjovsk
 */
public class DatasetTemplate extends EntityTemplate<Dataset, DatasetRepresentation> {

    protected final RealmTemplate realmTemplate;

    public DatasetTemplate(Configuration configuration) {
        super(configuration);
        this.realmTemplate = new RealmTemplate(this);
    }

    public DatasetTemplate() {
        this(loadConfiguration());
    }

    protected static Configuration loadConfiguration() {
        try {
            CombinedConfiguration configuration = new CombinedConfigurationNoInterpolation();
            String datasetPropertiesFile = System.getProperty("dataset.properties.file");
            Validate.notEmpty(datasetPropertiesFile);
            configuration.addConfiguration(loadFromFile(new File(datasetPropertiesFile)));
            return configuration;
        } catch (ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Dataset newEntity() {
        return new Dataset();
    }

    @Override
    public void processMappings(Dataset dataset) {
        dataset.setRealms(new NestedEntityTemplateWrapperList<>(dataset, realmTemplate));
        dataset.setAllUsers(new Flattened2DList<Realm, User>() {
            @Override
            public List<Realm> getXList() {
                return dataset.getRealms();
            }

            @Override
            public List<User> getYList(Realm realm) {
                return realm.getUsers();
            }

            @Override
            public int getYListSize() {
                return realmTemplate.userTemplate.usersPerRealm;
            }
        });
        dataset.setAllClients(new Flattened2DList<Realm, Client>() {
            @Override
            public List<Realm> getXList() {
                return dataset.getRealms();
            }

            @Override
            public List<Client> getYList(Realm realm) {
                return realm.getClients();
            }

            @Override
            public int getYListSize() {
                return realmTemplate.clientTemplate.clientsPerRealm;
            }
        });
    }

    @Override
    public void validateConfiguration() {
        realmTemplate.validateConfiguration();
        logger().info("");
    }

}
