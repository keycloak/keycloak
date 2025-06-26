import { useState } from "react";
import {
    KeycloakDataTable,
  } from "@keycloak/keycloak-ui-shared";
  import {
    TextContent,
    Text,
    EmptyState,
    Button,
    Modal,
    ModalVariant,
  } from "@patternfly/react-core";
  import { useTranslation } from "react-i18next";
  import type { ClientQuery } from "@keycloak/keycloak-admin-client/lib/resources/clients";
  import { useAdminClient } from "../admin-client";
  import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";

type GenerateDefaultUserContextModalProps = {
  onSubmit: (rows: ClientRepresentation[]) => void;
  onClose: () => void;
};


export const GenerateDefaultUserContextModal = ({
    onSubmit,
    onClose,

}: GenerateDefaultUserContextModalProps) => {
    const { t } = useTranslation();
      const { adminClient } = useAdminClient();
    
    const [selectedRows, setSelectedRows] = useState<ClientRepresentation[]>([]);
    const [key, setKey] = useState(0);
    const refresh = () => setKey(key + 1);


    const columns = [
        {
        name: 'Client ID',
        displayKey: 'Client ID',
        cellRenderer: (row: ClientRepresentation) => row.clientId!
        }
    ];

    const loader = async (first?: number, max?: number, search?: string) => {
        const params: ClientQuery = {
          first: first!,
          max: max!,
        };
        if (search) {
          params.clientId = search;
          params.search = true;
        }
        return adminClient.clients.find({ ...params });
      };

    return (
        <Modal
        variant={ModalVariant.large}
        title={
            "Generate Default User Contexts For selected Client/s"
        }
        isOpen
        onClose={onClose}
        actions={[
            <Button
            data-testid="Generate"
            key="confirm"
            isDisabled={selectedRows.length === 0}
            variant="primary"
            onClick={() => {
                onSubmit(selectedRows);
                onClose();
            }}
            >
            {t("Submit")}
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
            key={key}
            onSelect={(rows) => setSelectedRows([...rows])}
            isPaginated={false}
            canSelectAll
            isRadio={false}
            loader={loader}
            ariaLabelKey="clients"
            columns={columns}
        />
        </Modal>
    );
}