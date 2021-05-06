import React, { useContext } from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button, PageSection, Popover } from "@patternfly/react-core";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { upperCaseFormatter, emptyFormatter } from "../util";
import { useAdminClient } from "../context/auth/AdminClient";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { useRealm } from "../context/realm-context/RealmContext";
import { HelpContext } from "../components/help-enabler/HelpHeader";

export const UsersInRoleTab = () => {
  const history = useHistory();
  const { realm } = useRealm();

  const { t } = useTranslation("roles");

  const { id } = useParams<{ id: string }>();

  const adminClient = useAdminClient();

  const loader = async (first?: number, max?: number) => {
    const role = await adminClient.roles.findOneById({ id: id });
    const usersWithRole = await adminClient.roles.findUsersWithRole({
      name: role.name!,
      first: first!,
      max: max!,
    });
    return usersWithRole || [];
  };

  const { enabled } = useContext(HelpContext);

  return (
    <>
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
                      onClick={() => history.push(`/${realm}/groups`)}
                    >
                      {t("groups")}
                    </Button>
                    {t("or")}
                    <Button
                      className="kc-users-link"
                      variant="link"
                      onClick={() => history.push(`/${realm}/users`)}
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
                    onClick={() => history.push(`/${realm}/groups`)}
                  >
                    {t("groups")}
                  </Button>
                  {t("or")}
                  <Button
                    className="kc-users-link-empty-state"
                    variant="link"
                    onClick={() => history.push(`/${realm}/users`)}
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
    </>
  );
};
