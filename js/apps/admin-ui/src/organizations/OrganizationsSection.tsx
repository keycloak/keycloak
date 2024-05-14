import {
  Badge,
  Button,
  ButtonVariant,
  Chip,
  ChipGroup,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { toAddOrganization } from "./routes/AddOrganization";
import { TableText } from "@patternfly/react-table";
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

const Domains = (org: any) => {
  const { t } = useTranslation();
  return (
    <ChipGroup
      numChips={2}
      expandedText={t("hide")}
      collapsedText={t("showRemaining")}
    >
      {org.domains.map((dn: string) => (
        <Chip key={dn} isReadOnly>
          {dn}
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

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [selectedOrg, setSelectedOrg] = useState();

  function loader() {
    return Promise.resolve([
      {
        id: 12,
        name: "test",
        domains: ["one.ch", "two.ch"],
        description: "my domain",
        enabled: false,
      },
    ]);
  }

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "organizationDelete",
    messageKey: "organizationDeleteConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.orginization.del({
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
            />
          }
        />
      </PageSection>
    </>
  );
}
