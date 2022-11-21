package org.keycloak.rule;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Runs every test method in it's own thread. Useful for example for
 * testing bouncycastle FIPS (BCFIPS cannot switch bouncycastle to non-approved mode after it was switched before in approved mode in the same thread)
 *
 * Copy/paste from https://www.codeaffine.com/2014/07/21/a-junit-rule-to-run-a-test-in-its-own-thread/
 *
 * RunInThread an other accompanying files are licensed under the MIT
 * license.  Copyright (C) Frank Appel 2016-2021. All rights reserved
 */
public class RunInThreadRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description ) {
        Statement result = base;
        result = new RunInThreadStatement( base );
        return result;
    }


    private static class RunInThreadStatement extends Statement {

        private final Statement baseStatement;
        private Future<?> future;
        private volatile Throwable throwable;

        RunInThreadStatement( Statement baseStatement ) {
            this.baseStatement = baseStatement;
        }

        @Override
        public void evaluate() throws Throwable {
            ExecutorService executorService = runInThread();
            try {
                waitTillFinished();
            } finally {
                executorService.shutdown();
            }
            rethrowAssertionsAndErrors();
        }

        private ExecutorService runInThread() {
            ExecutorService result = Executors.newSingleThreadExecutor();
            future = result.submit( new Runnable() {
                @Override
                public void run() {
                    try {
                        baseStatement.evaluate();
                    } catch( Throwable throwable ) {
                        RunInThreadStatement.this.throwable = throwable;
                    }
                }
            } );
            return result;
        }

        private void waitTillFinished() {
            try {
                future.get();
            } catch (ExecutionException shouldNotHappen ) {
                throw new IllegalStateException( shouldNotHappen );
            } catch( InterruptedException shouldNotHappen ) {
                throw new IllegalStateException( shouldNotHappen );
            }
        }

        private void rethrowAssertionsAndErrors() throws Throwable {
            if( throwable != null ) {
                throw throwable;
            }
        }
    }
}
