package org.keycloak.tests.events;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.storage.datastore.PeriodicEventInvalidation;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServerWrapper;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.util.JsonSerialization;

public class EventRunOnServerHelper {

    private final RunOnServerClient runOnServer;

    public EventRunOnServerHelper(RunOnServerClient runOnServer) {
        this.runOnServer = runOnServer;
    }

    public void storeEvent(EventRepresentation event) {
        String serialized = JsonSerialization.valueAsString(event);
        runOnServer.run(session -> {
            EventRepresentation rep = JsonSerialization.readValue(serialized, EventRepresentation.class);
            Event e = new Event();
            e.setId(UUID.randomUUID().toString());
            e.setClientId(rep.getClientId());
            e.setDetails(rep.getDetails());
            e.setError(rep.getError());
            e.setIpAddress(rep.getIpAddress());
            e.setRealmId(rep.getRealmId());
            e.setSessionId(rep.getSessionId());
            e.setTime(rep.getTime());
            e.setType(EventType.valueOf(rep.getType()));
            e.setUserId(rep.getUserId());

            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.onEvent(e);
        });
    }

    public void storeEvent(AdminEventRepresentation event, boolean includeRepresentation) {
        String serialized = JsonSerialization.valueAsString(event);
        runOnServer.run(session -> {
            AdminEventRepresentation rep = JsonSerialization.readValue(serialized, AdminEventRepresentation.class);
            AdminEvent e = new AdminEvent();
            e.setId(UUID.randomUUID().toString());

            AuthDetails details = new AuthDetails();
            details.setClientId(rep.getAuthDetails().getClientId());
            details.setIpAddress(rep.getAuthDetails().getIpAddress());
            details.setRealmId(rep.getAuthDetails().getRealmId());
            details.setUserId(rep.getAuthDetails().getUserId());
            e.setAuthDetails(details);

            e.setError(rep.getError());
            e.setOperationType(OperationType.valueOf(rep.getOperationType()));
            if (rep.getResourceType() != null) {
                e.setResourceTypeAsString(rep.getResourceType());
            }
            e.setRealmId(rep.getRealmId());
            e.setRepresentation(rep.getRepresentation());
            e.setResourcePath(rep.getResourcePath());
            e.setTime(rep.getTime());

            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.onEvent(e, includeRepresentation);
        });
    }

    public EventRepresentation[] queryEvents(String realmId, List<EventType> types, String client,
                                             String user, String dateFrom, String dateTo,
                                             String ipAddress, Integer firstResult,
                                             Integer maxResults) {
        return runOnServer.fetch(new QueryHelper(realmId, types, client, user, dateFrom, dateTo, ipAddress, firstResult, maxResults));
    }

    public AdminEventRepresentation[] queryAdminEvents(String realmId, List<OperationType> operationTypes, String authRealm, String authClient,
                                                       String authUser, String authIpAddress,
                                                       String resourcePath, String dateFrom,
                                                       String dateTo, Integer firstResult,
                                                       Integer maxResults) {
        return runOnServer.fetch(new AdminQueryHelper(realmId, operationTypes, authRealm, authClient, authUser, authIpAddress, resourcePath, dateFrom, dateTo, firstResult, maxResults));
    }

    public void clearEvents(String... realmIds) {
        runOnServer.run(session -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            for (String realmId : realmIds) {
                eventStore.clear(session.realms().getRealm(realmId));
            }
        });
    }

    public void clearAdminEvents(String... realmIds) {
        runOnServer.run(session -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            for (String realmId : realmIds) {
                eventStore.clearAdmin(session.realms().getRealm(realmId));
            }
        });
    }

    public void clearAdminEvents(String realmId, long olderThan) {
        runOnServer.run(session -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.clearAdmin(session.realms().getRealm(realmId), olderThan);
        });
    }

    public void clearExpiredEvents() {
        runOnServer.run(session -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.clearExpiredEvents();
            session.invalidate(PeriodicEventInvalidation.JPA_EVENT_STORE);
        });
    }

    private static class QueryHelper implements FetchOnServerWrapper<EventRepresentation[]>, Serializable {

        private final String realmId;
        private final List<EventType> types;
        private final String client;
        private final String user;
        private final String dateFrom;
        private final String dateTo;
        private final String ipAddress;
        private final Integer firstResult;
        private final Integer maxResults;

        public QueryHelper(String realmId, List<EventType> types, String client,
                           String user, String dateFrom, String dateTo,
                           String ipAddress, Integer firstResult,
                           Integer maxResults) {
            this.realmId = realmId;
            this.types = types;
            this.client = client;
            this.user = user;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.ipAddress = ipAddress;
            this.firstResult = firstResult;
            this.maxResults = maxResults;
        }

        @Override
        public FetchOnServer getRunOnServer() {
            return session -> {
                EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

                EventQuery query = eventStore.createQuery();

                if (realmId != null) {
                    query.realm(realmId);
                }

                if (client != null) {
                    query.client(client);
                }

                if (types != null && !types.isEmpty()) {
                    query.type(types.toArray(new EventType[0]));
                }

                if (user != null) {
                    query.user(user);
                }

                if (dateFrom != null) {
                    Date from = formatDate(dateFrom, "Date(From)");
                    query.fromDate(from);
                }

                if (dateTo != null) {
                    Date to = formatDate(dateTo, "Date(To)");
                    query.toDate(to);
                }

                if (ipAddress != null) {
                    query.ipAddress(ipAddress);
                }
                if (firstResult != null) {
                    query.firstResult(firstResult);
                }
                if (maxResults != null) {
                    query.maxResults(maxResults);
                }

                return query.getResultStream().map(ModelToRepresentation::toRepresentation).toList();
            };
        }

        @Override
        public Class<EventRepresentation[]> getResultClass() {
            return EventRepresentation[].class;
        }

        private Date formatDate(String date, String paramName) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            try {
                return df.parse(date);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for '" + paramName + "', expected format is yyyy-MM-dd");
            }
        }
    }

    private static class AdminQueryHelper implements FetchOnServerWrapper<AdminEventRepresentation[]>, Serializable {

        private String realmId;
        private List<OperationType> operationTypes;
        private String authRealm;
        private String authClient;
        private String authUser;
        private String authIpAddress;
        private String resourcePath;
        private String dateFrom;
        private String dateTo;
        private Integer firstResult;
        private Integer maxResults;

        public AdminQueryHelper(String realmId, List<OperationType> operationTypes, String authRealm, String authClient,
                                String authUser, String authIpAddress,
                                String resourcePath, String dateFrom,
                                String dateTo, Integer firstResult,
                                Integer maxResults) {

            this.realmId = realmId;
            this.operationTypes = operationTypes;
            this.authRealm = authRealm;
            this.authClient = authClient;
            this.authUser = authUser;
            this.authIpAddress = authIpAddress;
            this.resourcePath = resourcePath;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.firstResult = firstResult;
            this.maxResults = maxResults;
        }

        @Override
        public FetchOnServer getRunOnServer() {
            return session -> {
                EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
                AdminEventQuery query = eventStore.createAdminQuery();

                if (realmId != null) {
                    query.realm(realmId);
                }

                if (authRealm != null) {
                    query.authRealm(authRealm);
                }

                if (authClient != null) {
                    query.authClient(authClient);
                }

                if (authUser != null) {
                    query.authUser(authUser);
                }

                if (authIpAddress != null) {
                    query.authIpAddress(authIpAddress);
                }

                if (resourcePath != null) {
                    query.resourcePath(resourcePath);
                }

                if (operationTypes != null && !operationTypes.isEmpty()) {
                    query.operation(operationTypes.toArray(new OperationType[0]));
                }

                if (dateFrom != null) {
                    Date from = formatDate(dateFrom, "Date(From)");
                    query.fromTime(from);
                }

                if (dateTo != null) {
                    Date to = formatDate(dateTo, "Date(To)");
                    query.toTime(to);
                }

                if (firstResult != null || maxResults != null) {
                    if (firstResult == null) {
                        firstResult = 0;
                    }
                    if (maxResults == null) {
                        maxResults = 100;
                    }
                    query.firstResult(firstResult);
                    query.maxResults(maxResults);
                }

                return query.getResultStream().map(ModelToRepresentation::toRepresentation).toList();
            };
        }

        @Override
        public Class<AdminEventRepresentation[]> getResultClass() {
            return AdminEventRepresentation[].class;
        }

        private Date formatDate(String date, String paramName) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            try {
                return df.parse(date);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for '" + paramName + "', expected format is yyyy-MM-dd");
            }
        }
    }


}
