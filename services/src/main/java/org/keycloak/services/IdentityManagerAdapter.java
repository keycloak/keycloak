package org.keycloak.services;

import org.keycloak.services.model.data.RealmModel;
import org.keycloak.services.model.data.RequiredCredentialModel;
import org.keycloak.services.model.data.ResourceModel;
import org.keycloak.services.model.data.RoleMappingModel;
import org.keycloak.services.model.data.RoleModel;
import org.keycloak.services.model.data.ScopeMappingModel;
import org.keycloak.services.model.data.UserAttributeModel;
import org.keycloak.services.model.data.UserCredentialModel;
import org.keycloak.services.model.data.UserModel;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public interface IdentityManagerAdapter
{
   RealmModel getRealm(String id);
   RealmModel create(RealmModel realm);
   void update(RealmModel realm);
   void delete(RealmModel realm);
   List<RequiredCredentialModel> getRequiredCredentials(RealmModel realm);
   RequiredCredentialModel getRealmCredential(String id);
   RequiredCredentialModel create(RealmModel realm, RequiredCredentialModel cred);
   void update(RequiredCredentialModel cred);
   void delete(RequiredCredentialModel cred);

   UserModel getUser(RealmModel realm, String username);
   UserModel create(RealmModel realm, UserModel user);
   void update(RealmModel realm, UserModel user);
   void delete(RealmModel realm,UserModel user);

   List<UserCredentialModel> getCredentials(UserModel user);
   UserCredentialModel getCredential(String id);
   UserCredentialModel create(UserModel user, UserCredentialModel cred);
   void update(UserCredentialModel cred);
   void delete(UserCredentialModel cred);

   UserAttributeModel getUserAttribute(String id);
   UserAttributeModel create(UserModel user, UserAttributeModel attribute);
   void update(UserAttributeModel attribute);
   void delete(UserAttributeModel attribute);

   ResourceModel getResource(String resourceid);
   List<ResourceModel> getResources(RealmModel realm);
   ResourceModel create(RealmModel realm, ResourceModel resource);
   void update(ResourceModel resource);
   void delete(ResourceModel resource);


   RoleModel getRoleByName(RealmModel realm, String roleName);
   RoleModel getRoleByName(ResourceModel resource, String roleName);
   List<RoleModel> getRoles(RealmModel realm, ResourceModel resource);
   List<RoleModel> getRoles(RealmModel realm);
   RoleModel getRole(String id);
   RoleModel create(RealmModel realm, ResourceModel resource, String role);
   RoleModel create(RealmModel realm, String role);
   void delete(RoleModel role);

   List<RoleMappingModel> getRoleMappings(RealmModel realm);
   List<RoleMappingModel> getRoleMappings(RealmModel realm, ResourceModel resource);
   RoleMappingModel getRoleMapping(RealmModel realm, UserModel user);
   RoleMappingModel getRoleMapping(RealmModel realm, ResourceModel resource, UserModel user);
   RoleMappingModel getRoleMapping(String id);
   RoleMappingModel create(RealmModel realm, UserModel user, RoleMappingModel mapping);
   RoleMappingModel create(RealmModel realm, ResourceModel resource, UserModel user, RoleMappingModel mapping);
   void delete(RoleMappingModel role);

   List<ScopeMappingModel> getScopeMappings(RealmModel realm);
   List<ScopeMappingModel> getScopeMappings(RealmModel realm, ResourceModel resource);
   ScopeMappingModel getScopeMapping(RealmModel realm, UserModel user);
   ScopeMappingModel getScopeMapping(RealmModel realm, ResourceModel resource, UserModel user);
   ScopeMappingModel getScopeMapping(String id);
   ScopeMappingModel create(RealmModel realm, UserModel user, ScopeMappingModel mapping);
   ScopeMappingModel create(RealmModel realm, ResourceModel resource, UserModel user, ScopeMappingModel mapping);
   void delete(ScopeMappingModel scope);


   List<RealmModel> getRealmsByName(String name);
}
