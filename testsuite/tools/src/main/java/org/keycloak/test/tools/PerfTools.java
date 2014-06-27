package org.keycloak.test.tools;

import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.test.tools.jobs.CreateUsers;
import org.keycloak.util.ProviderLoader;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

    private List<Job> jobs = new LinkedList<Job>();

    public PerfTools(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @GET
    @Path("jobs")
    @Produces("application/json")
    public List<Job> jobs() {
        return jobs;
    }

    @GET
    @Path("delete-jobs")
    public void deleteJobs() {
        Iterator<Job> itr = jobs.iterator();
        while(itr.hasNext()) {
            Job j = itr.next();
            if (j.getError() != null || j.getCount() == j.getTotal()) {
                itr.remove();
            }
        }
    }

    @GET
    @Path("{realm}/create-users")
    public Response createUsers(@PathParam("realm") String realmName, @QueryParam("count") Integer count, @QueryParam("batch") Integer batch, @QueryParam("start") Integer start, @QueryParam("prefix") String prefix, @QueryParam("roles") String roles) throws InterruptedException {
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

        String[] rolesArray = roles != null ? roles.split(",") : new String[0];

        Job job = new Job("Create users " + prefix + "-" + start + " to " + prefix + "-" + (start + count), count);
        jobs.add(job);

        for (int s = start; s < (start + count); s += batch) {
            int c = s + batch <= (start + count) ? batch : (start + count) - s;
            executor.submit(new CreateUsers(job, sessionFactory, realmName, s, c, prefix, rolesArray));
        }

        return Response.noContent().build();
    }

    @GET
    @Path("{realm}/delete-users")
    public void deleteUsers(@PathParam("realm") String realmName) {
        RealmModel realm = session.getRealmByName(realmName);
        for (UserModel user : realm.getUsers()) {
            realm.removeUser(user.getLoginName());
        }
    }

    @GET
    @Path("export")
    public void export(@QueryParam("dir") String dir) {
        ExportImportConfig.setAction("export");
        ExportImportConfig.setProvider("dir");
        ExportImportConfig.setDir(dir);

        Iterator<ExportImportProvider> providers = ProviderLoader.load(ExportImportProvider.class).iterator();

        if (providers.hasNext()) {
            ExportImportProvider exportImport = providers.next();
            exportImport.checkExportImport(sessionFactory);
        } else {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public class Job {
        private final String description;
        private final int total;
        private AtomicInteger count = new AtomicInteger();
        private String error;
        private AtomicLong started = new AtomicLong();
        private AtomicLong completed = new AtomicLong();

        public Job(String description, int total) {
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
