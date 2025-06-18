import { Button, PageSection, Popover } from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useHelp } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { emptyFormatter, upperCaseFormatter } from "../util";
import type { OrganizationRoleParams } from "./routes/OrganizationRole";
import { useParams } from "../utils/useParams";

export const OrganizationRoleMembersTab = () => {
  const { adminClient } = useAdminClient();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const { orgId, roleId } = useParams<OrganizationRoleParams>();

  const loader = async (first?: number, max?: number) => {
    if (!orgId || !roleId) {
      return [];
    }

    try {
      return await adminClient.organizations.listRoleMembers({
        orgId,
        roleId,
      });
    } catch (error) {
      console.error("Error loading organization role members:", error);
      return [];
    }
  };

  const { enabled } = useHelp();

  return (
    <PageSection data-testid="organization-role-members-page" variant="light">
      <KeycloakDataTable
        isPaginated
        loader={loader}
        ariaLabelKey="organizationRoleMembers"
        searchPlaceholderKey=""
        data-testid="organization-role-members-table"
        toolbarItem={
          enabled && (
            <Popover
              aria-label="Organization role members popover"
              position="bottom"
              bodyContent={
                <div>
                  {t("organizationRoleMembersEmpty")}
                  <Button
                    className="kc-groups-link"
                    variant="link"
                    onClick={() => navigate(`/${realm}/groups`)}
                  >
                    {t("groups")}
                  </Button>
                  {t("or")}
                  <Button
                    className="kc-users-link"
                    variant="link"
                    onClick={() => navigate(`/${realm}/users`)}
                  >
                    {t("users")}.
                  </Button>
                </div>
              }
              footerContent={t("whoWillAppearPopoverFooterText")}
            >
              <Button
                variant="link"
                className="kc-who-will-appear-button"
                key="who-will-appear-button"
                icon={<QuestionCircleIcon />}
              >
                {t("whoWillAppearLinkTextRoles")}
              </Button>
            </Popover>
          )
        }
        emptyState={
          <ListEmptyState
            hasIcon={true}
            message={t("noOrganizationRoleMembers")}
            instructions={
              <div>
                {t("noOrganizationRoleMembersInstructions")}
                <Button
                  className="kc-groups-link-empty-state"
                  variant="link"
                  onClick={() => navigate(`/${realm}/groups`)}
                >
                  {t("groups")}
                </Button>
                {t("or")}
                <Button
                  className="kc-users-link-empty-state"
                  variant="link"
                  onClick={() => navigate(`/${realm}/users`)}
                >
                  {t("users")}
                </Button>
                {t("noUsersEmptyStateDescriptionContinued")}
              </div>
            }
          />
        }
        columns={[
          {
            name: "username",
            displayKey: "userName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "email",
            displayKey: "email",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "lastName",
            displayKey: "lastName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "firstName",
            displayKey: "firstName",
            cellFormatters: [upperCaseFormatter(), emptyFormatter()],
          },
        ]}
      />
    </PageSection>
  );
};
