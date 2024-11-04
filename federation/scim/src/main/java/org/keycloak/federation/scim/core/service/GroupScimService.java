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
import org.keycloak.models.UserModel;
import org.keycloak.federation.scim.core.ScrimEndPointConfiguration;
import org.keycloak.federation.scim.core.exceptions.InconsistentScimMappingException;
import org.keycloak.federation.scim.core.exceptions.SkipOrStopStrategy;
import org.keycloak.federation.scim.core.exceptions.UnexpectedScimDataException;
import org.keycloak.federation.scim.jpa.ScimResourceMapping;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class GroupScimService extends AbstractScimService<GroupModel, Group> {
    private static final Logger LOGGER = Logger.getLogger(GroupScimService.class);

    public GroupScimService(KeycloakSession keycloakSession, ScrimEndPointConfiguration scimProviderConfiguration,
            SkipOrStopStrategy skipOrStopStrategy) {
        super(keycloakSession, scimProviderConfiguration, ScimResourceType.GROUP, skipOrStopStrategy);
    }

    @Override
    protected Stream<GroupModel> getResourceStream() {
        return getKeycloakDao().getGroupsStream();
    }

    @Override
    protected boolean entityExists(KeycloakId keycloakId) {
        return getKeycloakDao().groupExists(keycloakId);
    }

    @Override
    protected Optional<KeycloakId> matchKeycloakMappingByScimProperties(Group resource) {
        Set<String> names = new TreeSet<>();
        resource.getId().ifPresent(names::add);
        resource.getDisplayName().ifPresent(names::add);
        try (Stream<GroupModel> groupsStream = getKeycloakDao().getGroupsStream()) {
            Optional<GroupModel> group = groupsStream.filter(groupModel -> names.contains(groupModel.getName())).findFirst();
            return group.map(GroupModel::getId).map(KeycloakId::new);
        }
    }

    @Override
    protected KeycloakId createEntity(Group resource) throws UnexpectedScimDataException, InconsistentScimMappingException {
        String displayName = resource.getDisplayName().filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new UnexpectedScimDataException(
                        "Remote Scim group has empty name, can't create. Resource id = %s".formatted(resource.getId())));
        GroupModel group = getKeycloakDao().createGroup(displayName);
        List<Member> groupMembers = resource.getMembers();
        if (CollectionUtils.isNotEmpty(groupMembers)) {
            for (Member groupMember : groupMembers) {
                EntityOnRemoteScimId externalId = groupMember.getValue().map(EntityOnRemoteScimId::new)
                        .orElseThrow(() -> new UnexpectedScimDataException(
                                "can't create group member for group '%s' without id: ".formatted(displayName) + resource));
                KeycloakId userId = getScimResourceDao().findUserByExternalId(externalId)
                        .map(ScimResourceMapping::getIdAsKeycloakId).orElseThrow(() -> new InconsistentScimMappingException(
                                "can't find mapping for group member %s".formatted(externalId)));
                UserModel userModel = getKeycloakDao().getUserById(userId);
                userModel.joinGroup(group);
            }
        }
        return new KeycloakId(group.getId());
    }

    @Override
    protected boolean isMarkedToIgnore(GroupModel groupModel) {
        return BooleanUtils.TRUE.equals(groupModel.getFirstAttribute("scim-skip"));
    }

    @Override
    protected KeycloakId getId(GroupModel groupModel) {
        return new KeycloakId(groupModel.getId());
    }

    @Override
    protected Group scimRequestBodyForCreate(GroupModel groupModel) throws InconsistentScimMappingException {
        Set<KeycloakId> members = getKeycloakDao().getGroupMembers(groupModel);
        Group group = new Group();
        group.setExternalId(groupModel.getId());
        group.setDisplayName(groupModel.getName());
        for (KeycloakId member : members) {
            Member groupMember = new Member();
            Optional<ScimResourceMapping> optionalGroupMemberMapping = getScimResourceDao().findUserById(member);
            if (optionalGroupMemberMapping.isPresent()) {
                ScimResourceMapping groupMemberMapping = optionalGroupMemberMapping.get();
                EntityOnRemoteScimId externalIdAsEntityOnRemoteScimId = groupMemberMapping
                        .getExternalIdAsEntityOnRemoteScimId();
                groupMember.setValue(externalIdAsEntityOnRemoteScimId.asString());
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
    protected Group scimRequestBodyForUpdate(GroupModel groupModel, EntityOnRemoteScimId externalId)
            throws InconsistentScimMappingException {
        Group group = scimRequestBodyForCreate(groupModel);
        group.setId(externalId.asString());
        Meta meta = newMetaLocation(externalId);
        group.setMeta(meta);
        return group;
    }

    @Override
    protected boolean shouldIgnoreForScimSynchronization(GroupModel resource) {
        return false;
    }
}
