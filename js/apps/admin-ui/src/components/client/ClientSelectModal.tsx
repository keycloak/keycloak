import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ClientQuery } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { Button, Modal, ModalVariant } from "@patternfly/react-core";
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

  const loader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<ClientRepresentation[]> => {
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
      data-testid="select-client-modal"
      variant={ModalVariant.large}
      title={t("selectClient")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="confirm"
          key="confirm"
          isDisabled={selectedClients.length === 0}
          variant="primary"
          onClick={() => {
            onSelect(selectedClients);
            onClose();
          }}
        >
          {t("add")}
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
        onSelect={(rows) => setSelectedClients([...rows])}
        searchPlaceholderKey="searchForClient"
        isPaginated
        canSelectAll={!isRadio}
        isRadio={isRadio}
        loader={loader}
        ariaLabelKey="clientList"
        columns={[{ name: "clientId" }, { name: "name" }]}
      />
    </Modal>
  );
};
