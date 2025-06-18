import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useParams } from "../utils/useParams";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { OrganizationRoleMapping } from "./OrganizationRoleMapping";
import { OrganizationMemberRoleMappingParams } from "./routes/OrganizationMemberRoleMapping";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useState } from "react";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";

export default function OrganizationMemberRoleMappingDetails() {
  const { t } = useTranslation();
  const { orgId, userId } = useParams<OrganizationMemberRoleMappingParams>();
  const { adminClient } = useAdminClient();
  const [user, setUser] = useState<UserRepresentation>();
  const [orgName, setOrgName] = useState<string>("");

  useFetch(
    async () => {
      const [userInfo, orgInfo] = await Promise.all([
        adminClient.users.findOne({ id: userId }),
        adminClient.organizations.findOne({ id: orgId }),
      ]);
      return { user: userInfo, org: orgInfo };
    },
    ({ user, org }) => {
      if (!user) {
        throw new Error(t("userNotFound"));
      }
      if (!org) {
        throw new Error(t("organizationNotFound"));
      }
      setUser(user);
      setOrgName(org.name || org.alias || "");
    },
    [userId, orgId],
  );

  if (!user) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ViewHeader
        titleKey="organizationMemberRoleMapping"
        subKey={t("organizationMemberRoleMappingSubTitle", {
          user: user.username,
          organization: orgName,
        })}
      />
      <PageSection variant="light">
        <OrganizationRoleMapping
          orgId={orgId}
          userId={userId}
          name={user.username!}
        />
      </PageSection>
    </>
  );
}
