import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import {
  ListEmptyState,
  OrganizationTable,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { toEditOrganization } from "../organizations/routes/EditOrganization";
import { toAddOrganization } from "./routes/AddOrganization";

export default function OrganizationSection() {
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [selectedOrg, setSelectedOrg] = useState<OrganizationRepresentation>();

  async function loader(first?: number, max?: number, search?: string) {
    return await adminClient.organizations.find({ first, max, search });
  }

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "organizationDelete",
    messageKey: "organizationDeleteConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.organizations.delById({
          id: selectedOrg!.id!,
        });
        addAlert(t("organizationDeletedSuccess"));
        refresh();
      } catch (error) {
        addError("organizationDeleteError", error);
      }
    },
  });

  return (
    <>
      <ViewHeader
        titleKey="organizationsList"
        subKey="organizationsExplain"
        divider
      />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <DeleteConfirm />
        <OrganizationTable
          link={({ organization, children }) => (
            <Link
              key={organization.id}
              to={toEditOrganization({
                realm,
                id: organization.id!,
                tab: "settings",
              })}
            >
              {children}
            </Link>
          )}
          key={key}
          loader={loader}
          searchPlaceholderKey="searchOrganization"
          isPaginated
          toolbarItem={
            <ToolbarItem>
              <Button
                data-testid="addOrganization"
                component={(props) => (
                  <Link {...props} to={toAddOrganization({ realm })} />
                )}
              >
                {t("createOrganization")}
              </Button>
            </ToolbarItem>
          }
          onDelete={(org) => {
            setSelectedOrg(org);
            toggleDeleteDialog();
          }}
        >
          <ListEmptyState
            message={t("emptyOrganizations")}
            instructions={t("emptyOrganizationsInstructions")}
            primaryActionText={t("createOrganization")}
            onPrimaryAction={() => navigate(toAddOrganization({ realm }))}
          />
        </OrganizationTable>
      </PageSection>
    </>
  );
}
