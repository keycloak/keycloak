/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.map.storage.ldap.role;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StreamUtils;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.role.MapRoleEntity;

import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.MapModelCriteriaBuilderAssumingEqualForField;
import org.keycloak.models.map.storage.ldap.role.entity.LdapMapRoleEntityFieldDelegate;
import org.keycloak.models.map.storage.ldap.store.LdapMapIdentityStore;
import org.keycloak.models.map.storage.ldap.config.LdapMapConfig;
import org.keycloak.models.map.storage.ldap.LdapMapKeycloakTransaction;
import org.keycloak.models.map.storage.ldap.model.LdapMapDn;
import org.keycloak.models.map.storage.ldap.model.LdapMapObject;
import org.keycloak.models.map.storage.ldap.model.LdapMapQuery;
import org.keycloak.models.map.storage.ldap.role.config.LdapMapRoleMapperConfig;
import org.keycloak.models.map.storage.ldap.role.entity.LdapRoleEntity;
import org.keycloak.provider.Provider;

import javax.naming.NamingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LdapRoleMapKeycloakTransaction extends LdapMapKeycloakTransaction<LdapMapRoleEntityFieldDelegate, MapRoleEntity, RoleModel> implements Provider {

    private final StringKeyConverter<String> keyConverter = new StringKeyConverter.StringKey();
    private final Set<String> deletedKeys = new HashSet<>();
    private final LdapMapRoleMapperConfig roleMapperConfig;
    private final LdapMapConfig ldapMapConfig;
    private final LdapMapIdentityStore identityStore;

    public LdapRoleMapKeycloakTransaction(KeycloakSession session, Config.Scope config) {
        this.roleMapperConfig = new LdapMapRoleMapperConfig(config);
        this.ldapMapConfig = new LdapMapConfig(config);
        this.identityStore = new LdapMapIdentityStore(session, ldapMapConfig);
        session.enlistForClose(this);
    }

    // interface matching the constructor of this class
    public interface LdapRoleMapKeycloakTransactionFunction<A, B, R> {
        R apply(A a, B b);
    }

    // TODO: entries might get stale if a DN of an entry changes due to changes in the entity in the same transaction
    private final Map<String, String> dns = new HashMap<>();

    public String readIdByDn(String dn) {
        // TODO: this might not be necessary if the LDAP server would support an extended OID
        // https://ldapwiki.com/wiki/LDAP_SERVER_EXTENDED_DN_OID

        String id = dns.get(dn);
        if (id == null) {
            for (Map.Entry<String, LdapMapRoleEntityFieldDelegate> entry : entities.entrySet()) {
                LdapMapObject ldap = entry.getValue().getLdapMapObject();
                if (ldap.getDn().toString().equals(dn)) {
                    id = ldap.getId();
                    break;
                }
            }
        }
        if (id != null) {
            return id;
        }

        LdapMapQuery ldapQuery = new LdapMapQuery();

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapQuery.setSearchScope(ldapMapConfig.getSearchScope());
        ldapQuery.setSearchDn(roleMapperConfig.getCommonRolesDn());

        // TODO: read them properly to be able to store them in the transaction so they are cached?!
        Collection<String> roleObjectClasses = ldapMapConfig.getRoleObjectClasses();
        ldapQuery.addObjectClasses(roleObjectClasses);

        String rolesRdnAttr = roleMapperConfig.getRoleNameLdapAttribute();

        ldapQuery.addReturningLdapAttribute(rolesRdnAttr);

        LdapMapDn.RDN rdn = LdapMapDn.fromString(dn).getFirstRdn();
        String key = rdn.getAllKeys().get(0);
        String value = rdn.getAttrValue(key);

        LdapRoleModelCriteriaBuilder mcb =
                new LdapRoleModelCriteriaBuilder(roleMapperConfig).compare(RoleModel.SearchableFields.NAME, ModelCriteriaBuilder.Operator.EQ, value);
        mcb = mcb.withCustomFilter(roleMapperConfig.getCustomLdapFilter());
        ldapQuery.setModelCriteriaBuilder(mcb);

        List<LdapMapObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);
        if (ldapObjects.size() == 1) {
            dns.put(dn, ldapObjects.get(0).getId());
            return ldapObjects.get(0).getId();
        }
        return null;
    }

    private MapModelCriteriaBuilder<String, MapRoleEntity, RoleModel> createCriteriaBuilderMap() {
        // The realmId might not be set of instances retrieved by read(id) and we're still sure that they belong to the realm being searched.
        // Therefore, ignore the field realmId when searching the instances that are stored within the transaction.
        return new MapModelCriteriaBuilderAssumingEqualForField<>(keyConverter, MapFieldPredicates.getPredicates(RoleModel.class), RoleModel.SearchableFields.REALM_ID);
    }

    @Override
    public LdapMapRoleEntityFieldDelegate create(MapRoleEntity value) {
        DeepCloner CLONER = new DeepCloner.Builder()
                .constructor(MapRoleEntity.class, cloner -> new LdapMapRoleEntityFieldDelegate(new LdapRoleEntity(cloner, roleMapperConfig, this, value.getClientId())))
                .build();

        LdapMapRoleEntityFieldDelegate mapped = (LdapMapRoleEntityFieldDelegate) CLONER.from(value);

        // LDAP should never use the UUID provided by the caller, as UUID is generated by the LDAP directory
        mapped.setId(null);
        // Roles as groups need to have at least one member on most directories. Add ourselves as a member as a dummy.
        if (mapped.getLdapMapObject().getId() == null && mapped.getLdapMapObject().getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute()) == null) {
            // insert our own name as dummy member of this role to avoid a schema conflict in LDAP
            mapped.getLdapMapObject().setAttribute(roleMapperConfig.getMembershipLdapAttribute(), Stream.of(mapped.getLdapMapObject().getDn().toString()).collect(Collectors.toSet()));
        }

        try {
            // in order to get the ID, we need to write it to LDAP
            identityStore.add(mapped.getLdapMapObject());
            // TODO: add a flag for temporary created roles until they are finally committed so that they don't show up in ready(query) in their temporary state
        } catch (ModelException ex) {
            if (value.isClientRole() && ex.getCause() instanceof NamingException) {
                // the client hasn't been created, therefore adding it here
                LdapMapObject client = new LdapMapObject();
                client.setObjectClasses(Arrays.asList("top", "organizationalUnit"));
                client.setRdnAttributeName("ou");
                client.setDn(LdapMapDn.fromString(roleMapperConfig.getRolesDn(mapped.isClientRole(), mapped.getClientId())));
                client.setSingleAttribute("ou", mapped.getClientId());
                identityStore.add(client);

                tasksOnRollback.add(new DeleteOperation() {
                    @Override
                    public void execute() {
                        identityStore.remove(client);
                    }
                });

                // retry creation of client role
                identityStore.add(mapped.getLdapMapObject());
            }
        }

        entities.put(mapped.getId(), mapped);

        tasksOnRollback.add(new DeleteOperation() {
            @Override
            public void execute() {
                identityStore.remove(mapped.getLdapMapObject());
                entities.remove(mapped.getId());
            }
        });

        return mapped;
    }

    @Override
    public boolean delete(String key) {
        LdapMapRoleEntityFieldDelegate read = read(key);
        if (read == null) {
            throw new ModelException("unable to read entity with key " + key);
        }
        if (!deletedKeys.contains((key))) {
            // avoid enlisting LDAP removal twice if client calls it twice
            deletedKeys.add(key);
            tasksOnCommit.add(new DeleteOperation() {
                @Override
                public void execute() {
                    identityStore.remove(read.getLdapMapObject());
                    // once removed from LDAP, avoid updating a modified entity in LDAP.
                    entities.remove(read.getId());
                }
            });
        }
        return true;
    }

    public LdapRoleEntity readLdap(String key) {
        LdapMapRoleEntityFieldDelegate read = read(key);
        if (read == null) {
            return null;
        } else {
            return read.getEntityFieldDelegate();
        }
    }

    @Override
    public LdapMapRoleEntityFieldDelegate read(String key) {
        if (deletedKeys.contains(key)) {
            return null;
        }

        // reuse an existing live entity
        LdapMapRoleEntityFieldDelegate val = entities.get(key);

        if (val == null) {

            // try to look it up as a realm role
            val = lookupEntityById(key, null);

            if (val == null) {
                // try to find out the client ID
                LdapMapQuery ldapQuery = new LdapMapQuery();

                // For now, use same search scope, which is configured "globally" and used for user's search.
                ldapQuery.setSearchScope(ldapMapConfig.getSearchScope());

                // remove prefix with placeholder to allow for a broad search
                String sdn = roleMapperConfig.getClientRolesDn();
                ldapQuery.setSearchDn(sdn.replaceAll(".*\\{0},", ""));

                LdapMapObject ldapObject = identityStore.fetchById(key, ldapQuery);
                if (ldapObject != null) {
                    // as the client ID is now known, search again with the specific configuration
                    LdapMapDn.RDN firstRdn = ldapObject.getDn().getParentDn().getFirstRdn();
                    String clientId = firstRdn.getAttrValue(firstRdn.getAllKeys().get(0));
                    // lookup with clientId, as the search above might have been broader than a restricted search
                    val = lookupEntityById(key, clientId);
                }
            }

            if (val != null) {
                entities.put(key, val);
            }

        }
        return val;
    }

    private LdapMapRoleEntityFieldDelegate lookupEntityById(String id, String clientId) {
        LdapMapQuery ldapQuery = getLdapQuery(clientId != null, clientId);

        LdapMapObject ldapObject = identityStore.fetchById(id, ldapQuery);
        if (ldapObject != null) {
            return new LdapMapRoleEntityFieldDelegate(new LdapRoleEntity(ldapObject, roleMapperConfig, this, clientId));
        }
        return null;
    }

    @Override
    public Stream<MapRoleEntity> read(QueryParameters<RoleModel> queryParameters) {
        LdapRoleModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createLdapModelCriteriaBuilder());

        Boolean isClientRole = mcb.isClientRole();
        String clientId = mcb.getClientId();

        LdapMapQuery ldapQuery = getLdapQuery(isClientRole, clientId);

        mcb = mcb.withCustomFilter(roleMapperConfig.getCustomLdapFilter());
        ldapQuery.setModelCriteriaBuilder(mcb);

        Stream<MapRoleEntity> ldapStream;

        MapModelCriteriaBuilder<String,MapRoleEntity,RoleModel> mapMcb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(createCriteriaBuilderMap());

        Stream<LdapMapRoleEntityFieldDelegate> existingEntities = entities.entrySet().stream()
                .filter(me -> mapMcb.getKeyFilter().test(keyConverter.fromString(me.getKey())) && !deletedKeys.contains(me.getKey()))
                .map(Map.Entry::getValue)
                .filter(mapMcb.getEntityFilter())
                // snapshot list
                .collect(Collectors.toList()).stream();

        // current approach: combine the results in a correct way from existing entities in the transaction and LDAP
        // problem here: pagination doesn't work any more as results are retrieved from both, and then need to be sorted
        // possible alternative: use search criteria only on LDAP, and replace found entities with those stored in transaction already
        // this will then not find additional entries modified or created in this transaction

        try {
            List<LdapMapObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);

            ldapStream = ldapObjects.stream().map(ldapMapObject -> {
                        // we might have fetch client and realm roles at the same time, now try to decode what is what
                        StreamUtils.Pair<Boolean, String> client = getClientId(ldapMapObject.getDn());
                        if (client == null) {
                            return null;
                        }
                        LdapMapRoleEntityFieldDelegate entity = new LdapMapRoleEntityFieldDelegate(new LdapRoleEntity(ldapMapObject, roleMapperConfig, this, client.getV()));
                        LdapMapRoleEntityFieldDelegate existingEntry = entities.get(entity.getId());
                        if (existingEntry != null) {
                            // this entry will be part of the existing entities
                            return null;
                        }
                        entities.put(entity.getId(), entity);
                        return (MapRoleEntity) entity;
                    })
                    .filter(Objects::nonNull)
                    .filter(me -> !deletedKeys.contains(me.getId()))
                    // re-apply filters about client roles that we might have skipped for LDAP
                    .filter(me -> mapMcb.getKeyFilter().test(me.getId()))
                    .filter(me -> mapMcb.getEntityFilter().test(me))
                    // snapshot list, as the contents depends on entities and also updates the entities,
                    // and two streams open at the same time could otherwise interfere
                    .collect(Collectors.toList()).stream();
        } catch (ModelException ex) {
            if (clientId != null && ex.getCause() instanceof NamingException) {
                // the client wasn't found in LDAP, assume an empty result
                ldapStream = Stream.empty();
            } else {
                throw ex;
            }
        }

        ldapStream = Stream.concat(ldapStream, existingEntities);

        if (!queryParameters.getOrderBy().isEmpty()) {
            ldapStream = ldapStream.sorted(MapFieldPredicates.getComparator(queryParameters.getOrderBy().stream()));
        }
        if (queryParameters.getOffset() != null) {
            ldapStream = ldapStream.skip(queryParameters.getOffset());
        }
        if (queryParameters.getLimit() != null) {
            ldapStream = ldapStream.limit(queryParameters.getLimit());
        }

        return ldapStream;
    }

    private StreamUtils.Pair<Boolean, String> getClientId(LdapMapDn dn) {
        if (dn.getParentDn().equals(LdapMapDn.fromString(roleMapperConfig.getRealmRolesDn()))) {
            return new StreamUtils.Pair<>(false, null);
        }
        String clientsDnWildcard = roleMapperConfig.getClientRolesDn();
        if (clientsDnWildcard != null) {
            clientsDnWildcard = clientsDnWildcard.replaceAll(".*\\{0},", "");
            if (dn.getParentDn().getParentDn().equals(LdapMapDn.fromString(clientsDnWildcard))) {
                LdapMapDn.RDN firstRdn = dn.getParentDn().getFirstRdn();
                return new StreamUtils.Pair<>(true, firstRdn.getAttrValue(firstRdn.getAllKeys().get(0)));
            }
        }
        return null;
    }

    private LdapMapQuery getLdapQuery(Boolean isClientRole, String clientId) {
        LdapMapQuery ldapMapQuery = new LdapMapQuery();

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapMapQuery.setSearchScope(ldapMapConfig.getSearchScope());

        String rolesDn = roleMapperConfig.getRolesDn(isClientRole, clientId);
        ldapMapQuery.setSearchDn(rolesDn);

        Collection<String> roleObjectClasses = ldapMapConfig.getRoleObjectClasses();
        ldapMapQuery.addObjectClasses(roleObjectClasses);

        String rolesRdnAttr = roleMapperConfig.getRoleNameLdapAttribute();

        ldapMapQuery.addReturningLdapAttribute(rolesRdnAttr);
        ldapMapQuery.addReturningLdapAttribute("description");
        ldapMapQuery.addReturningLdapAttribute(roleMapperConfig.getMembershipLdapAttribute());
        roleMapperConfig.getRoleAttributes().forEach(ldapMapQuery::addReturningLdapAttribute);
        return ldapMapQuery;
    }

    @Override
    public void commit() {
        super.commit();
        for (MapTaskWithValue mapTaskWithValue : tasksOnCommit) {
            mapTaskWithValue.execute();
        }

        entities.forEach((entityKey, entity) -> {
            if (entity.isUpdated()) {
                identityStore.update(entity.getLdapMapObject());
            }
        });
        // once the commit is complete, clear the local storage to avoid problems when rollback() is called later
        // due to a different transaction failing.
        tasksOnCommit.clear();
        entities.clear();
        tasksOnRollback.clear();
    }

    @Override
    public void rollback() {
        super.rollback();
        Iterator<MapTaskWithValue> iterator = tasksOnRollback.descendingIterator();
        while (iterator.hasNext()) {
            iterator.next().execute();
        }
    }

    protected LdapRoleModelCriteriaBuilder createLdapModelCriteriaBuilder() {
        return new LdapRoleModelCriteriaBuilder(roleMapperConfig);
    }

    @Override
    public void close() {
        identityStore.close();
    }

}
