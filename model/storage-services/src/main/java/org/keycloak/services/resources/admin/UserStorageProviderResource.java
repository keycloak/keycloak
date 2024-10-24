/**
 * @resource User Storage Provider
 * @author
 * @version $Revision: 1 $
 */
public class UserStorageProviderResource {
    private static final Logger logger = Logger.getLogger(UserStorageProviderResource.class);

    protected final RealmModel realm;

    protected final AdminPermissionEvaluator auth;

    protected final AdminEventBuilder adminEvent;

    protected final ClientConnection clientConnection;

    protected final KeycloakSession session;

    protected final HttpHeaders headers;

    public UserStorageProviderResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.adminEvent = adminEvent;
        this.clientConnection = session.getContext().getConnection();
        this.headers = session.getContext().getRequestHeaders();
    }

    public static String getErrorCode(Throwable throwable) {
        if (throwable instanceof org.keycloak.models.ModelException) {
           if (throwable.getCause() != null) {
                return getErrorCode(throwable.getCause());
            }
        }
        return LDAPServerCapabilitiesManager.getErrorCode(throwable);
    }

    /**
     * Need this for admin console to display simple name of provider when displaying user detail
     *
     * KEYCLOAK-4328
     *
     * @param id
     * @return
     */
    @GET
    @Path("{id}/name")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getSimpleName(@PathParam("id") String id) {
        auth.users().requireQuery();

        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) {
            throw new NotFoundException("Found, but not a UserStorageProvider");
        }

        // No log statement here regarding removing users since the method is fetching a name

        Map<String, String> data = new HashMap<>();
        data.put("id", model.getId());
        data.put("name", model.getName());
        return data;
    }

    /**
     * Trigger sync of users
     *
     * Action can be "triggerFullSync" or "triggerChangedUsersSync"
     *
     * @param id
     * @param action
     * @return
     */
    @POST
    @Path("{id}/sync")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public SynchronizationResult syncUsers(@PathParam("id") String id,
                                           @QueryParam("action") String action) {
        auth.users().requireManage();

        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) {
            throw new NotFoundException("Found, but not a UserStorageProvider");
        }

        UserStorageProviderModel providerModel = new UserStorageProviderModel(model);

        logger.debug("Syncing users");

        UserStorageSyncManager syncManager = new UserStorageSyncManager();
        SynchronizationResult syncResult;
        if ("triggerFullSync".equals(action)) {
            try {
                syncResult = syncManager.syncAllUsers(session.getKeycloakSessionFactory(), realm.getId(), providerModel);
            } catch(Exception e) {
                String errorMsg = getErrorCode(e);
                throw ErrorResponse.error(errorMsg, Response.Status.BAD_REQUEST);
            }
        } else if ("triggerChangedUsersSync".equals(action)) {
            try {
                syncResult = syncManager.syncChangedUsers(session.getKeycloakSessionFactory(), realm.getId(), providerModel);
            } catch(Exception e) {
                String errorMsg = getErrorCode(e);
                throw ErrorResponse.error(errorMsg, Response.Status.BAD_REQUEST);
            }
        } else if (action == null || action.isEmpty()) {
            logger.debug("Missing action");
            throw new BadRequestException("Missing action");
        } else {
            logger.debug("Unknown action: " + action);
            throw new BadRequestException("Unknown action: " + action);
        }

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("action", action);
        eventRep.put("result", syncResult);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(eventRep).success();

        return syncResult;
    }

    /**
     * Remove imported users
     *
     * @param id
     */
    @POST
    @Path("{id}/remove-imported-users")
    @NoCache
    public void removeImportedUsers(@PathParam("id") String id) {
        auth.users().requireManage();

        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) {
            throw new NotFoundException("Found, but not a UserStorageProvider");
        }

        session.users().removeImportedUsers(realm, id);
        logger.infof("Successfully removed imported users for federation provider: '%s' with name: '%s', realm: '%s'", model.getProviderId(), model.getName(), realm.getId());

        Map<String, String> eventRep = new HashMap<>();
        eventRep.put("action", "remove-imported-users");
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(eventRep).success();
    }

    /**
     * Unlink imported users from a storage provider
     *
     * @param id
     */
    @POST
    @Path("{id}/unlink-users")
    @NoCache
    public void unlinkUsers(@PathParam("id") String id) {
        auth.users().requireManage();

        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) {
            throw new NotFoundException("Found, but not a UserStorageProvider");
        }

        session.users().unlinkUsers(realm, id);
    }

    /**
     * Trigger sync of mapper data related to ldap mapper (roles, groups, ...)
     *
     * direction is "fedToKeycloak" or "keycloakToFed"
     *
     * @return
     */
    @POST
    @Path("{parentId}/mappers/{id}/sync")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public SynchronizationResult syncMapperData(@PathParam("parentId") String parentId, @PathParam("id") String mapperId, @QueryParam("direction") String direction) {
        auth.users().requireManage();

        ComponentModel parentModel = realm.getComponent(parentId);
        if (parentModel == null) throw new NotFoundException("Parent model not found");
        ComponentModel mapperModel = realm.getComponent(mapperId);
        if (mapperModel == null) throw new NotFoundException("Mapper model not found");

        LDAPStorageProvider ldapProvider = (LDAPStorageProvider) session.getProvider(UserStorageProvider.class, parentModel);
        LDAPStorageMapper mapper = session.getProvider(LDAPStorageMapper.class, mapperModel);

        ServicesLogger.LOGGER.syncingDataForMapper(mapperModel.getName(), mapperModel.getProviderId(), direction);

        SynchronizationResult syncResult;
        if ("fedToKeycloak".equals(direction)) {
            try {
                syncResult = mapper.syncDataFromFederationProviderToKeycloak(realm);
            } catch(Exception e) {
                String errorMsg = getErrorCode(e);
                throw ErrorResponse.error(errorMsg, Response.Status.BAD_REQUEST);
            }
        } else if ("keycloakToFed".equals(direction)) {
            try {
                syncResult = mapper.syncDataFromKeycloakToFederationProvider(realm);
            } catch(Exception e) {
                String errorMsg = getErrorCode(e);
                throw ErrorResponse.error(errorMsg, Response.Status.BAD_REQUEST);
            }
        } else {
            throw new BadRequestException("Unknown direction: " + direction);
        }

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("action", direction);
        eventRep.put("result", syncResult);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(eventRep).success();
        return syncResult;
    }
}
