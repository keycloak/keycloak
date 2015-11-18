package org.keycloak.representations.idm;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientInitialAccessCreatePresentation {

    private Integer expiration;

    private Integer count;

    public ClientInitialAccessCreatePresentation() {
    }

    public ClientInitialAccessCreatePresentation(Integer expiration, Integer count) {
        this.expiration = expiration;
        this.count = count;
    }

    public Integer getExpiration() {
        return expiration;
    }

    public void setExpiration(Integer expiration) {
        this.expiration = expiration;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

}
