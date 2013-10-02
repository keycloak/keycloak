package org.keycloak.models.picketlink.relationships;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.sample.User;
import org.picketlink.idm.query.AttributeParameter;
import org.picketlink.idm.query.RelationshipQueryParameter;

/**
 * Binding between user and his social username for particular Social provider
 *
 * Example: Keycloak user "john" has username "john123" in social provider "facebook"
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SocialLinkRelationship extends AbstractAttributedType implements Relationship {

    private static final long serialVersionUID = 154879L;

    public static final AttributeParameter SOCIAL_PROVIDER = new AttributeParameter("socialProvider");
    public static final AttributeParameter SOCIAL_USERNAME = new AttributeParameter("socialUsername");

    public static final RelationshipQueryParameter USER = new RelationshipQueryParameter() {

        @Override
        public String getName() {
            return "user";
        }
    };

    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSocialProvider() {
        return (String)getAttribute("socialProvider").getValue();
    }

    public void setSocialProvider(String socialProvider) {
        setAttribute(new Attribute<String>("socialProvider", socialProvider));
    }

    public String getSocialUsername() {
        return (String)getAttribute("socialUsername").getValue();
    }

    public void setSocialUsername(String socialProviderUserId) {
        setAttribute(new Attribute<String>("socialUsername", socialProviderUserId));
    }
}
