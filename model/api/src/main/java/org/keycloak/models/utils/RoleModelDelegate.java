package org.keycloak.models.utils;

import java.util.Map;
import java.util.Set;

import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:jli@vizuri.com">Jiehuan Li</a>
 * @version $Revision: 1 $
 */
public class RoleModelDelegate implements RoleModel {
    protected RoleModel delegate;

    public RoleModelDelegate(RoleModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void setName(String rolename) {
        delegate.setName(rolename);
    }

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public void setDescription(String description) {
		delegate.setDescription(description);
	}

	@Override
	public boolean isComposite() {
		return delegate.isComposite();
	}

	@Override
	public void addCompositeRole(RoleModel role) {
		delegate.addCompositeRole(role);
		
	}

	@Override
	public void removeCompositeRole(RoleModel role) {
		delegate.removeCompositeRole(role);
	}

	@Override
	public Set<RoleModel> getComposites() {
		return delegate.getComposites();
	}

	@Override
	public RoleContainerModel getContainer() {
		return delegate.getContainer();
	}

	@Override
	public boolean hasRole(RoleModel role) {
		return delegate.hasRole(role);
	}
    
    
    
    //TODO: Implement the following
    @Override
    public void setAttribute(String name, String value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }
//
//    @Override
//    public Set<RequiredAction> getRequiredActions() {
//        return delegate.getRequiredActions();
//    }
//
//    @Override
//    public void addRequiredAction(RequiredAction action) {
//        delegate.addRequiredAction(action);
//    }
//
//    @Override
//    public void removeRequiredAction(RequiredAction action) {
//        delegate.removeRequiredAction(action);
//    }
//
//    @Override
//    public Set<UserModel> getRealmUserMappings() {
//        return delegate.getRealmUserMappings();
//    }
//
//    @Override
//    public Set<RoleModel> getApplicationUserMappings(ApplicationModel app) {
//        return delegate.getApplicationUserMappings(app);
//    }
//
//    @Override
//    public boolean hasUser(UserModel user) {
//        return delegate.hasUser(user);
//    }
//
//    @Override
//    public void addUser(UserModel user) {
//        delegate.addUser(user);
//    }
//
//    @Override
//    public Set<UserModel> getUserMappings() {
//        return delegate.getUserMappings();
//    }
//
//    @Override
//    public void deleteUserMapping(UserModel user) {
//        delegate.deleteUserMapping(user);
//    }
//
    @Override
    public String getFederationLink() {
        return delegate.getFederationLink();
    }

    @Override
    public void setFederationLink(String link) {
        delegate.setFederationLink(link);
    }
}
