import { Button, ToolbarItem } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { useRealm } from "../context/realm-context/RealmContext";
import { OrganizationTable } from "../organizations/OrganizationTable";
import { toAddOrganization } from "../organizations/routes/AddOrganization";
import { UserParams } from "./routes/User";

export const Organizations = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { id } = useParams<UserParams>();

  return (
    <OrganizationTable
      loader={() =>
        adminClient.organizations.memberOrganizations({
          userId: id!,
        })
      }
      toolbarItem={
        <>
          <ToolbarItem>
            <Button
              data-testid="joinOrganization"
              component={(props) => (
                <Link {...props} to={toAddOrganization({ realm })} />
              )}
            >
              {t("joinOrganization")}
            </Button>
          </ToolbarItem>
          <ToolbarItem>
            <Button
              data-testid="removeOrganization"
              variant="secondary"
              component={(props) => (
                <Link {...props} to={toAddOrganization({ realm })} />
              )}
            >
              {t("remove")}
            </Button>
          </ToolbarItem>
        </>
      }
    >
      <ListEmptyState
        message={t("emptyUserOrganizations")}
        instructions={t("emptyUserOrganizationsInstructions")}
        secondaryActions={[
          {
            text: t("joinOrganization"),
            onClick: () => alert("join organization"),
          },
          {
            text: t("sendInvitation"),
            onClick: () => alert("send invitation"),
          },
        ]}
      />
    </OrganizationTable>
  );
};
