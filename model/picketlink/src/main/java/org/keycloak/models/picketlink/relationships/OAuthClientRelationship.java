package org.keycloak.models.picketlink.relationships;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.annotation.AttributeProperty;
import org.picketlink.idm.model.sample.User;
import org.picketlink.idm.query.AttributeParameter;
import org.picketlink.idm.query.RelationshipQueryParameter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientRelationship extends AbstractAttributedType implements Relationship {
    private static final long serialVersionUID = 1L;

    public static final AttributeParameter REALM = new AttributeParameter("realm");
    public static final RelationshipQueryParameter OAUTH_AGENT = new RelationshipQueryParameter() {

        @Override
        public String getName() {
            return "oauthAgent";
        }
    };
    protected User oauthAgent;


    public OAuthClientRelationship() {
    }

    public String getRealm() {
        return (String)getAttribute("realm").getValue();
    }

    public void setRealm(String realm) {
        setAttribute(new Attribute<String>("realm", realm));
    }

    public User getOauthAgent() {
        return oauthAgent;
    }

    public void setOauthAgent(User oauthAgent) {
        this.oauthAgent = oauthAgent;
    }

    @AttributeProperty
    public String getBaseUrl() {
        return (String)getAttribute("baseUrl").getValue();
    }

    public void setBaseUrl(String base) {
        setAttribute(new Attribute<String>("baseUrl", base));
    }

}
