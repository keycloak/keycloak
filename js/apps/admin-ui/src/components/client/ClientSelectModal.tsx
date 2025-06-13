import ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { ClientQuery } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { Button, Modal } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";

type ClientSelectModalProps = {
  onClose: () => void;
  onSelect: (clients: ClientRepresentation[]) => void;
  isRadio?: boolean;
};

export const ClientSelectModal = ({
  onClose,
  onSelect,
  isRadio = false,
}: ClientSelectModalProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const [selectedClients, setSelectedClients] = useState<
    ClientRepresentation[]
  >([]);

  const loader = (first?: number, max?: number, search?: string) => {
    const params: ClientQuery = {
      first: first!,
      max: max!,
    };

    if (search) {
      params.clientId = search;
      params.search = true;
    }

    return adminClient.clients.find(params);
  };

  return (
    <Modal
      title={t("selectClient")}
      variant="medium"
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="confirm"
          key="confirm"
          onClick={() => {
            onSelect(selectedClients);
            onClose();
          }}
        >
          {t("add")}
        </Button>,
        <Button key="close" onClick={onClose} variant="secondary">
          {t("close")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        searchPlaceholderKey="searchForClient"
        isPaginated
        isRadio={isRadio}
        ariaLabelKey="clientList"
        canSelectAll
        onSelect={setSelectedClients}
        loader={loader}
        columns={[{ name: "clientId" }, { name: "name" }]}
      />
    </Modal>
  );
};
