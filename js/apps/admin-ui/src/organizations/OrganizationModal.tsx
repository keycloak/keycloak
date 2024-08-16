import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { Button, Modal, ModalVariant } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";

type OrganizationModalProps = {
  onAdd: (orgs: OrganizationRepresentation[]) => Promise<void>;
  onClose: () => void;
};

export const OrganizationModal = ({
  onAdd,
  onClose,
}: OrganizationModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();

  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);

  const loader = async (first?: number, max?: number, search?: string) => {
    return await adminClient.organizations.find({ first, max, search });
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("joinOrganization")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="join"
          key="confirm"
          variant="primary"
          onClick={async () => {
            await onAdd(selectedRows);
            onClose();
          }}
        >
          {t("join")}
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
        canSelectAll
        onSelect={(rows) => setSelectedRows([...rows])}
        columns={[
          {
            name: "name",
            displayKey: "organizationName",
          },
          {
            name: "description",
          },
        ]}
      />
    </Modal>
  );
};
