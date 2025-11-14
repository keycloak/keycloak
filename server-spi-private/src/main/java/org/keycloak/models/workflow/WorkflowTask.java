package org.keycloak.models.workflow;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.common.util.Time;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.utils.KeycloakModelUtils;

public final class WorkflowTask extends AbstractKeycloakTransaction implements Runnable {

    private final Runnable task;
    private final WorkflowExecutor executor;
    private final String id;
    private CompletableFuture<Void> future;
    private long startTime;
    private AtomicReference<Thread> thread;

    WorkflowTask(WorkflowExecutor executor, Runnable task) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(task, "task");
        this.executor = executor;
        this.task = task;
        id = KeycloakModelUtils.generateId();
    }

    @Override
    public void run() {
        if (thread.compareAndSet(null, Thread.currentThread())) {
            task.run();
        }
    }

    @Override
    protected void commitImpl() {
        startTime = Time.currentTimeMillis();
        thread = new AtomicReference<>();
        future = executor.submit(this);
    }

    @Override
    protected void rollbackImpl() {
        future = CompletableFuture.failedFuture(new RuntimeException("Parent transaction rolled back"));
    }

    @Override
    public boolean isActive() {
        return future != null && !future.isDone();
    }

    public boolean isScheduled() {
        return future == null;
    }

    public boolean isDone() {
        return future != null && future.isDone();
    }

    public boolean isCompletedExceptionally() {
        return future != null && future.isCompletedExceptionally();
    }

    public Throwable getThrowable() {
        if (future != null && future.isCompletedExceptionally()) {
            try {
                future.join();
            } catch (Throwable t) {
                return t;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String status = isScheduled() ? "SCHEDULED" : isActive() ? "ACTIVE" : isCompletedExceptionally() ? "FAILED" : "SUCCESS";
        return "id: " + id + ", executionTime: " + (System.currentTimeMillis() - startTime) + "ms , status: " + status + ", task: [" + task.toString() + "]";
    }

    public void cancel() {
        if (future != null) {
            future.cancel(true);
            if (thread.get() != null) {
                thread.get().interrupt();
            }
        }
    }
}
