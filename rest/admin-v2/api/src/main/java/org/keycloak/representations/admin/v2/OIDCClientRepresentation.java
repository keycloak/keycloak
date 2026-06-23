package org.keycloak.representations.admin.v2;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.keycloak.representations.admin.v2.validation.ClientSecretNotBlank;
import org.keycloak.representations.admin.v2.validation.ConfidentialFlowsRequireAuth;
import org.keycloak.representations.admin.v2.validation.PutClient;
import org.keycloak.representations.admin.v2.validation.RedirectFlowsRequireUris;
import org.keycloak.representations.admin.v2.validation.ServiceAccountRolesRequireFlow;
import org.keycloak.representations.admin.v2.validation.ValidAuthMethod;
import org.keycloak.representations.admin.v2.validation.ValidWebOrigin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
@ConfidentialFlowsRequireAuth
@RedirectFlowsRequireUris
@ServiceAccountRolesRequireFlow
public class OIDCClientRepresentation extends BaseClientRepresentation {
    public static final String PROTOCOL = "openid-connect";

    public enum Flow {
        STANDARD,
        IMPLICIT,
        DIRECT_GRANT,
        SERVICE_ACCOUNT,
        TOKEN_EXCHANGE,
        DEVICE,
        CIBA
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Login flows that are enabled for this client")
    private Set<Flow> loginFlows = new LinkedHashSet<>();

    @JsonMerge
    @Valid
    @JsonPropertyDescription("Authentication configuration for this client")
    private Auth auth;

    @Size(max = 100)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Web origins that are allowed to make requests to this client")
    private Set<@NotBlank @Size(max = 255) @ValidWebOrigin String> webOrigins = new LinkedHashSet<>();

    @Size(max = 300)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Roles assigned to the service account")
    private Set<@NotBlank @Size(max = 255) String> serviceAccountRoles = new LinkedHashSet<>();

    public OIDCClientRepresentation() {
        this.protocol = PROTOCOL;
    }

    public OIDCClientRepresentation(String clientId) {
        this.protocol = PROTOCOL;
        this.clientId = clientId;
    }

    public Set<Flow> getLoginFlows() {
        return loginFlows;
    }

    public void setLoginFlows(Set<Flow> loginFlows) {
        this.loginFlows = loginFlows;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public Set<String> getServiceAccountRoles() {
        return serviceAccountRoles;
    }

    public void setServiceAccountRoles(Set<String> serviceAccountRoles) {
        this.serviceAccountRoles = serviceAccountRoles;
    }

    @ClientSecretNotBlank(groups = PutClient.class, affectedFieldNames = {"secret"})
    public static class Auth extends BaseRepresentation {

        @NotBlank
        @ValidAuthMethod
        @JsonPropertyDescription("Which authentication method is used for this client")
        private String method;

        @Size(min = 6, max = 255)
        @JsonPropertyDescription("Secret used to authenticate this client with Secret authentication")
        private String secret;

        @Size(max = 65536)
        @JsonPropertyDescription("Public key used to authenticate this client with Signed JWT authentication")
        private String certificate;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Auth)) {
                return false;
            }
            Auth auth = (Auth)o;
            return Objects.equals(method, auth.method) && Objects.equals(secret, auth.secret) && Objects.equals(certificate, auth.certificate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, secret, certificate);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OIDCClientRepresentation)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        OIDCClientRepresentation that = (OIDCClientRepresentation)o;
        return Objects.equals(loginFlows, that.loginFlows) && Objects.equals(auth, that.auth) && Objects.equals(webOrigins, that.webOrigins) && Objects.equals(serviceAccountRoles, that.serviceAccountRoles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), loginFlows, auth, webOrigins, serviceAccountRoles);
    }
}
