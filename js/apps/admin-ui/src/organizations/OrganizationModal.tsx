import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  KeycloakDataTable,
  ListEmptyState,
} from "@keycloak/keycloak-ui-shared";
import { Button, Modal, ModalVariant } from "@patternfly/react-core";
import { TableText } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";

type OrganizationModalProps = {
  mode?: "join" | "send" | "add";
  existingOrgs: OrganizationRepresentation[];
  onAdd: (orgs: OrganizationRepresentation[]) => Promise<void>;
  onClose: () => void;
  isRadio?: boolean;
};

const TITLES = {
  join: "joinOrganization",
  send: "sendInvitation",
  add: "selectOrganization",
};

export const OrganizationModal = ({
  mode = "join",
  existingOrgs,
  onAdd,
  onClose,
  isRadio = false,
}: OrganizationModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();

  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);

  const loader = async (first?: number, max?: number, search?: string) => {
    const params = {
      first,
      search,
      max: max! + existingOrgs.length,
    };

    const orgs = await adminClient.organizations.find(params);

    return orgs;
  };

  const isRowDisabled = (row?: OrganizationRepresentation) => {
    return existingOrgs.some((org) => org.id === row?.id);
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t(TITLES[mode])}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid={mode === "add" ? "add" : "join"}
          key="confirm"
          variant="primary"
          onClick={async () => {
            await onAdd(selectedRows);
            onClose();
          }}
          isDisabled={selectedRows.length === 0}
        >
          {t(mode)}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant="link"
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        loader={loader}
        isPaginated
        ariaLabelKey="organizationsList"
        searchPlaceholderKey="searchOrganization"
        canSelectAll={!isRadio}
        onSelect={(rows) => setSelectedRows([...rows])}
        isRowDisabled={isRowDisabled}
        columns={[
          {
            name: "name",
            displayKey: "organizationName",
          },
          {
            name: "description",
            cellRenderer: (row) => (
              <TableText wrapModifier="truncate">{row.description}</TableText>
            ),
          },
        ]}
        isRadio={isRadio}
        emptyState={
          <ListEmptyState
            hasIcon={false}
            message={t("emptyOrganizations")}
            instructions={t("emptyOrganizationsInstructions")}
          /> /* This component doesn't ever get rendered on the user join org screen if there are
        no organizations so this doesn't actually change any functionality there.
        Empty message when searching already handled within KeycloakDataTable. */
        }
      />
    </Modal>
  );
};
