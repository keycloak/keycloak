package org.keycloak.audit.jpa;

import org.json.JSONObject;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.Event;
import org.keycloak.audit.EventQuery;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaAuditProvider implements AuditProvider {

    private EntityManager em;
    private EntityTransaction tx;

    public JpaAuditProvider(EntityManager em) {
        this.em = em;
    }

    @Override
    public EventQuery createQuery() {
        return new JpaEventQuery(em);
    }

    @Override
    public void clear() {
        beginTx();
        em.createQuery("delete from EventEntity").executeUpdate();
    }

    @Override
    public void clear(long olderThan) {
        beginTx();
        em.createQuery("delete from EventEntity where time < :time").setParameter("time", olderThan).executeUpdate();
    }

    @Override
    public void onEvent(Event event) {
        beginTx();
        em.persist(convert(event));
    }

    @Override
    public void close() {
        if (tx != null) {
            tx.commit();
        }

        em.close();
    }

    private void beginTx() {
        if (tx == null) {
            tx = em.getTransaction();
            tx.begin();
        }
    }

    static EventEntity convert(Event o) {
        EventEntity e = new EventEntity();
        e.setId(UUID.randomUUID().toString());
        e.setTime(o.getTime());
        e.setEvent(o.getEvent());
        e.setRealmId(o.getRealmId());
        e.setClientId(o.getClientId());
        e.setUserId(o.getUserId());
        e.setIpAddress(o.getIpAddress());
        e.setError(o.getError());
        e.setDetailsJson(new JSONObject(o.getDetails()).toString());
        return e;
    }

    static Event convert(EventEntity o) {
        Event e = new Event();
        e.setTime(o.getTime());
        e.setEvent(o.getEvent());
        e.setRealmId(o.getRealmId());
        e.setClientId(o.getClientId());
        e.setUserId(o.getUserId());
        e.setIpAddress(o.getIpAddress());
        e.setError(o.getError());

        JSONObject object = new JSONObject(o.getDetailsJson());
        Map<String, String> details = new HashMap<String, String>();
        for (Object k : object.keySet()) {
            details.put((String) k, object.getString((String) k));
        }

        e.setDetails(details);
        return e;
    }

}
