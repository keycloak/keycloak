import { Button, PageSection, Popover } from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom-v5-compat";

import { useHelp } from "../components/help-enabler/HelpHeader";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { useParams } from "../utils/useParams";
import type { ClientRoleParams } from "./routes/ClientRole";

export const UsersInRoleTab = () => {
  const navigate = useNavigate();
  const { realm } = useRealm();

  const { t } = useTranslation("roles");
  const { id, clientId } = useParams<ClientRoleParams>();

  const { adminClient } = useAdminClient();

  const loader = async (first?: number, max?: number) => {
    const role = await adminClient.roles.findOneById({ id: id });
    if (!role) {
      throw new Error(t("common:notFound"));
    }

    if (role.clientRole) {
      return adminClient.clients.findUsersWithRole({
        roleName: role.name!,
        id: clientId,
        first,
        max,
      });
    }

    return adminClient.roles.findUsersWithRole({
      name: role.name!,
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
        ariaLabelKey="roles:roleList"
        searchPlaceholderKey=""
        toolbarItem={
          enabled && (
            <Popover
              aria-label="Basic popover"
              position="bottom"
              bodyContent={
                <div>
                  {t("roles:whoWillAppearPopoverText")}
                  <Button
                    className="kc-groups-link"
                    variant="link"
                    onClick={() => navigate(`/${realm}/groups`)}
                  >
                    {t("common:groups")}
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
              footerContent={t("roles:whoWillAppearPopoverFooterText")}
            >
              <Button
                variant="link"
                className="kc-who-will-appear-button"
                key="who-will-appear-button"
                icon={<QuestionCircleIcon />}
              >
                {t("roles:whoWillAppearLinkText")}
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
                  {t("common:groups")}
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
            displayKey: "roles:userName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "email",
            displayKey: "roles:email",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "lastName",
            displayKey: "roles:lastName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "firstName",
            displayKey: "roles:firstName",
            cellFormatters: [upperCaseFormatter(), emptyFormatter()],
          },
        ]}
      />
    </PageSection>
  );
};
