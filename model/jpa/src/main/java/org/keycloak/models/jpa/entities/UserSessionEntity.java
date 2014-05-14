package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.GenericGenerator;
import org.keycloak.models.UserModel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "getUserSessionByUser", query = "select s from UserSessionEntity s where s.user = :user"),
        @NamedQuery(name = "removeUserSessionByUser", query = "delete from UserSessionEntity s where s.user = :user"),
        @NamedQuery(name = "removeUserSessionExpired", query = "delete from UserSessionEntity s where s.expires < :currentTime")
})
public class UserSessionEntity {

    @Id
    @GenericGenerator(name="uuid_generator", strategy="org.keycloak.models.jpa.utils.JpaIdGenerator")
    @GeneratedValue(generator = "uuid_generator")
    private String id;

    @ManyToOne
    private UserEntity user;

    String ipAddress;

    int started;

    int expires;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getStarted() {
        return started;
    }

    public void setStarted(int started) {
        this.started = started;
    }

    public int getExpires() {
        return expires;
    }

    public void setExpires(int expires) {
        this.expires = expires;
    }

}
