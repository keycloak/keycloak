package org.keycloak.events.jpa;

import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventType;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaEventQuery implements EventQuery {

    private final EntityManager em;
    private final CriteriaBuilder cb;
    private final CriteriaQuery<EventEntity> cq;
    private final Root<EventEntity> root;
    private final ArrayList<Predicate> predicates;
    private Integer firstResult;
    private Integer maxResults;

    public JpaEventQuery(EntityManager em) {
        this.em = em;

        cb = em.getCriteriaBuilder();
        cq = cb.createQuery(EventEntity.class);
        root = cq.from(EventEntity.class);
        predicates = new ArrayList<Predicate>(4);
    }

    @Override
    public EventQuery type(EventType... types) {
        List<String> eventStrings = new LinkedList<String>();
        for (EventType e : types) {
            eventStrings.add(e.toString());
        }
        predicates.add(root.get("type").in(eventStrings));
        return this;
    }

    @Override
    public EventQuery realm(String realmId) {
        predicates.add(cb.equal(root.get("realmId"), realmId));
        return this;
    }

    @Override
    public EventQuery client(String clientId) {
        predicates.add(cb.equal(root.get("clientId"), clientId));
        return this;
    }

    @Override
    public EventQuery user(String userId) {
        predicates.add(cb.equal(root.get("userId"), userId));
        return this;
    }

    @Override
    public EventQuery ipAddress(String ipAddress) {
        predicates.add(cb.equal(root.get("ipAddress"), ipAddress));
        return this;
    }

    @Override
    public EventQuery firstResult(int firstResult) {
        this.firstResult = firstResult;
        return this;
    }

    @Override
    public EventQuery maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public List<Event> getResultList() {
        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        }

        cq.orderBy(cb.desc(root.get("time")));

        TypedQuery<EventEntity> query = em.createQuery(cq);

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }

        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        List<Event> events = new LinkedList<Event>();
        for (EventEntity e : query.getResultList()) {
            events.add(JpaEventStoreProvider.convert(e));
        }

        return events;
    }

}
