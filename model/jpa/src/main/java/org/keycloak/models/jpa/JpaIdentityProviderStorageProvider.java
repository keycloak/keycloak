/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.util.IdentityProviderTypeUtil;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.IdentityProviderShowInAccountConsole;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.IdentityProviderType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.IdentityProviderEntity;
import org.keycloak.models.jpa.entities.IdentityProviderMapperEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.utils.StringUtil;

import org.hibernate.Session;
import org.jboss.logging.Logger;

import static org.keycloak.models.IdentityProviderModel.ALIAS;
import static org.keycloak.models.IdentityProviderModel.ALIAS_NOT_IN;
import static org.keycloak.models.IdentityProviderModel.AUTHENTICATE_BY_DEFAULT;
import static org.keycloak.models.IdentityProviderModel.DISPLAY_NAME;
import static org.keycloak.models.IdentityProviderModel.ENABLED;
import static org.keycloak.models.IdentityProviderModel.FIRST_BROKER_LOGIN_FLOW_ID;
import static org.keycloak.models.IdentityProviderModel.HIDE_ON_LOGIN;
import static org.keycloak.models.IdentityProviderModel.LINK_ONLY;
import static org.keycloak.models.IdentityProviderModel.ORGANIZATION_ID;
import static org.keycloak.models.IdentityProviderModel.ORGANIZATION_ID_NOT_NULL;
import static org.keycloak.models.IdentityProviderModel.POST_BROKER_LOGIN_FLOW_ID;
import static org.keycloak.models.IdentityProviderModel.SEARCH;
import static org.keycloak.models.IdentityProviderModel.SHOW_IN_ACCOUNT_CONSOLE;
import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * A JPA based implementation of {@link IdentityProviderStorageProvider}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaIdentityProviderStorageProvider implements IdentityProviderStorageProvider {

    protected static final Logger logger = Logger.getLogger(IdentityProviderStorageProvider.class);

    private final EntityManager em;
    private final KeycloakSession session;

    public JpaIdentityProviderStorageProvider(KeycloakSession session) {
        this.session = session;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public IdentityProviderModel create(IdentityProviderModel identityProvider) {
        IdentityProviderEntity entity = new IdentityProviderEntity();
        if (identityProvider.getInternalId() == null) {
            entity.setInternalId(KeycloakModelUtils.generateId());
        } else {
            entity.setInternalId(identityProvider.getInternalId());
        }

        entity.setAlias(identityProvider.getAlias());
        entity.setRealmId(this.getRealm().getId());
        entity.setDisplayName(identityProvider.getDisplayName());
        entity.setProviderId(identityProvider.getProviderId());
        entity.setEnabled(identityProvider.isEnabled());
        entity.setStoreToken(identityProvider.isStoreToken());
        entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
        entity.setTrustEmail(identityProvider.isTrustEmail());
        entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
        entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
        entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
        entity.setOrganizationId(identityProvider.getOrganizationId());
        entity.setConfig(identityProvider.getConfig());
        entity.setLinkOnly(identityProvider.isLinkOnly());
        entity.setHideOnLogin(identityProvider.isHideOnLogin());
        em.persist(entity);
        // flush so that constraint violations are flagged and converted into model exception now rather than at the end of the tx.
        em.flush();

        identityProvider.setInternalId(entity.getInternalId());
        return identityProvider;
    }

    @Override
    public void update(IdentityProviderModel identityProvider) {
        // find idp by id and update it.
        IdentityProviderEntity entity = this.getEntityById(identityProvider.getInternalId(), true);
        entity.setAlias(identityProvider.getAlias());
        entity.setDisplayName(identityProvider.getDisplayName());
        entity.setEnabled(identityProvider.isEnabled());
        entity.setTrustEmail(identityProvider.isTrustEmail());
        entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
        entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
        entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
        entity.setOrganizationId(identityProvider.getOrganizationId());
        entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
        entity.setStoreToken(identityProvider.isStoreToken());
        entity.setConfig(identityProvider.getConfig());
        entity.setLinkOnly(identityProvider.isLinkOnly());
        entity.setHideOnLogin(identityProvider.isHideOnLogin());

        // flush so that constraint violations are flagged and converted into model exception now rather than at the end of the tx.
        em.flush();

        // send identity provider updated event.
        RealmModel realm = this.getRealm();
        session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderUpdatedEvent() {

            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public IdentityProviderModel getUpdatedIdentityProvider() {
                return identityProvider;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
    }

    @Override
    public boolean remove(String alias) {
        // find provider by alias in the DB and remove it.
        IdentityProviderEntity entity = this.getEntityByAlias(alias);

        if (entity != null) {
            //call toModel(entity) now as after em.remove(entity) and the flush it might throw LazyInitializationException
            //when accessing the config of the entity (entity.getConfig()) withing the toModel(entity)
            IdentityProviderModel model = toModel(entity);

            em.remove(entity);
            // flush so that constraint violations are flagged and converted into model exception now rather than at the end of the tx.
            em.flush();

            session.identityProviders().getMappersByAliasStream(alias).forEach(session.identityProviders()::removeMapper);

            // send identity provider removed event.
            RealmModel realm = this.getRealm();
            session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderRemovedEvent() {

                @Override
                public RealmModel getRealm() {
                    return realm;
                }

                @Override
                public IdentityProviderModel getRemovedIdentityProvider() {
                    return model;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void removeAll() {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaDelete<IdentityProviderEntity> delete = builder.createCriteriaDelete(IdentityProviderEntity.class);
        Root<IdentityProviderEntity> idp = delete.from(IdentityProviderEntity.class);
        delete.where(builder.equal(idp.get("realmId"), this.getRealm().getId()));
        this.em.createQuery(delete).executeUpdate();
    }

    @Override
    public IdentityProviderModel getById(String internalId) {
        return toModel(getEntityById(internalId, false));
    }

    @Override
    public IdentityProviderModel getByAlias(String alias) {
        return toModel(getEntityByAlias(alias));
    }

    @Override
    public Stream<IdentityProviderModel> getAllStream(IdentityProviderQuery query, Integer first, Integer max) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<IdentityProviderEntity> cq = builder.createQuery(IdentityProviderEntity.class);
        Root<IdentityProviderEntity> idp = cq.from(IdentityProviderEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(idp.get("realmId"), getRealm().getId()));

        List<String> includedProviderFactories = null;
        if (query.getType() != null && query.getType() != IdentityProviderType.ANY) {
            includedProviderFactories = IdentityProviderTypeUtil.listFactoriesByType(session, query.getType());
        } else if (query.getCapability() != null) {
            includedProviderFactories = IdentityProviderTypeUtil.listFactoriesByCapability(session, query.getCapability());
        }

        if (includedProviderFactories != null) {
            predicates.add(builder.in(idp.get("providerId")).value(includedProviderFactories));
        }

        if (query.getOptions() != null) {
            for (Map.Entry<String, String> entry : query.getOptions().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (StringUtil.isBlank(key)) {
                    continue;
                }
                switch(key) {
                    case AUTHENTICATE_BY_DEFAULT:
                    case ENABLED:
                    case HIDE_ON_LOGIN:
                    case LINK_ONLY: {
                        Path<Boolean> path = idp.get(key);
                        if (Boolean.parseBoolean(value)) {
                            predicates.add(builder.isTrue(path));
                        } else {
                            predicates.add(builder.or(builder.isNull(path), builder.equal(path, Boolean.FALSE)));
                        }
                        break;
                    }
                    case ALIAS:
                    case FIRST_BROKER_LOGIN_FLOW_ID:
                    case POST_BROKER_LOGIN_FLOW_ID:
                    case ORGANIZATION_ID: {
                        if (StringUtil.isBlank(value)) {
                            predicates.add(builder.isNull(idp.get(key)));
                        } else {
                            predicates.add(builder.equal(idp.get(key), value));
                        }
                        break;
                    }
                    case ORGANIZATION_ID_NOT_NULL: {
                        predicates.add(builder.isNotNull(idp.get(ORGANIZATION_ID)));
                        break;
                    }
                    case SEARCH: {
                        if (StringUtil.isNotBlank(value)) {
                            predicates.add(this.getAliasSearchPredicate(value, builder, idp));
                        }
                        break;
                    }
                    case ALIAS_NOT_IN: {
                        if (StringUtil.isNotBlank(value)) {
                            List<String> aliases = Arrays.asList(value.split(","));
                            predicates.add(builder.not(idp.get(ALIAS).in(aliases)));
                        }
                        break;
                    }
                    default: {
						boolean orNull = switch (key) {
							case SHOW_IN_ACCOUNT_CONSOLE -> IdentityProviderShowInAccountConsole.ALWAYS.name().equals(value);
							default -> false;
						};
						List<Predicate> orPredicates = new ArrayList<>();
						orPredicates.add(createConfigPredicate(builder, idp, key, value));
						if (orNull) {
							orPredicates.add(createConfigPredicate(builder, idp, key, null));
						}

						predicates.add(builder.or(orPredicates.toArray(Predicate[]::new)));
                    }
                }
            }
        }

        cq.orderBy(builder.asc(idp.get(ALIAS)));
        TypedQuery<IdentityProviderEntity> typedQuery = em.createQuery(cq.select(idp).where(predicates.toArray(Predicate[]::new)));
        return closing(paginateQuery(typedQuery, first, max).getResultStream()).map(this::toModel);
    }

	private Predicate createConfigPredicate(CriteriaBuilder builder, Root<IdentityProviderEntity> idp, String key, String value) {
		String dbProductName = em.unwrap(Session.class).doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());
		MapJoin<IdentityProviderEntity, String, String> configJoin = idp.joinMap("config", JoinType.LEFT);
		configJoin.on(builder.equal(configJoin.key(), key));

		if (value == null)  {
			return builder.isNull(configJoin.value());
		}

		if (dbProductName.equals("Oracle")) {
			// SELECT * FROM identity_provider_config WHERE ... DBMS_LOB.COMPARE(value, '0') = 0 ...;
			// Oracle is not able to compare a CLOB with a VARCHAR unless it being converted with TO_CHAR
			// But for this all values in the table need to be smaller than 4K, otherwise the cast will fail with
			// "ORA-22835: Buffer too small for CLOB to CHAR" (even if it is in another row).
			// This leaves DBMS_LOB.COMPARE as the option to compare the CLOB with the value.
			return builder.equal(builder.function("DBMS_LOB.COMPARE", Integer.class, configJoin.value(), builder.literal(value)), 0);
		} else {
			return builder.equal(configJoin.value(), value);
		}
	}

    @Override
    public Stream<String> getByFlow(String flowId, String search, Integer first, Integer max) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> query = builder.createQuery(String.class);
        Root<IdentityProviderEntity> idp = query.from(IdentityProviderEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(idp.get("realmId"), getRealm().getId()));

        if (StringUtil.isNotBlank(flowId)) {
            predicates.add(builder.or(
                    builder.equal(idp.get(FIRST_BROKER_LOGIN_FLOW_ID), flowId),
                    builder.equal(idp.get(POST_BROKER_LOGIN_FLOW_ID), flowId)
            ));
        }

        if (StringUtil.isNotBlank(search)) {
            predicates.add(this.getAliasSearchPredicate(search, builder, idp));
        }

        query.orderBy(builder.asc(idp.get(ALIAS)));
        TypedQuery<String> typedQuery = em.createQuery(query.select(idp.get(ALIAS)).where(predicates.toArray(Predicate[]::new)));
        return closing(paginateQuery(typedQuery, first, max).getResultStream());
    }

    @Override
    public long count() {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<IdentityProviderEntity> idp = query.from(IdentityProviderEntity.class);
        query.select(builder.count(query.from(IdentityProviderEntity.class)));
        query.where(builder.equal(idp.get("realmId"), getRealm().getId()));
        return em.createQuery(query).getSingleResult();
    }

    @Override
    public void close() {
    }

    @Override
    public IdentityProviderMapperModel createMapper(IdentityProviderMapperModel model) {
        checkUniqueMapperNamePerIdentityProvider(model);

        IdentityProviderMapperEntity entity = new IdentityProviderMapperEntity();
        entity.setId(model.getId() == null ? KeycloakModelUtils.generateId() : model.getId());
        entity.setName(model.getName());
        entity.setIdentityProviderAlias(model.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setRealmId(getRealm().getId());
        entity.setConfig(model.getConfig());

        em.persist(entity);
        model.setId(entity.getId());

        return model;
    }

    @Override
    public void updateMapper(IdentityProviderMapperModel model) {
        IdentityProviderMapperEntity entity = getMapperEntityById(model.getId(), true);
        if (!model.getName().equals(entity.getName())) {
            checkUniqueMapperNamePerIdentityProvider(model);
        }

        entity.setName(model.getName());
        entity.setIdentityProviderAlias(model.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setConfig(model.getConfig());
    }

    @Override
    public boolean removeMapper(IdentityProviderMapperModel model) {
        em.remove(getMapperEntityById(model.getId(), true));
        return true;
    }

    @Override
    public void removeAllMappers() {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaDelete<IdentityProviderMapperEntity> delete = builder.createCriteriaDelete(IdentityProviderMapperEntity.class);
        Root<IdentityProviderMapperEntity> mapper = delete.from(IdentityProviderMapperEntity.class);
        delete.where(builder.equal(mapper.get("realmId"), getRealm().getId()));
        em.createQuery(delete).executeUpdate();
    }

    @Override
    public IdentityProviderMapperModel getMapperById(String id) {
        return toModel(getMapperEntityById(id, false));
    }

    @Override
    public IdentityProviderMapperModel getMapperByName(String identityProviderAlias, String name) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<IdentityProviderMapperEntity> query = builder.createQuery(IdentityProviderMapperEntity.class);
        Root<IdentityProviderMapperEntity> mapper = query.from(IdentityProviderMapperEntity.class);

        Predicate predicate = builder.and(
                builder.equal(mapper.get("realmId"), getRealm().getId()),
                builder.equal(mapper.get("identityProviderAlias"), identityProviderAlias),
                builder.equal(mapper.get("name"), name));

        TypedQuery<IdentityProviderMapperEntity> typedQuery = em.createQuery(query.select(mapper).where(predicate));
        try {
            return toModel(typedQuery.getSingleResult());
        } catch (NoResultException nre) {
            return null;
        }
    }

    @Override
    public Stream<IdentityProviderMapperModel> getMappersStream(Map<String, String> options, Integer first, Integer max) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<IdentityProviderMapperEntity> query = builder.createQuery(IdentityProviderMapperEntity.class);
        Root<IdentityProviderMapperEntity> idp = query.from(IdentityProviderMapperEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(idp.get("realmId"), getRealm().getId()));

        if (options != null) {
            for (Map.Entry<String, String> entry : options.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (StringUtil.isBlank(key)) {
                    continue;
                }
                String dbProductName = em.unwrap(Session.class).doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());
                MapJoin<IdentityProviderMapperEntity, String, String> configJoin = idp.joinMap("config");
                Predicate configNamePredicate = builder.equal(configJoin.key(), key);

                if (dbProductName.equals("Oracle")) {
                    // Oracle is not able to compare a CLOB with a VARCHAR unless it being converted with TO_CHAR
                    // But for this all values in the table need to be smaller than 4K, otherwise the cast will fail with
                    // "ORA-22835: Buffer too small for CLOB to CHAR" (even if it is in another row).
                    // This leaves DBMS_LOB.COMPARE and DBMS_LOB.INSTR as the options to compare the CLOB with the value.
                    if (value.endsWith("*")) {
                        // prefix search - use DBMS_LOB.INSTR
                        value = value.substring(0, value.length() - 1);
                        Predicate configValuePredicate = builder.equal(builder.function("DBMS_LOB.INSTR", Integer.class, configJoin.value(), builder.literal(value)), 1);
                        predicates.add(builder.and(configNamePredicate, configValuePredicate));
                    } else {
                        Predicate configValuePredicate = builder.equal(builder.function("DBMS_LOB.COMPARE", Integer.class, configJoin.value(), builder.literal(value)), 0);
                        predicates.add(builder.and(configNamePredicate, configValuePredicate));
                    }
                } else {
                    if (value.endsWith("*")) {
                        value = value.replace("%", "\\%").replace("_", "\\_").replace("*", "%");
                        predicates.add(builder.and(configNamePredicate, builder.like(configJoin.value(), value)));
                    } else {
                        predicates.add(builder.and(configNamePredicate, builder.equal(configJoin.value(), value)));
                    }
                }
            }
        }

        TypedQuery<IdentityProviderMapperEntity> typedQuery = em.createQuery(query.select(idp).where(predicates.toArray(Predicate[]::new)));
        return closing(paginateQuery(typedQuery, first, max).getResultStream()).map(this::toModel);
    }

    @Override
    public Stream<IdentityProviderMapperModel> getMappersByAliasStream(String identityProviderAlias) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<IdentityProviderMapperEntity> query = builder.createQuery(IdentityProviderMapperEntity.class);
        Root<IdentityProviderMapperEntity> mapper = query.from(IdentityProviderMapperEntity.class);

        Predicate predicate = builder.and(
                builder.equal(mapper.get("realmId"), getRealm().getId()),
                builder.equal(mapper.get("identityProviderAlias"), identityProviderAlias));

        TypedQuery<IdentityProviderMapperEntity> typedQuery = em.createQuery(query.select(mapper).where(predicate).orderBy(builder.asc(mapper.get("id"))));

        return closing(typedQuery.getResultStream().map(this::toModel));
    }

    private IdentityProviderEntity getEntityById(String id, boolean failIfNotFound) {
        if (id == null) {
            if (failIfNotFound) {
                throw new ModelException("Identity Provider with null internal id does not exist");
            }
            return null;
        }

        IdentityProviderEntity entity = em.find(IdentityProviderEntity.class, id);
        if (entity == null) {
            if (failIfNotFound) {
                throw new ModelException("Identity Provider with internal id [" + id + "] does not exist");
            }
            return null;
        }

        // check realm to ensure this entity is fetched in the context of the correct realm.
        if (!this.getRealm().getId().equals(entity.getRealmId())) {
            throw new ModelException("Identity Provider with internal id [" + entity.getInternalId() + "] does not belong to realm [" + getRealm().getName() + "]");
        }
        return entity;
    }

    private IdentityProviderEntity getEntityByAlias(String alias) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<IdentityProviderEntity> query = builder.createQuery(IdentityProviderEntity.class);
        Root<IdentityProviderEntity> idp = query.from(IdentityProviderEntity.class);

        Predicate predicate = builder.and(builder.equal(idp.get("realmId"), getRealm().getId()),
                builder.equal(idp.get(ALIAS), alias));

        TypedQuery<IdentityProviderEntity> typedQuery = em.createQuery(query.select(idp).where(predicate));
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    private Predicate getAliasSearchPredicate(String search, CriteriaBuilder builder, Root<IdentityProviderEntity> idp) {
        if (search.startsWith("\"") && search.endsWith("\"")) {
            // exact search - alias must be an exact match
            search = search.substring(1, search.length() - 1);
            return builder.or(builder.equal(idp.get(ALIAS), search),builder.equal(idp.get(DISPLAY_NAME), search));
        } else {
            search = search.replace("%", "\\%").replace("_", "\\_").replace("*", "%");
            if (!search.endsWith("%")) {
                search += "%"; // default to prefix search
            }
            return builder.or(builder.like(builder.lower(idp.get(ALIAS)), search.toLowerCase(), '\\'),builder.like(builder.lower(idp.get(DISPLAY_NAME)), search.toLowerCase(), '\\'));
        }
    }

    private IdentityProviderModel toModel(IdentityProviderEntity entity) {
        if (entity == null) {
            return null;
        }

        IdentityProviderModel identityProviderModel = getModelFromProviderFactory(entity.getProviderId());
        identityProviderModel.setProviderId(entity.getProviderId());
        identityProviderModel.setAlias(entity.getAlias());
        identityProviderModel.setDisplayName(entity.getDisplayName());
        identityProviderModel.setInternalId(entity.getInternalId());
        Map<String, String> config = new HashMap<>(entity.getConfig());
        identityProviderModel.setConfig(config);
        identityProviderModel.setEnabled(entity.isEnabled());
        identityProviderModel.setLinkOnly(entity.isLinkOnly());
        identityProviderModel.setHideOnLogin(entity.isHideOnLogin());
        identityProviderModel.setTrustEmail(entity.isTrustEmail());
        identityProviderModel.setAuthenticateByDefault(entity.isAuthenticateByDefault());
        identityProviderModel.setFirstBrokerLoginFlowId(entity.getFirstBrokerLoginFlowId());
        identityProviderModel.setPostBrokerLoginFlowId(entity.getPostBrokerLoginFlowId());
        identityProviderModel.setOrganizationId(entity.getOrganizationId());
        identityProviderModel.setStoreToken(entity.isStoreToken());
        identityProviderModel.setAddReadTokenRoleOnCreate(entity.isAddReadTokenRoleOnCreate());

        return identityProviderModel;
    }

    private IdentityProviderModel getModelFromProviderFactory(String providerId) {

        IdentityProviderFactory factory = (IdentityProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(IdentityProvider.class, providerId);
        if (factory == null) {
            factory = (IdentityProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(SocialIdentityProvider.class, providerId);
        }

        if (factory != null) {
            return factory.createConfig();
        } else {
            logger.warn("Couldn't find a suitable identity provider factory for " + providerId);
            return new IdentityProviderModel();
        }
    }

    private void checkUniqueMapperNamePerIdentityProvider(IdentityProviderMapperModel model) {
        if (session.identityProviders().getMapperByName(model.getIdentityProviderAlias(), model.getName()) != null) {
            throw new ModelException("Identity provider mapper name must be unique per identity provider");
        }
    }

    private IdentityProviderMapperEntity getMapperEntityById(String id, boolean failIfNotFound) {
        IdentityProviderMapperEntity entity = em.find(IdentityProviderMapperEntity.class, id);

        if (failIfNotFound && entity == null) {
            throw new ModelException("Identity Provider Mapper with id [" + id + "] does not exist");
        }

        if (entity == null) return null;

        // check realm to ensure this entity is fetched in the context of the correct realm.
        if (!getRealm().getId().equals(entity.getRealmId())) {
            throw new ModelException("Identity Provider Mapper with id [" + entity.getId() + "] does not belong to realm [" + getRealm().getName() + "]");
        }

        return entity;
    }

    private IdentityProviderMapperModel toModel(IdentityProviderMapperEntity entity) {
        if (entity == null) return null;

        IdentityProviderMapperModel mapping = new IdentityProviderMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setIdentityProviderAlias(entity.getIdentityProviderAlias());
        mapping.setIdentityProviderMapper(entity.getIdentityProviderMapper());
        Map<String, String> config = entity.getConfig() == null ? new HashMap<>() : new HashMap<>(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }

    private RealmModel getRealm() {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            throw new IllegalStateException("Session not bound to a realm");
        }
        return realm;
    }
}
