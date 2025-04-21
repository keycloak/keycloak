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

package org.keycloak.testsuite.model.infinispan;

import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.commons.TimeoutException;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.AsyncInterceptor;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.infinispan.interceptors.BaseCustomAsyncInterceptor;

import static org.junit.Assert.assertTrue;

/**
 * An Infinispan {@link AsyncInterceptor} that throws {@link TimeoutException} for several times before letting the
 * operation continue normally.
 */
public class TimeOutInterceptor extends BaseCustomAsyncInterceptor {

    public static final String MESSAGE = "Generated TimeOutException";

    private final AtomicInteger putCount = new AtomicInteger(0);
    private final AtomicInteger removeCount = new AtomicInteger(0);
    private final AtomicInteger replaceCheckCount = new AtomicInteger(0);
    private final AtomicInteger replaceCount = new AtomicInteger(0);

    public static TimeOutInterceptor getOrInject(Cache<?, ?> cache) {
        var interceptorChain = ComponentRegistry.componentOf(cache, AsyncInterceptorChain.class);
        var existing = interceptorChain.findInterceptorWithClass(TimeOutInterceptor.class);
        if (existing != null) {
            return existing;
        }
        var interceptor = new TimeOutInterceptor();
        interceptorChain.addInterceptor(interceptor, 0);
        return interceptor;
    }

    @Override
    public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) {
        timeOut(putCount);
        return invokeNext(ctx, command);
    }

    @Override
    public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) {
        timeOut(removeCount);
        return invokeNext(ctx, command);
    }

    @Override
    public Object visitReplaceCommand(InvocationContext ctx, ReplaceCommand command) {
        if (replaceCheckCount.decrementAndGet() >= 0) {
            return false;
        }
        timeOut(replaceCount);
        return invokeNext(ctx, command);
    }

    private static void timeOut(AtomicInteger counter) {
        if (counter.decrementAndGet() >= 0) {
            throw new TimeoutException(MESSAGE);
        }
    }

    private static void setCount(AtomicInteger counter, int value) {
        assertTrue("number of failures must be positive: %s".formatted(value), value > 0);
        counter.set(value);
    }

    private static void assertCountExhausted(AtomicInteger counter, String operation) {
        var pending = counter.get();
        assertTrue("Operation '%s' still has %s pending time out(s)".formatted(operation, pending), pending <= 0);
    }

    public void timeoutPuts(int numberOfTimeOuts) {
        setCount(putCount, numberOfTimeOuts);
    }

    public void timeoutRemove(int numberOfTimeOuts) {
        setCount(putCount, numberOfTimeOuts);
    }

    public void timeoutReplace(int numberOfTimeOuts) {
        setCount(replaceCount, numberOfTimeOuts);
    }

    public void replaceReturnsFalse(int numberOfCheckFails) {
        setCount(replaceCheckCount, numberOfCheckFails);
    }

    public void assertPutTimedOutExhausted() {
        assertCountExhausted(putCount, "put");
    }

    public void assertRemoveTimedOutExhausted() {
        assertCountExhausted(removeCount, "remove");
    }

    public void assertReplaceTimedOutExhausted() {
        assertCountExhausted(replaceCount, "replace");
    }

    public void assertReplaceCheckFailExhausted() {
        assertCountExhausted(replaceCheckCount, "replace (check)");
    }

    public void reset() {
        putCount.set(0);
        removeCount.set(0);
        replaceCount.set(0);
        replaceCheckCount.set(0);
    }
}
