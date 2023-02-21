package org.keycloak.models.map.common.delegate;


import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;

public interface EntityFieldDelegate<E> extends UpdatableEntity {

    public abstract class WithEntity<E extends UpdatableEntity> implements EntityFieldDelegate<E> {
        protected final E entity;

        public WithEntity(E entity) {
            this.entity = entity;
        }

        @Override
        public <EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object get(EF field) {
            return field.get(entity);
        }

        @Override
        public <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void set(EF field, T value) {
            field.set(entity, value);
        }

        @Override
        public <K, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<E>> & org.keycloak.models.map.common.EntityField<E>> Object mapRemove(EF field, K key) {
            return field.mapRemove(entity, key);
        }

        @Override
        public <K, T, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<E>> & org.keycloak.models.map.common.EntityField<E>> void mapPut(EF field, K key, T value) {
            field.mapPut(entity, key, value);
        }

        @Override
        public <K, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<E>> & org.keycloak.models.map.common.EntityField<E>> Object mapGet(EF field, K key) {
            return field.mapGet(entity, key);
        }

        @Override
        public <T, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<E>> & org.keycloak.models.map.common.EntityField<E>> Object collectionRemove(EF field, T value) {
            return field.collectionRemove(entity, value);
        }

        @Override
        public <T, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<E>> & org.keycloak.models.map.common.EntityField<E>> void collectionAdd(EF field, T value) {
            field.collectionAdd(entity, value);
        }

        @Override
        public boolean isUpdated() {
            return entity.isUpdated();
        }

        @Override
        public String toString() {
            return "&" + String.valueOf(entity);
        }
    }

    // Non-collection values
    <EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object get(EF field);
    <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void set(EF field, T value);

    <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void collectionAdd(EF field, T value);
    <T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object collectionRemove(EF field, T value);

    <K, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object mapGet(EF field, K key);
    <K, T, EF extends Enum<? extends EntityField<E>> & EntityField<E>> void mapPut(EF field, K key, T value);
    <K, EF extends Enum<? extends EntityField<E>> & EntityField<E>> Object mapRemove(EF field, K key);

}
