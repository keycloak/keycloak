package org.keycloak.protocol.oidc.mappers;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.representations.IDToken;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

public class GroupMembershipMapperTest {
    @Test
    public void shouldGenerateNestedJsonStructure() {

        IDToken token = new IDToken();
        ProtocolMapperModel protocolMapperModel = newProtocolMapper(true);
        UserSessionModel userSessionModel = newUserSessionWithGroups("group1", "group2");

        GroupMembershipMapper groupMembershipMapper = new GroupMembershipMapper();
        groupMembershipMapper.setClaim(token, protocolMapperModel, userSessionModel);

        Map<String, Object> otherClaims = token.getOtherClaims();
        assertThat(otherClaims, Matchers.hasKey("a"));
        assertThat((Map<String, Object>) otherClaims.get("a"), Matchers.hasKey("b"));
        assertThat(((Map<?, List<?>>) otherClaims.get("a")).get("b"), Matchers.contains("group1", "group2"));
    }

    private ProtocolMapperModel newProtocolMapper(boolean multivalued) {
        ProtocolMapperModel protocolMapperModel = GroupMembershipMapper.create("name", "a.b", false, "", true, false);
        if (multivalued)
            protocolMapperModel.getConfig().put(ProtocolMapperUtils.MULTIVALUED, "true");
        return protocolMapperModel;
    }

    private UserSessionModel newUserSessionWithGroups(String ...groupNames) {
        UserSessionModel userSessionModel = Mockito.mock(UserSessionModel.class, RETURNS_DEEP_STUBS);
        Set<GroupModel> groups = groups(groupNames);
        Mockito.when(userSessionModel.getUser().getGroups()).thenReturn(groups);
        return userSessionModel;
    }

    private Set<GroupModel> groups(String ...groupNames) {
        return Arrays.stream(groupNames).map(name -> {
            GroupModel groupModel = Mockito.mock(GroupModel.class);
            Mockito.when(groupModel.getName()).thenReturn(name);

            return groupModel;
        }).collect(Collectors.toSet());
    }
}
