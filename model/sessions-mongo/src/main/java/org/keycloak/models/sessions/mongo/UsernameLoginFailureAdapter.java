package org.keycloak.models.sessions.mongo;

import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.mongo.entities.MongoUsernameLoginFailureEntity;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsernameLoginFailureAdapter  extends AbstractMongoAdapter<MongoUsernameLoginFailureEntity> implements UsernameLoginFailureModel {
    protected MongoUsernameLoginFailureEntity user;

    public UsernameLoginFailureAdapter(MongoStoreInvocationContext invocationContext, MongoUsernameLoginFailureEntity user) {
        super(invocationContext);
        this.user = user;
    }

    @Override
    protected MongoUsernameLoginFailureEntity getMongoEntity() {
        return user;
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public int getFailedLoginNotBefore() {
        return user.getFailedLoginNotBefore();
    }

    @Override
    public void setFailedLoginNotBefore(int notBefore) {
        user.setFailedLoginNotBefore(notBefore);
        updateMongoEntity();
    }

    @Override
    public int getNumFailures() {
        return user.getNumFailures();
    }

    @Override
    public void incrementFailures() {
        user.setNumFailures(getNumFailures() + 1);
        updateMongoEntity();
    }

    @Override
    public void clearFailures() {
        user.setNumFailures(0);
        updateMongoEntity();
    }

    @Override
    public long getLastFailure() {
        return user.getLastFailure();
    }

    @Override
    public void setLastFailure(long lastFailure) {
        user.setLastFailure(lastFailure);
        updateMongoEntity();
    }

    @Override
    public String getLastIPFailure() {
        return user.getLastIPFailure();
    }

    @Override
    public void setLastIPFailure(String ip) {
        user.setLastIPFailure(ip);
        updateMongoEntity();
    }}
