package org.keycloak.test.tools;

import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.test.tools.jobs.CreateUsersJob;
import org.keycloak.test.tools.jobs.DeleteUsersJob;
import org.keycloak.test.tools.jobs.UpdateUsersJob;
import org.keycloak.test.tools.jobs.UsersJob;
import org.keycloak.test.tools.jobs.UsersJobInitializer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("perf")
public class PerfTools {

    private ExecutorService executor = Executors.newFixedThreadPool(20);

    private final KeycloakSessionFactory sessionFactory;

    @Context
    private KeycloakSession session;

    private List<JobRepresentation> jobs = new LinkedList<JobRepresentation>();

    public PerfTools(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @GET
    @Path("jobs")
    @Produces("application/json")
    public List<JobRepresentation> jobs() {
        return jobs;
    }

    @GET
    @Path("delete-jobs")
    public void deleteJobs() {
        Iterator<JobRepresentation> itr = jobs.iterator();
        while(itr.hasNext()) {
            JobRepresentation j = itr.next();
            if (j.getError() != null || j.getCount() == j.getTotal()) {
                itr.remove();
            }
        }
    }

    @GET
    @Path("{realm}/create-users")
    public void createUsers(@PathParam("realm") String realmName, @QueryParam("count") Integer count,
                            @QueryParam("batch") Integer batch, @QueryParam("start") Integer start, @QueryParam("prefix") String prefix,
                            @QueryParam("async") Boolean async, @QueryParam("roles") String roles) throws InterruptedException {
        final String[] rolesArray = roles != null ? roles.split(",") : new String[0];

        createAndRunJob(realmName, count, batch, start, prefix, async, "Create users", new UsersJobInitializer() {

            @Override
            public UsersJob instantiateJob() {
                return new CreateUsersJob(rolesArray);
            }

        });
    }

    // Same as createUsers, but dynamically compute "start" (Next available user)
    @GET
    @Path("{realm}/create-available-users")
    public void createAvailableUsers(@PathParam("realm") String realmName, @QueryParam("count") Integer count,
                                     @QueryParam("batch") Integer batch, @QueryParam("prefix") String prefix,
                                     @QueryParam("async") Boolean async, @QueryParam("roles") String roles) throws InterruptedException {
        int start = getUsersCount(realmName, prefix);
        createUsers(realmName, count, batch, start, prefix, async, roles);
    }

    @GET
    @Path("{realm}/delete-users")
    public void deleteUsers(@PathParam("realm") String realmName, @QueryParam("count") Integer count,
                            @QueryParam("batch") Integer batch, @QueryParam("start") Integer start, @QueryParam("prefix") String prefix,
                            @QueryParam("async") Boolean async) throws InterruptedException {
        createAndRunJob(realmName, count, batch, start, prefix, async, "Delete users", new UsersJobInitializer() {

            @Override
            public UsersJob instantiateJob() {
                return new DeleteUsersJob();
            }

        });
    }

    @GET
    @Path("{realm}/delete-all-users")
    public void deleteUsers(@PathParam("realm") String realmName, @QueryParam("prefix") String prefix, @QueryParam("async") Boolean async) throws InterruptedException {
        int count = getUsersCount(realmName, prefix);
        if (count == 0) {
            return;
        }

        int batch = count / 10;
        if (batch == 0) {
            batch = 1;
        }

        deleteUsers(realmName, count, batch, 0, prefix, async);
    }

    @GET
    @Path("{realm}/update-users")
    public void updateUsers(@PathParam("realm") String realmName, @QueryParam("count") Integer count,
                            @QueryParam("batch") Integer batch, @QueryParam("start") Integer start, @QueryParam("prefix") String prefix,
                            @QueryParam("async") Boolean async, @QueryParam("roles") String roles) throws InterruptedException {
        final String[] rolesArray = roles != null ? roles.split(",") : new String[0];

        createAndRunJob(realmName, count, batch, start, prefix, async, "Update users", new UsersJobInitializer() {

            @Override
            public UsersJob instantiateJob() {
                return new UpdateUsersJob(rolesArray);
            }

        });
    }

    @GET
    @Path("{realm}/update-all-users")
    public void updateAllUsers(@PathParam("realm") String realmName, @QueryParam("prefix") String prefix, @QueryParam("async") Boolean async,
                               @QueryParam("roles") String roles) throws InterruptedException {
        int count = getUsersCount(realmName, prefix);
        if (count == 0) {
            return;
        }

        int batch = count / 10;
        if (batch == 0) {
            batch = 1;
        }

        updateUsers(realmName, count, batch, 0, prefix, async, roles);
    }


    @GET
    @Path("{realm}/get-users-count")
    public Response getUsersCountReq(@PathParam("realm") String realmName, @QueryParam("prefix") String prefix) {
        int usersCount = getUsersCount(realmName, prefix);
        return Response.ok(String.valueOf(usersCount)).build();
    }

    private int getUsersCount(String realmName, String prefix) {
        RealmModel realm = session.realms().getRealmByName(realmName);

        // TODO: method for count on model
        if (prefix == null) {
            return session.users().getUsers(realm).size();
        } else {
            return session.users().searchForUser(prefix, realm).size();
        }
    }

    private void createAndRunJob(String realmName, Integer count, Integer batch, Integer start, String prefix, Boolean async, String jobName, UsersJobInitializer initializer) throws InterruptedException {
        if (count == null) {
            count = 1;
        }
        if (batch == null) {
            batch = 1000;
        }
        if (start == null) {
            start = 0;
        }
        if (prefix == null) {
            prefix = String.valueOf(System.currentTimeMillis());
        }
        if (async == null) {
            async = true;
        }

        int executorsCount = count / batch;
        if (count % batch > 0) {
            executorsCount++;
        }
        CountDownLatch latch = new CountDownLatch(executorsCount);

        JobRepresentation job = new JobRepresentation(jobName + " " + prefix + "-" + start + " to " + prefix + "-" + (start + count), count);
        jobs.add(job);

        List<UsersJob> usersJobs = new ArrayList<UsersJob>();
        for (int s = start; s < (start + count); s += batch) {
            int c = s + batch <= (start + count) ? batch : (start + count) - s;
            UsersJob usersJob = initializer.instantiateJob();
            usersJob.init(job, sessionFactory, realmName, s, c, prefix, latch);
            usersJobs.add(usersJob);
        }

        // Run executors once all are initialized
        for (UsersJob usersJob : usersJobs) {
            executor.submit(usersJob);
        }

        if (!async) {
            latch.await();
        }
    }

    @GET
    @Path("export")
    public void export(@QueryParam("dir") String dir) {
        ExportImportConfig.setAction("export");
        ExportImportConfig.setProvider("dir");
        ExportImportConfig.setDir(dir);

        new ExportImportManager().checkExportImport(sessionFactory);
    }

    public static class JobRepresentation {
        private final String description;
        private final int total;
        private AtomicInteger count = new AtomicInteger();
        private String error;
        private AtomicLong started = new AtomicLong();
        private AtomicLong completed = new AtomicLong();

        public JobRepresentation(String description, int total) {
            this.description = description;
            this.total = total;
        }

        public Long getStarted() {
            long s = started.get();
            return s != 0 ? s : null;
        }

        public Long getCompleted() {
            long c = completed.get();
            return c != 0 ? c : null;
        }

        public String getDescription() {
            return description;
        }

        public int getTotal() {
            return total;
        }

        public int getCount() {
            return count.get();
        }

        public void start() {
            started.compareAndSet(0, System.currentTimeMillis());
        }

        public void increment() {
            if (count.incrementAndGet() == total) {
                completed.set(System.currentTimeMillis());
            }
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

}
