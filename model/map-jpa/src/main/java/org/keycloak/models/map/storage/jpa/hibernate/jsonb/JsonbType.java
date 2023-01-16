/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.hibernate.jsonb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.DynamicParameterizedType;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntityImpl;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityWithAttributes;
import org.keycloak.models.map.common.Serialization.IgnoreUpdatedMixIn;
import org.keycloak.models.map.common.Serialization.IgnoredTypeMixIn;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationExecutionEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationExecutionEntityImpl;
import org.keycloak.models.map.realm.entity.MapAuthenticationFlowEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationFlowEntityImpl;
import org.keycloak.models.map.realm.entity.MapAuthenticatorConfigEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticatorConfigEntityImpl;
import org.keycloak.models.map.realm.entity.MapClientInitialAccessEntity;
import org.keycloak.models.map.realm.entity.MapClientInitialAccessEntityImpl;
import org.keycloak.models.map.realm.entity.MapIdentityProviderEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderEntityImpl;
import org.keycloak.models.map.realm.entity.MapIdentityProviderMapperEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderMapperEntityImpl;
import org.keycloak.models.map.realm.entity.MapOTPPolicyEntity;
import org.keycloak.models.map.realm.entity.MapOTPPolicyEntityImpl;
import org.keycloak.models.map.realm.entity.MapRequiredActionProviderEntity;
import org.keycloak.models.map.realm.entity.MapRequiredActionProviderEntityImpl;
import org.keycloak.models.map.realm.entity.MapRequiredCredentialEntity;
import org.keycloak.models.map.realm.entity.MapRequiredCredentialEntityImpl;
import org.keycloak.models.map.realm.entity.MapWebAuthnPolicyEntity;
import org.keycloak.models.map.realm.entity.MapWebAuthnPolicyEntityImpl;
import org.keycloak.models.map.user.MapUserCredentialEntity;
import org.keycloak.models.map.user.MapUserCredentialEntityImpl;
import org.keycloak.util.EnumWithStableIndex;
import java.util.function.BiConsumer;
import org.hibernate.usertype.BaseUserTypeSupport;

public class JsonbType extends BaseUserTypeSupport<Object> implements DynamicParameterizedType {

    public static final JsonbType INSTANCE = new JsonbType();
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .activateDefaultTyping(new LaissezFaireSubTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY)
            .registerModule(new SimpleModule().addAbstractTypeMapping(MapProtocolMapperEntity.class, MapProtocolMapperEntityImpl.class)
                    // realm abstract type mappings
                    .addAbstractTypeMapping(MapAuthenticationExecutionEntity.class, MapAuthenticationExecutionEntityImpl.class)
                    .addAbstractTypeMapping(MapAuthenticationFlowEntity.class, MapAuthenticationFlowEntityImpl.class)
                    .addAbstractTypeMapping(MapAuthenticatorConfigEntity.class, MapAuthenticatorConfigEntityImpl.class)
                    .addAbstractTypeMapping(MapClientInitialAccessEntity.class, MapClientInitialAccessEntityImpl.class)
                    .addAbstractTypeMapping(MapIdentityProviderEntity.class, MapIdentityProviderEntityImpl.class)
                    .addAbstractTypeMapping(MapIdentityProviderMapperEntity.class, MapIdentityProviderMapperEntityImpl.class)
                    .addAbstractTypeMapping(MapOTPPolicyEntity.class, MapOTPPolicyEntityImpl.class)
                    .addAbstractTypeMapping(MapRequiredActionProviderEntity.class, MapRequiredActionProviderEntityImpl.class)
                    .addAbstractTypeMapping(MapRequiredCredentialEntity.class, MapRequiredCredentialEntityImpl.class)
                    .addAbstractTypeMapping(MapWebAuthnPolicyEntity.class, MapWebAuthnPolicyEntityImpl.class)
                    // user abstract type mappings
                    .addAbstractTypeMapping(MapUserCredentialEntity.class, MapUserCredentialEntityImpl.class))
            .addMixIn(UpdatableEntity.class, IgnoreUpdatedMixIn.class)
            .addMixIn(DeepCloner.class, IgnoredTypeMixIn.class)
            .addMixIn(EntityWithAttributes.class, IgnoredMetadataFieldsMixIn.class)
            .addMixIn(EnumWithStableIndex.class, EnumsMixIn.class)
            ;
    abstract class IgnoredMetadataFieldsMixIn {
        @JsonIgnore public abstract String getId();
        @JsonIgnore public abstract Map<String, List<String>> getAttributes();

        // roles: assumed it's true when getClient() != null, see AbstractRoleEntity.isClientRole()
        @JsonIgnore public abstract Boolean isClientRole();
    }

    abstract static class EnumsMixIn implements EnumWithStableIndex {

        // we convert enums to its index and vice versa
        @Override
        @JsonValue public abstract int getStableIndex();
    }

    private Class valueType;

	@Override
    @SuppressWarnings("unchecked")
	protected void resolve(BiConsumer resolutionConsumer) {
		resolutionConsumer.accept(new JsonbJavaTypeDescriptor(), JsonbSqlTypeDescriptor.INSTANCE);
	}

    @Override
    public void setParameterValues(Properties parameters) {
        this.valueType = ((ParameterType) parameters.get(PARAMETER_TYPE)).getReturnedClass();
    }

    private static class JsonbSqlTypeDescriptor implements JdbcType {

        private static final JsonbSqlTypeDescriptor INSTANCE = new JsonbSqlTypeDescriptor();

        @Override
        public int getJdbcTypeCode() {
            return Types.OTHER;
        }

        @Override
        public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
            return new BasicBinder<X>(javaTypeDescriptor, this) {
                @Override
                protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                    st.setObject(index, javaTypeDescriptor.unwrap(value, JsonNode.class, options), getJdbcTypeCode());
                }

                @Override
                protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
                    st.setObject(name, javaTypeDescriptor.unwrap(value, JsonNode.class, options), getJdbcTypeCode());
                }
            };
        }

        @Override
        public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
            return new BasicExtractor<X>(javaTypeDescriptor, this) {
                @Override
                protected X doExtract(ResultSet rs, int index, WrapperOptions options) throws SQLException {
                    return javaTypeDescriptor.wrap(extractJson(rs, index), options);
                }

                @Override
                protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                    return javaTypeDescriptor.wrap(extractJson(statement, index), options);
                }

                @Override
                protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                    return javaTypeDescriptor.wrap(extractJson(statement, name), options);
                }
            };
        }

        private Object extractJson(ResultSet rs, int index) throws SQLException {
            return rs.getObject(index);
        }

        private Object extractJson(CallableStatement statement, int index) throws SQLException {
            return statement.getObject(index);
        }

        private Object extractJson(CallableStatement statement, String name) throws SQLException {
            return statement.getObject(name);
        }
    }

    private class JsonbJavaTypeDescriptor extends AbstractJavaType<Object>  {

        public JsonbJavaTypeDescriptor() {
            super(Object.class, new MutableMutabilityPlan<Object>() {
                @Override
                protected Object deepCopyNotNull(Object value) {
                    try {
                        return MAPPER.readValue(MAPPER.writerFor(value.getClass()).writeValueAsBytes(value), value.getClass());
                    } catch (IOException e) {
                        throw new HibernateException("unable to deep copy object", e);
                    }
                }
            });
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object fromString(CharSequence json) {
            try {
                ObjectNode tree = MAPPER.readValue(json.toString(), ObjectNode.class);
                JsonNode ev = tree.get("entityVersion");
                if (ev == null || ! ev.isInt()) throw new IllegalArgumentException("unable to read entity version from " + json);

                Integer entityVersion = ev.asInt();

                tree = migrate(tree, entityVersion);

                return MAPPER.treeToValue(tree, valueType);
            } catch (IOException e) {
                throw new HibernateException("unable to read", e);
            }
        }

        private ObjectNode migrate(ObjectNode tree, Integer entityVersion) {
            return JpaEntityMigration.MIGRATIONS.getOrDefault(valueType, (node, version) -> node).apply(tree, entityVersion);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <X> X unwrap(Object value, Class<X> type, WrapperOptions options) {
            if (value == null) return null;

            String stringValue = (value instanceof String) ? (String) value : toString(value);
            try {
                return (X) MAPPER.readTree(stringValue);
            } catch (IOException e) {
                throw new HibernateException("unable to read", e);
            }
        }

        @Override
        public <X> Object wrap(X value, WrapperOptions options) {
            if (value == null) return null;

            return fromString(value.toString());
        }

        @Override
        public String toString(Object value) {
            try {
                return MAPPER.writeValueAsString(value);
            } catch (IOException e) {
                throw new HibernateException("unable to transform value: " + value + " as String.", e);
            }
        }

        @Override
        public boolean areEqual(Object one, Object another) {
            if (one == another) return true;
            return Objects.equals(one, another);
        }
    }
}
