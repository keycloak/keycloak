package org.keycloak.representations.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Creates a response understandable by the docker client in the form:
 *
 {
 "token" : "eyJh...nSQ",
 "expires_in" : 300,
 "issued_at" : "2016-09-02T10:56:33Z"
 }
 */
public class DockerResponse {

    @JsonProperty("token")
    private String token;
    @JsonProperty("expires_in")
    private Integer expires_in;
    @JsonProperty("issued_at")
    private String issued_at;

    public DockerResponse() {
    }

    public DockerResponse(final String token, final Integer expires_in, final String issued_at) {
        this.token = token;
        this.expires_in = expires_in;
        this.issued_at = issued_at;
    }

    public String getToken() {
        return token;
    }

    public DockerResponse setToken(final String token) {
        this.token = token;
        return this;
    }

    public Integer getExpires_in() {
        return expires_in;
    }

    public DockerResponse setExpires_in(final Integer expires_in) {
        this.expires_in = expires_in;
        return this;
    }

    public String getIssued_at() {
        return issued_at;
    }

    public DockerResponse setIssued_at(final String issued_at) {
        this.issued_at = issued_at;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerResponse)) return false;

        final DockerResponse that = (DockerResponse) o;

        if (token != null ? !token.equals(that.token) : that.token != null) return false;
        if (expires_in != null ? !expires_in.equals(that.expires_in) : that.expires_in != null) return false;
        return issued_at != null ? issued_at.equals(that.issued_at) : that.issued_at == null;

    }

    @Override
    public int hashCode() {
        int result = token != null ? token.hashCode() : 0;
        result = 31 * result + (expires_in != null ? expires_in.hashCode() : 0);
        result = 31 * result + (issued_at != null ? issued_at.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DockerResponse{" +
                "token='" + token + '\'' +
                ", expires_in='" + expires_in + '\'' +
                ", issued_at='" + issued_at + '\'' +
                '}';
    }
}
