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
package org.keycloak.models.map.storage.ldap.user;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapKeycloakTransactionWithAuth;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.ldap.config.LdapKerberosConfig;
import org.keycloak.models.map.storage.ldap.user.kerberos.impl.KerberosServerSubjectAuthenticator;
import org.keycloak.models.map.storage.ldap.user.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.models.map.user.MapCredentialValidationOutput;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.LdapMapKeycloakTransaction;
import org.keycloak.models.map.storage.ldap.MapModelCriteriaBuilderAssumingEqualForField;
import org.keycloak.models.map.storage.ldap.config.LdapMapConfig;
import org.keycloak.models.map.storage.ldap.model.LdapMapDn;
import org.keycloak.models.map.storage.ldap.model.LdapMapObject;
import org.keycloak.models.map.storage.ldap.model.LdapMapQuery;
import org.keycloak.models.map.storage.ldap.store.LdapMapIdentityStore;
import org.keycloak.models.map.storage.ldap.user.config.LdapMapUserMapperConfig;
import org.keycloak.models.map.storage.ldap.user.entity.LdapMapUserEntityFieldDelegate;
import org.keycloak.models.map.storage.ldap.user.entity.LdapUserEntity;
import org.keycloak.provider.Provider;

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

import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class LdapUserMapKeycloakTransaction extends LdapMapKeycloakTransaction<LdapMapUserEntityFieldDelegate, MapUserEntity, UserModel>
        implements Provider, MapKeycloakTransactionWithAuth<MapUserEntity, UserModel> {

    private static final Logger logger = Logger.getLogger(LdapUserMapKeycloakTransaction.class);
    private final StringKeyConverter<String> keyConverter = new StringKeyConverter.StringKey();
    private final Set<String> deletedKeys = new HashSet<>();
    private final LdapMapUserMapperConfig userMapperConfig;
    private final LdapMapConfig ldapMapConfig;
    private final LdapMapIdentityStore identityStore;

    @Deprecated
    private MapKeycloakTransaction<MapUserEntity, UserModel> delegate;

    public LdapUserMapKeycloakTransaction(KeycloakSession session, Config.Scope config) {
        this.userMapperConfig = new LdapMapUserMapperConfig(config);
        this.ldapMapConfig = new LdapMapConfig(config);
        this.identityStore = new LdapMapIdentityStore(session, ldapMapConfig);
        session.enlistForClose(this);
    }

    public void setDelegate(MapKeycloakTransaction<MapUserEntity,UserModel> delegate) {
        this.delegate = delegate;
    }

    public MapKeycloakTransaction<MapUserEntity, UserModel> getDelegate() {
        return delegate;
    }

    public LdapMapIdentityStore getIdentityStore() {
        return identityStore;
    }

    @Override
    public MapCredentialValidationOutput<MapUserEntity> authenticate(RealmModel realm, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return null;
        UserCredentialModel credential = (UserCredentialModel)input;
        if (credential.getType().equals(UserCredentialModel.KERBEROS)) {
            String spnegoToken = credential.getChallengeResponse();

            LdapKerberosConfig kerberosConfig = new LdapKerberosConfig(ldapMapConfig);
            KerberosServerSubjectAuthenticator kerberosAuth = new KerberosServerSubjectAuthenticator(kerberosConfig);
            SPNEGOAuthenticator spnegoAuthenticator = new SPNEGOAuthenticator(kerberosConfig, kerberosAuth, spnegoToken);
            spnegoAuthenticator.authenticate();

            Map<String, String> state = new HashMap<>();
            if (spnegoAuthenticator.isAuthenticated()) {
                MapUserEntity user = findOrCreateAuthenticatedUser(realm, spnegoAuthenticator.getAuthenticatedUsername(), spnegoAuthenticator.getKerberosRealm());
                if (user == null) {
                    return MapCredentialValidationOutput.failed();
                } else {
                    String delegationCredential = spnegoAuthenticator.getSerializedDelegationCredential();
                    if (delegationCredential != null) {
                        state.put(KerberosConstants.GSS_DELEGATION_CREDENTIAL, delegationCredential);
                    }
                    return new MapCredentialValidationOutput<>(user, CredentialValidationOutput.Status.AUTHENTICATED, state);
                }
            }  else if (spnegoAuthenticator.getResponseToken() != null) {
                // Case when SPNEGO handshake requires multiple steps
                logger.tracef("SPNEGO Handshake will continue");
                state.put(KerberosConstants.RESPONSE_TOKEN, spnegoAuthenticator.getResponseToken());
                return new MapCredentialValidationOutput<>(null, CredentialValidationOutput.Status.CONTINUE, state);
            } else {
                logger.tracef("SPNEGO Handshake not successful");
                return MapCredentialValidationOutput.failed();
            }

        } else {
            return null;
        }
    }

    private MapUserEntity findOrCreateAuthenticatedUser(RealmModel realm, String username, String kerberosRealm) {
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(UserModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
        .compare(UserModel.SearchableFields.USERNAME, ModelCriteriaBuilder.Operator.EQ, username)
                // .compare(UserModel.SearchableFields.FEDERATION_LINK, ModelCriteriaBuilder.Operator.EQ, kerberosRealm)
        ;
        List<MapUserEntity> users = read(withCriteria(mcb)).limit(2).collect(Collectors.toList());
        if (users.size() != 1) {
            return null;
        } else {
            return users.get(0);
        }

    }

    @Override
    public void close() {
        identityStore.close();
    }

    // interface matching the constructor of this class
    public interface LdapUserMapKeycloakTransactionFunction<A, B, R> {
        R apply(A a, B b);
    }

    // TODO: entries might get stale if a DN of an entry changes due to changes in the entity in the same transaction
    private final Map<String, String> dns = new HashMap<>();

    public String readIdByDn(String dn) {
        // TODO: this might not be necessary if the LDAP server would support an extended OID
        // https://ldapwiki.com/wiki/LDAP_SERVER_EXTENDED_DN_OID

        String id = dns.get(dn);
        if (id == null) {
            for (Map.Entry<String, LdapMapUserEntityFieldDelegate> entry : entities.entrySet()) {
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
        ldapQuery.setSearchDn(userMapperConfig.getCommonUsersDn());

        // TODO: read them properly to be able to store them in the transaction so they are cached?!
        Collection<String> userObjectClasses = ldapMapConfig.getUserObjectClasses();
        ldapQuery.addObjectClasses(userObjectClasses);

        String usersRdnAttr = userMapperConfig.getUserNameLdapAttribute();

        ldapQuery.addReturningLdapAttribute(usersRdnAttr);
        ldapQuery.addReturningLdapAttribute("sn");
        ldapQuery.addReturningLdapAttribute("cn");
        ldapQuery.addReturningLdapAttribute("mail");

        LdapMapDn.RDN rdn = LdapMapDn.fromString(dn).getFirstRdn();
        String key = rdn.getAllKeys().get(0);
        String value = rdn.getAttrValue(key);

        LdapUserModelCriteriaBuilder mcb =
                new LdapUserModelCriteriaBuilder(userMapperConfig).compare(UserModel.SearchableFields.USERNAME, ModelCriteriaBuilder.Operator.EQ, value);
        mcb = mcb.withCustomFilter(userMapperConfig.getCustomLdapFilter());
        ldapQuery.setModelCriteriaBuilder(mcb);

        List<LdapMapObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);
        if (ldapObjects.size() == 1) {
            dns.put(dn, ldapObjects.get(0).getId());
            return ldapObjects.get(0).getId();
        }
        return null;
    }

    private MapModelCriteriaBuilder<String, MapUserEntity, UserModel> createCriteriaBuilderMap() {
        // The realmId might not be set of instances retrieved by read(id) and we're still sure that they belong to the realm being searched.
        // Therefore, ignore the field realmId when searching the instances that are stored within the transaction.
        return new MapModelCriteriaBuilderAssumingEqualForField<>(keyConverter, MapFieldPredicates.getPredicates(UserModel.class), UserModel.SearchableFields.REALM_ID);
    }

    @Override
    public LdapMapUserEntityFieldDelegate create(MapUserEntity value) {
        DeepCloner CLONER = new DeepCloner.Builder()
                .constructor(MapUserEntity.class, cloner -> new LdapMapUserEntityFieldDelegate(new LdapUserEntity(cloner, userMapperConfig, this)))
                .build();

        LdapMapUserEntityFieldDelegate mapped = (LdapMapUserEntityFieldDelegate) CLONER.from(value);

        // LDAP should never use the UUID provided by the caller, as UUID is generated by the LDAP directory
        mapped.setId(null);

        // in order to get the ID, we need to write it to LDAP
        identityStore.add(mapped.getLdapMapObject());
        // TODO: add a flag for temporary created users until they are finally committed so that they don't show up in ready(query) in their temporary state

        mapped.getEntityFieldDelegate().createDelegate();

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
        if (deletedKeys.contains(key)) {
            return true;
        }
        LdapMapUserEntityFieldDelegate read = read(key);
        if (read == null) {
            throw new ModelException("unable to read entity with key " + key);
        }
        deletedKeys.add(key);
        tasksOnCommit.add(new DeleteOperation() {
            @Override
            public void execute() {
                identityStore.remove(read.getLdapMapObject());
                // once removed from LDAP, avoid updating a modified entity in LDAP.
                entities.remove(key);
            }
        });
        delegate.delete(key);
        return true;
    }

    @Override
    public LdapMapUserEntityFieldDelegate read(String key) {
        if (deletedKeys.contains(key)) {
            return null;
        }

        // reuse an existing live entity
        LdapMapUserEntityFieldDelegate val = entities.get(key);

        if (val == null) {

            // try to look it up as a realm user
            val = lookupEntityById(key);

            if (val != null) {
                entities.put(key, val);
            }

        }
        return val;
    }

    private LdapMapUserEntityFieldDelegate lookupEntityById(String id) {
        LdapMapQuery ldapQuery = getLdapQuery();

        LdapMapObject ldapObject = identityStore.fetchById(id, ldapQuery);
        if (ldapObject != null) {
            return new LdapMapUserEntityFieldDelegate(new LdapUserEntity(ldapObject, userMapperConfig, this));
        }
        return null;
    }

    @Override
    public Stream<MapUserEntity> read(QueryParameters<UserModel> queryParameters) {
        LdapUserModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createLdapModelCriteriaBuilder());

        LdapMapQuery ldapQuery = getLdapQuery();

        mcb = mcb.withCustomFilter(userMapperConfig.getCustomLdapFilter());
        ldapQuery.setModelCriteriaBuilder(mcb);

        Stream<MapUserEntity> ldapStream;

        MapModelCriteriaBuilder<String,MapUserEntity,UserModel> mapMcb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(createCriteriaBuilderMap());

        Stream<LdapMapUserEntityFieldDelegate> existingEntities = entities.entrySet().stream()
                .filter(me -> mapMcb.getKeyFilter().test(keyConverter.fromString(me.getKey())) && !deletedKeys.contains(me.getKey()))
                .map(Map.Entry::getValue)
                .filter(mapMcb.getEntityFilter())
                // snapshot list
                .collect(Collectors.toList()).stream();

        List<LdapMapObject> ldapObjects = identityStore.fetchQueryResults(ldapQuery);

        ldapStream = ldapObjects.stream().map(ldapMapObject -> {
                    // we might have fetch client and realm users at the same time, now try to decode what is what
                    LdapMapUserEntityFieldDelegate entity = new LdapMapUserEntityFieldDelegate(new LdapUserEntity(ldapMapObject, userMapperConfig, this));
                    LdapMapUserEntityFieldDelegate existingEntry = entities.get(entity.getId());
                    if (existingEntry != null) {
                        // this entry will be part of the existing entities
                        return null;
                    }
                    entities.put(entity.getId(), entity);
                    return (MapUserEntity) entity;
                })
                .filter(Objects::nonNull)
                .filter(me -> !deletedKeys.contains(me.getId()))
                // re-apply filters about client users that we might have skipped for LDAP
                .filter(me -> mapMcb.getKeyFilter().test(me.getId()))
                .filter(me -> mapMcb.getEntityFilter().test(me))
                // snapshot list, as the contents depends on entities and also updates the entities,
                // and two streams open at the same time could otherwise interfere
                .collect(Collectors.toList()).stream();

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

    private LdapMapQuery getLdapQuery() {
        LdapMapQuery ldapMapQuery = new LdapMapQuery();

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapMapQuery.setSearchScope(ldapMapConfig.getSearchScope());

        String usersDn = ldapMapConfig.getUsersDn();
        ldapMapQuery.setSearchDn(usersDn);

        Collection<String> userObjectClasses = ldapMapConfig.getUserObjectClasses();
        ldapMapQuery.addObjectClasses(userObjectClasses);

        String usersRdnAttr = userMapperConfig.getUserNameLdapAttribute();

        ldapMapQuery.addReturningLdapAttribute(usersRdnAttr);
        ldapMapQuery.addReturningLdapAttribute("sn");
        ldapMapQuery.addReturningLdapAttribute("cn");
        ldapMapQuery.addReturningLdapAttribute("mail");

        ldapMapQuery.addReturningLdapAttribute(userMapperConfig.getMembershipLdapAttribute());
        userMapperConfig.getUserAttributes().forEach(ldapMapQuery::addReturningLdapAttribute);
        return ldapMapQuery;
    }

    @Override
    public void commit() {
        super.commit();
        delegate.commit();

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
        delegate.rollback();

        Iterator<MapTaskWithValue> iterator = tasksOnRollback.descendingIterator();
        while (iterator.hasNext()) {
            iterator.next().execute();
        }
    }

    protected LdapUserModelCriteriaBuilder createLdapModelCriteriaBuilder() {
        return new LdapUserModelCriteriaBuilder(userMapperConfig);
    }

}
