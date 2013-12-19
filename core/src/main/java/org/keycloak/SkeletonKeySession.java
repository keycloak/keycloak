package org.keycloak;

import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.representations.SkeletonKeyToken;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SkeletonKeySession implements Serializable {
    protected String tokenString;
    protected SkeletonKeyToken token;
    protected transient ResourceMetadata metadata;

    public SkeletonKeySession() {
    }

    public SkeletonKeySession(String tokenString, SkeletonKeyToken token, ResourceMetadata metadata) {
        this.tokenString = tokenString;
        this.token = token;
        this.metadata = metadata;
    }

    public SkeletonKeyToken getToken() {
        return token;
    }

    public String getTokenString() {
        return tokenString;
    }

    public ResourceMetadata getMetadata() {
        return metadata;
    }

}
