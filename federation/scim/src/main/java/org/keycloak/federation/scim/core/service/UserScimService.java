package org.keycloak.federation.scim.core.service;

import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.MultiComplexNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PersonRole;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.federation.scim.core.ScrimEndPointConfiguration;
import org.keycloak.federation.scim.core.exceptions.InconsistentScimMappingException;
import org.keycloak.federation.scim.core.exceptions.SkipOrStopStrategy;
import org.keycloak.federation.scim.core.exceptions.UnexpectedScimDataException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class UserScimService extends AbstractScimService<UserModel, User> {
    private static final Logger LOGGER = Logger.getLogger(UserScimService.class);

    public UserScimService(KeycloakSession keycloakSession, ScrimEndPointConfiguration scimProviderConfiguration,
            SkipOrStopStrategy skipOrStopStrategy) {
        super(keycloakSession, scimProviderConfiguration, ScimResourceType.USER, skipOrStopStrategy);
    }

    @Override
    protected Stream<UserModel> getResourceStream() {
        return getKeycloakDao().getUsersStream();
    }

    @Override
    protected boolean entityExists(KeycloakId keycloakId) {
        return getKeycloakDao().userExists(keycloakId);
    }

    @Override
    protected Optional<KeycloakId> matchKeycloakMappingByScimProperties(User resource) throws InconsistentScimMappingException {
        Optional<KeycloakId> matchedByUsername = resource.getUserName().map(getKeycloakDao()::getUserByUsername)
                .map(this::getId);
        Optional<KeycloakId> matchedByEmail = resource.getEmails().stream().findFirst().flatMap(MultiComplexNode::getValue)
                .map(getKeycloakDao()::getUserByEmail).map(this::getId);
        if (matchedByUsername.isPresent() && matchedByEmail.isPresent() && !matchedByUsername.equals(matchedByEmail)) {
            String inconstencyErrorMessage = "Found 2 possible users for remote user " + matchedByUsername.get() + " - "
                    + matchedByEmail.get();
            LOGGER.warn(inconstencyErrorMessage);
            throw new InconsistentScimMappingException(inconstencyErrorMessage);
        }
        if (matchedByUsername.isPresent()) {
            return matchedByUsername;
        }
        return matchedByEmail;
    }

    @Override
    protected KeycloakId createEntity(User resource) throws UnexpectedScimDataException {
        String username = resource.getUserName().filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new UnexpectedScimDataException(
                        "Remote Scim user has empty username, can't create. Resource id = %s".formatted(resource.getId())));
        UserModel user = getKeycloakDao().addUser(username);
        resource.getEmails().stream().findFirst().flatMap(MultiComplexNode::getValue).ifPresent(user::setEmail);
        boolean userEnabled = resource.isActive().orElse(false);
        user.setEnabled(userEnabled);
        user.setFederationLink(getConfiguration().getId());
        return new KeycloakId(user.getId());
    }

    @Override
    protected boolean isMarkedToIgnore(UserModel userModel) {
        return BooleanUtils.TRUE.equals(userModel.getFirstAttribute("scim-skip"));
    }

    @Override
    protected KeycloakId getId(UserModel userModel) {
        return new KeycloakId(userModel.getId());
    }

    @Override
    protected User scimRequestBodyForCreate(UserModel roleMapperModel) {
        String firstAndLastName = String.format("%s %s", StringUtils.defaultString(roleMapperModel.getFirstName()),
                StringUtils.defaultString(roleMapperModel.getLastName())).trim();
        String displayName = Objects.toString(firstAndLastName, roleMapperModel.getUsername());
        Stream<RoleModel> groupRoleModels = roleMapperModel.getGroupsStream().flatMap(RoleMapperModel::getRoleMappingsStream);
        Stream<RoleModel> roleModels = roleMapperModel.getRoleMappingsStream();
        Stream<RoleModel> allRoleModels = Stream.concat(groupRoleModels, roleModels);
        List<PersonRole> roles = allRoleModels.filter(r -> BooleanUtils.TRUE.equals(r.getFirstAttribute("scim")))
                .map(RoleModel::getName).map(roleName -> {
                    PersonRole personRole = new PersonRole();
                    personRole.setValue(roleName);
                    return personRole;
                }).toList();
        User user = new User();
        user.setRoles(roles);
        user.setExternalId(roleMapperModel.getId());
        user.setUserName(roleMapperModel.getUsername());
        user.setDisplayName(displayName);
        Name name = new Name();
        name.setFamilyName(roleMapperModel.getLastName());
        name.setGivenName(roleMapperModel.getFirstName());
        user.setName(name);
        List<Email> emails = new ArrayList<>();
        if (roleMapperModel.getEmail() != null) {
            emails.add(Email.builder().value(roleMapperModel.getEmail()).build());
        }
        user.setEmails(emails);
        user.setActive(roleMapperModel.isEnabled());
        return user;
    }

    @Override
    protected User scimRequestBodyForUpdate(UserModel userModel, String externalId) {
        User user = scimRequestBodyForCreate(userModel);
        user.setId(externalId);
        Meta meta = newMetaLocation(externalId);
        user.setMeta(meta);
        return user;
    }

    @Override
    protected boolean shouldIgnoreForScimSynchronization(UserModel userModel) {
        return "admin".equals(userModel.getUsername());
    }
}
