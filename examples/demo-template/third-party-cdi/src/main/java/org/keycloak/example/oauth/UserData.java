package org.keycloak.example.oauth;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SessionScoped
@Named("userData")
public class UserData implements Serializable {

    private String accessToken;
    private List<String> products;
    private List<String> customers;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isHasAccessToken() {
        return accessToken != null;
    }

    public String getAccessTokenAvailabilityMessage() {
        StringBuilder builder = new StringBuilder("Access token ");
        if (!isHasAccessToken()) {
            builder.append("not ");
        }
        return builder.append("available!").toString();
    }

    public List<String> getProducts() {
        return products;
    }

    public void setProducts(List<String> products) {
        this.products = products;
    }

    public boolean isHasProducts() {
        return products != null;
    }

    public List<String> getCustomers() {
        return customers;
    }

    public void setCustomers(List<String> customers) {
        this.customers = customers;
    }

    public boolean isHasCustomers() {
        return customers != null;
    }
}
