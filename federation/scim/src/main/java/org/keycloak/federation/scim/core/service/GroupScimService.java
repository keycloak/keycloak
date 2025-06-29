package org.keycloak.federation.scim.core.service;

import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.federation.scim.core.ScimEndPointConfiguration;
import org.keycloak.federation.scim.core.exceptions.InconsistentScimMappingException;
import org.keycloak.federation.scim.core.exceptions.SkipOrStopStrategy;
import org.keycloak.federation.scim.core.exceptions.UnexpectedScimDataException;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupScimService extends AbstractScimService<GroupModel, Group> {
    private static final Logger LOGGER = Logger.getLogger(GroupScimService.class);

    public GroupScimService(KeycloakSession keycloakSession, ScimEndPointConfiguration scimProviderConfiguration,
            SkipOrStopStrategy skipOrStopStrategy) {
        super(keycloakSession, scimProviderConfiguration, ScimResourceType.GROUP, skipOrStopStrategy);
    }

    @Override
    protected Stream<GroupModel> getResourceStream() {
        return keycloakSession.groups().getGroupsStream(getRealm());
    }

    @Override
    protected boolean entityExists(String keycloakId) {
        return keycloakSession.groups().getGroupById(getRealm(), keycloakId) != null;
    }

    @Override
    protected Optional<String> matchKeycloakMappingByScimProperties(Group resource) {
        Set<String> names = new TreeSet<>();
        resource.getId().ifPresent(names::add);
        resource.getDisplayName().ifPresent(names::add);
        try (Stream<GroupModel> groupsStream = keycloakSession.groups().getGroupsStream(getRealm())) {
            Optional<GroupModel> group = groupsStream.filter(groupModel -> names.contains(groupModel.getName())).findFirst();
            return group.map(GroupModel::getId);
        }
    }

    @Override
    protected String createEntity(Group resource) throws UnexpectedScimDataException, InconsistentScimMappingException {
        String displayName = resource.getDisplayName().filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new UnexpectedScimDataException(
                        "Remote Scim group has empty name, can't create. Resource id = %s".formatted(resource.getId())));
        RealmModel realm = getRealm();
        GroupModel group = keycloakSession.groups().createGroup(realm, displayName);
        List<Member> groupMembers = resource.getMembers();
        if (CollectionUtils.isNotEmpty(groupMembers)) {
            for (Member groupMember : groupMembers) {
                String externalId = groupMember.getValue()
                        .orElseThrow(() -> new UnexpectedScimDataException(
                                "can't create group member for group '%s' without id: ".formatted(displayName) + resource));
                String userId = findByExternalId(externalId, ScimResourceType.USER);

                if (userId == null) {
                    throw new InconsistentScimMappingException("can't find mapping for group member %s".formatted(externalId));
                }
                UserModel userModel = keycloakSession.users().getUserById(realm, userId);
                userModel.joinGroup(group);
            }
        }
        return group.getId();
    }

    @Override
    protected boolean isMarkedToIgnore(GroupModel groupModel) {
        return BooleanUtils.TRUE.equals(groupModel.getFirstAttribute("scim-skip"));
    }

    @Override
    protected String getId(GroupModel groupModel) {
        return groupModel.getId();
    }

    @Override
    protected Group scimRequestBodyForCreate(GroupModel groupModel) throws InconsistentScimMappingException {
        Set<String> members = keycloakSession.users().getGroupMembersStream(getRealm(), groupModel).map(UserModel::getId).collect(Collectors.toSet());
        Group group = new Group();
        group.setExternalId(groupModel.getId());
        group.setDisplayName(groupModel.getName());
        for (String member : members) {
            Member groupMember = new Member();
            String externalIdAsEntityOnRemoteScimId = findMappingById(member);
            if (externalIdAsEntityOnRemoteScimId != null) {
                groupMember.setValue(externalIdAsEntityOnRemoteScimId);
                URI ref = getUri(ScimResourceType.USER, externalIdAsEntityOnRemoteScimId);
                groupMember.setRef(ref.toString());
                group.addMember(groupMember);
            } else {
                String message = "Unmapped member " + member + " for group " + groupModel.getId();
                if (skipOrStopStrategy.allowMissingMembersWhenPushingGroupToScim(this.getConfiguration())) {
                    LOGGER.warn(message);
                } else {
                    throw new InconsistentScimMappingException(message);
                }
            }
        }
        return group;
    }

    @Override
    protected Group scimRequestBodyForUpdate(GroupModel groupModel, String externalId)
            throws InconsistentScimMappingException {
        Group group = scimRequestBodyForCreate(groupModel);
        group.setId(externalId);
        Meta meta = newMetaLocation(externalId);
        group.setMeta(meta);
        return group;
    }

    @Override
    protected boolean shouldIgnoreForScimSynchronization(GroupModel resource) {
        return false;
    }
}
