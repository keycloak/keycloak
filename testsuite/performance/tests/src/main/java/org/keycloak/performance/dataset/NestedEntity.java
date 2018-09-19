package org.keycloak.performance.dataset;

import org.apache.commons.lang.Validate;
import static org.keycloak.performance.iteration.RandomBooleans.getRandomBooleans;
import static org.keycloak.performance.iteration.RandomIntegers.getRandomIntegers;
import org.keycloak.performance.util.ValidateNumber;

/**
 *
 * @author tkyjovsk
 * @param <PE> parent entity
 */
public abstract class NestedEntity<PE extends Entity, REP> extends Entity<REP> {

    private final PE parentEntity;

    private final int index;
    private final int seed;

    public NestedEntity(PE parentEntity, int index) {
        Validate.notNull(parentEntity);
        this.parentEntity = parentEntity;
        ValidateNumber.minValue(index, 0);
        this.index = index;
        this.seed = parentEntity.hashCode() + simpleClassName().hashCode();
    }

    public NestedEntity(PE parentEntity) {
        this(parentEntity, 0);
    }

    public PE getParentEntity() {
        return parentEntity;
    }

    public synchronized final int getIndex() {
        return index;
    }

    public synchronized int getSeed() {
        return seed;
    }

    @Override
    public synchronized int hashCode() {
        return simpleClassName().hashCode() * getIndex() + getParentEntity().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    public synchronized int indexBasedRandomInt(int bound) {
        return getRandomIntegers(getSeed(), bound).get(getIndex());
    }

    public synchronized boolean indexBasedRandomBool(int truePercentage) {
        return getRandomBooleans(getSeed(), truePercentage).get(getIndex());
    }

    public synchronized boolean indexBasedRandomBool() {
        return indexBasedRandomBool(50);
    }

}
