/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
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

package org.keycloak.models.redis.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.session.AbstractRedisPersistenceTransaction;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AbstractRedisPersistenceTransaction.
 * Tests the common transaction lifecycle management for Redis-backed session adapters.
 */
class AbstractRedisPersistenceTransactionTest {

    private AtomicBoolean modifiedFlag;
    private AtomicInteger persistCallCount;
    private Consumer<Boolean> modifiedFlagSetter;
    private BooleanSupplier modifiedFlagGetter;
    private TestRedisPersistenceTransaction transaction;

    /**
     * Concrete implementation of AbstractRedisPersistenceTransaction for testing.
     */
    private class TestRedisPersistenceTransaction extends AbstractRedisPersistenceTransaction {

        public TestRedisPersistenceTransaction(Consumer<Boolean> modifiedFlagSetter,
                                               BooleanSupplier modifiedFlagGetter) {
            super(modifiedFlagSetter, modifiedFlagGetter);
        }

        @Override
        protected void persist() {
            persistCallCount.incrementAndGet();
        }
    }

    @BeforeEach
    void setUp() {
        modifiedFlag = new AtomicBoolean(false);
        persistCallCount = new AtomicInteger(0);
        modifiedFlagSetter = modifiedFlag::set;
        modifiedFlagGetter = modifiedFlag::get;
        transaction = new TestRedisPersistenceTransaction(modifiedFlagSetter, modifiedFlagGetter);
    }

    @Test
    void testBegin_shouldDoNothing() {
        // When
        transaction.begin();

        // Then - no exception thrown, no side effects
        assertThat(modifiedFlag.get()).isFalse();
        assertThat(persistCallCount.get()).isZero();
    }

    @Test
    void testCommit_shouldCallPersist() {
        // Given
        modifiedFlag.set(true);

        // When
        transaction.commit();

        // Then
        assertThat(persistCallCount.get()).isEqualTo(1);
    }

    @Test
    void testCommit_whenCalledMultipleTimes_shouldCallPersistMultipleTimes() {
        // Given
        modifiedFlag.set(true);

        // When
        transaction.commit();
        transaction.commit();
        transaction.commit();

        // Then
        assertThat(persistCallCount.get()).isEqualTo(3);
    }

    @Test
    void testCommit_whenNotModified_shouldStillCallPersist() {
        // Given
        modifiedFlag.set(false);

        // When
        transaction.commit();

        // Then - persist() is always called on commit, regardless of modified flag
        // The persist() implementation is responsible for checking the flag
        assertThat(persistCallCount.get()).isEqualTo(1);
    }

    @Test
    void testRollback_shouldResetModifiedFlag() {
        // Given
        modifiedFlag.set(true);

        // When
        transaction.rollback();

        // Then
        assertThat(modifiedFlag.get()).isFalse();
        assertThat(persistCallCount.get()).isZero(); // persist should not be called
    }

    @Test
    void testRollback_whenNotModified_shouldStillSetFlagToFalse() {
        // Given
        modifiedFlag.set(false);

        // When
        transaction.rollback();

        // Then
        assertThat(modifiedFlag.get()).isFalse();
        assertThat(persistCallCount.get()).isZero();
    }

    @Test
    void testRollback_afterCommit_shouldResetFlag() {
        // Given
        modifiedFlag.set(true);
        transaction.commit();

        // When
        modifiedFlag.set(true); // Simulate modification after commit
        transaction.rollback();

        // Then
        assertThat(modifiedFlag.get()).isFalse();
    }

    @Test
    void testSetRollbackOnly_shouldDoNothing() {
        // Given
        modifiedFlag.set(true);

        // When
        transaction.setRollbackOnly();

        // Then - no exception thrown, no side effects
        assertThat(modifiedFlag.get()).isTrue();
        assertThat(persistCallCount.get()).isZero();
    }

    @Test
    void testGetRollbackOnly_shouldAlwaysReturnFalse() {
        // When/Then
        assertThat(transaction.getRollbackOnly()).isFalse();

        // Even after setRollbackOnly
        transaction.setRollbackOnly();
        assertThat(transaction.getRollbackOnly()).isFalse();

        // Even after commit
        transaction.commit();
        assertThat(transaction.getRollbackOnly()).isFalse();

        // Even after rollback
        transaction.rollback();
        assertThat(transaction.getRollbackOnly()).isFalse();
    }

    @Test
    void testIsActive_whenModified_shouldReturnTrue() {
        // Given
        modifiedFlag.set(true);

        // When/Then
        assertThat(transaction.isActive()).isTrue();
    }

    @Test
    void testIsActive_whenNotModified_shouldReturnFalse() {
        // Given
        modifiedFlag.set(false);

        // When/Then
        assertThat(transaction.isActive()).isFalse();
    }

    @Test
    void testIsActive_afterRollback_shouldReturnFalse() {
        // Given
        modifiedFlag.set(true);
        assertThat(transaction.isActive()).isTrue();

        // When
        transaction.rollback();

        // Then
        assertThat(transaction.isActive()).isFalse();
    }

    @Test
    void testIsActive_reflectsCurrentModifiedState() {
        // Initially not modified
        assertThat(transaction.isActive()).isFalse();

        // Mark as modified
        modifiedFlag.set(true);
        assertThat(transaction.isActive()).isTrue();

        // Rollback resets flag
        transaction.rollback();
        assertThat(transaction.isActive()).isFalse();

        // Modify again
        modifiedFlag.set(true);
        assertThat(transaction.isActive()).isTrue();
    }

    @Test
    void testTransactionLifecycle_commitScenario() {
        // Given - simulate typical transaction lifecycle
        assertThat(transaction.isActive()).isFalse();

        // Begin transaction
        transaction.begin();
        assertThat(transaction.isActive()).isFalse(); // Still not modified

        // Make modifications
        modifiedFlag.set(true);
        assertThat(transaction.isActive()).isTrue();

        // Commit
        transaction.commit();
        assertThat(persistCallCount.get()).isEqualTo(1);
    }

    @Test
    void testTransactionLifecycle_rollbackScenario() {
        // Given - simulate transaction that gets rolled back
        transaction.begin();
        modifiedFlag.set(true);
        assertThat(transaction.isActive()).isTrue();

        // Rollback
        transaction.rollback();

        // Then
        assertThat(transaction.isActive()).isFalse();
        assertThat(modifiedFlag.get()).isFalse();
        assertThat(persistCallCount.get()).isZero(); // persist not called
    }

    @Test
    void testMultipleTransactions_independent() {
        // Create two independent transactions with different flags
        AtomicBoolean flag1 = new AtomicBoolean(false);
        AtomicBoolean flag2 = new AtomicBoolean(false);
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);

        TestRedisPersistenceTransaction tx1 = new TestRedisPersistenceTransaction(
                flag1::set, flag1::get) {
            @Override
            protected void persist() {
                count1.incrementAndGet();
            }
        };

        TestRedisPersistenceTransaction tx2 = new TestRedisPersistenceTransaction(
                flag2::set, flag2::get) {
            @Override
            protected void persist() {
                count2.incrementAndGet();
            }
        };

        // Modify and commit tx1
        flag1.set(true);
        tx1.commit();
        assertThat(count1.get()).isEqualTo(1);
        assertThat(count2.get()).isZero();

        // Modify and rollback tx2
        flag2.set(true);
        tx2.rollback();
        assertThat(flag2.get()).isFalse();
        assertThat(count2.get()).isZero();

        // tx1 should be unaffected
        assertThat(flag1.get()).isTrue();
        assertThat(count1.get()).isEqualTo(1);
    }

    @Test
    void testPersist_canThrowException() {
        // Given - transaction that throws exception in persist
        RuntimeException testException = new RuntimeException("Persistence failed");
        TestRedisPersistenceTransaction failingTransaction = new TestRedisPersistenceTransaction(
                modifiedFlagSetter, modifiedFlagGetter) {
            @Override
            protected void persist() {
                throw testException;
            }
        };

        // When/Then - exception should propagate from commit
        assertThatThrownBy(failingTransaction::commit)
                .isEqualTo(testException);
    }

    @Test
    void testModifiedFlagSetter_calledOnRollback() {
        // Given
        AtomicInteger setterCallCount = new AtomicInteger(0);
        Consumer<Boolean> countingSetter = flag -> {
            setterCallCount.incrementAndGet();
            modifiedFlag.set(flag);
        };

        TestRedisPersistenceTransaction tx = new TestRedisPersistenceTransaction(
                countingSetter, modifiedFlagGetter);

        modifiedFlag.set(true);

        // When
        tx.rollback();

        // Then
        assertThat(setterCallCount.get()).isEqualTo(1);
        assertThat(modifiedFlag.get()).isFalse();
    }

    @Test
    void testModifiedFlagGetter_calledOnIsActive() {
        // Given
        AtomicInteger getterCallCount = new AtomicInteger(0);
        BooleanSupplier countingGetter = () -> {
            getterCallCount.incrementAndGet();
            return modifiedFlag.get();
        };

        TestRedisPersistenceTransaction tx = new TestRedisPersistenceTransaction(
                modifiedFlagSetter, countingGetter);

        // When
        tx.isActive();
        tx.isActive();
        tx.isActive();

        // Then
        assertThat(getterCallCount.get()).isEqualTo(3);
    }

    @Test
    void testNullModifiedFlagSetter_shouldThrowNPE() {
        // When/Then - NPE should occur when rollback tries to use null setter
        TestRedisPersistenceTransaction tx = new TestRedisPersistenceTransaction(
                null, modifiedFlagGetter);

        assertThatThrownBy(tx::rollback)
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testNullModifiedFlagGetter_shouldThrowNPE() {
        // When/Then - NPE should occur when isActive tries to use null getter
        TestRedisPersistenceTransaction tx = new TestRedisPersistenceTransaction(
                modifiedFlagSetter, null);

        assertThatThrownBy(tx::isActive)
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testCommitAfterRollback_shouldCallPersist() {
        // Given
        modifiedFlag.set(true);
        transaction.rollback();
        assertThat(modifiedFlag.get()).isFalse();

        // When - modify again and commit
        modifiedFlag.set(true);
        transaction.commit();

        // Then
        assertThat(persistCallCount.get()).isEqualTo(1);
    }

    @Test
    void testRollbackAfterCommit_shouldNotAffectPersistCount() {
        // Given
        modifiedFlag.set(true);
        transaction.commit();
        int countAfterCommit = persistCallCount.get();

        // When
        modifiedFlag.set(true);
        transaction.rollback();

        // Then - rollback doesn't call persist
        assertThat(persistCallCount.get()).isEqualTo(countAfterCommit);
        assertThat(modifiedFlag.get()).isFalse();
    }
}