import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import {
  Badge,
  Button,
  ButtonVariant,
  Chip,
  ChipGroup,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { TableText } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { toAddOrganization } from "./routes/AddOrganization";
import { toEditOrganization } from "./routes/EditOrganization";

const OrgDetailLink = (organization: any) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  return (
    <TableText wrapModifier="truncate">
      <Link
        key={organization.id}
        to={toEditOrganization({
          realm,
          id: organization.id!,
          tab: "settings",
        })}
      >
        {organization.name}
        {!organization.enabled && (
          <Badge
            key={`${organization.id}-disabled`}
            isRead
            className="pf-v5-u-ml-sm"
          >
            {t("disabled")}
          </Badge>
        )}
      </Link>
    </TableText>
  );
};

const Domains = (org: OrganizationRepresentation) => {
  const { t } = useTranslation();
  return (
    <ChipGroup
      numChips={2}
      expandedText={t("hide")}
      collapsedText={t("showRemaining")}
    >
      {org.domains?.map((dn) => (
        <Chip key={dn.name} isReadOnly>
          {dn.name}
        </Chip>
      ))}
    </ChipGroup>
  );
};

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
        <KeycloakDataTable
          key={key}
          loader={loader}
          isPaginated
          ariaLabelKey="organizationList"
          searchPlaceholderKey="searchOrganization"
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
          actions={[
            {
              title: t("delete"),
              onRowClick: (org) => {
                setSelectedOrg(org);
                toggleDeleteDialog();
              },
            },
          ]}
          columns={[
            {
              name: "name",
              displayKey: "name",
              cellRenderer: OrgDetailLink,
            },
            {
              name: "domains",
              displayKey: "domains",
              cellRenderer: Domains,
            },
            {
              name: "description",
              displayKey: "description",
            },
          ]}
          emptyState={
            <ListEmptyState
              message={t("emptyOrganizations")}
              instructions={t("emptyOrganizationsInstructions")}
              primaryActionText={t("createOrganization")}
              onPrimaryAction={() => navigate(toAddOrganization({ realm }))}
            />
          }
        />
      </PageSection>
    </>
  );
}
