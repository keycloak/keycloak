import { Button, PageSection, Popover } from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useHelp } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import type { ClientRoleParams } from "../clients/routes/ClientRole";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { useParams } from "../utils/useParams";

export const UsersInRoleTab = () => {
  const { adminClient } = useAdminClient();

  const navigate = useNavigate();
  const { realm } = useRealm();

  const { t } = useTranslation();
  const { id, clientId } = useParams<ClientRoleParams>();

  const loader = async (first?: number, max?: number) => {
    const role = await adminClient.roles.findOneById({ id: id });
    if (!role) {
      throw new Error(t("notFound"));
    }

    if (role.clientRole) {
      return adminClient.clients.findUsersWithRole({
        roleName: role.name!,
        id: clientId,
        briefRepresentation: true,
        first,
        max,
      });
    }

    return adminClient.roles.findUsersWithRole({
      name: role.name!,
      briefRepresentation: true,
      first,
      max,
    });
  };

  const { enabled } = useHelp();

  return (
    <PageSection data-testid="users-page" variant="light">
      <KeycloakDataTable
        isPaginated
        loader={loader}
        ariaLabelKey="roleList"
        searchPlaceholderKey=""
        data-testid="users-in-role-table"
        toolbarItem={
          enabled && (
            <Popover
              aria-label="Basic popover"
              position="bottom"
              bodyContent={
                <div>
                  {t("whoWillAppearPopoverTextRoles")}
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
            message={t("noDirectUsers")}
            instructions={
              <div>
                {t("noUsersEmptyStateDescription")}
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
