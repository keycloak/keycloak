import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Badge,
  Button,
  Chip,
  ChipGroup,
  Divider,
  Modal,
  ModalVariant,
  Select,
  SelectGroup,
  SelectOption,
  SelectVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";
import { useFetch, useAdminClient } from "../../context/auth/AdminClient";
import {
  castAdminClient,
  mapping,
  ResourcesKey,
  Row,
  ServiceRole,
} from "./RoleMapping";
import { KeycloakSpinner } from "../keycloak-spinner/KeycloakSpinner";

type AddRoleMappingModalProps = {
  id: string;
  type: ResourcesKey;
  name?: string;
  isRadio?: boolean;
  onAssign: (rows: Row[]) => void;
  onClose: () => void;
  isLDAPmapper?: boolean;
};

type ClientRole = ClientRepresentation & {
  numberOfRoles: number;
};

const realmRole = {
  name: "realmRoles",
} as ClientRepresentation;

export const AddRoleMappingModal = ({
  id,
  name,
  type,
  isRadio = false,
  isLDAPmapper,
  onAssign,
  onClose,
}: AddRoleMappingModalProps) => {
  const { t } = useTranslation("common");
  const { adminClient } = useAdminClient();

  const [clients, setClients] = useState<ClientRole[]>([]);
  const [searchToggle, setSearchToggle] = useState(false);

  const [selectedClients, setSelectedClients] = useState<ClientRole[]>([]);
  const [selectedRows, setSelectedRows] = useState<Row[]>([]);

  const [data, setData] = useState<Row[]>();

  const mapType = mapping.find((m) => m.resource === type)!;

  useFetch(
    async () => {
      const clients = await adminClient.clients.find();
      return (
        await Promise.all(
          clients.map(async (client) => {
            const roles = await castAdminClient(
              adminClient,
              mapType.resource === "roles" ? "clients" : mapType.resource
            )[mapType.functions.list[0]]({
              id: mapType.resource === "roles" ? client.id : id,
              clientUniqueId: client.id,
              client: client.id,
            });

            return {
              roles,
              client,
            };
          })
        )
      )
        .flat()
        .filter((row) => row.roles.length !== 0)
        .map((row) => ({ ...row.client, numberOfRoles: row.roles.length }));
    },
    (clients) => {
      setClients(clients);
    },
    []
  );

  useFetch(
    async () => {
      const realmRolesSelected = selectedClients.some(
        (client) => client.name === "realmRoles"
      );
      let selected = selectedClients;
      if (realmRolesSelected) {
        selected = selectedClients.filter(
          (client) => client.name !== "realmRoles"
        );
      }

      const availableRoles = await castAdminClient(
        adminClient,
        mapType.resource
      )[mapType.functions.list[1]]({
        id,
      });

      const realmRoles = availableRoles.map((role) => {
        return {
          id: role.id,
          role,
          client: undefined,
        };
      });

      const allClients =
        selectedClients.length !== 0
          ? selected
          : await adminClient.clients.find();

      const roles = (
        await Promise.all(
          allClients.map(async (client) => {
            const clientAvailableRoles = await castAdminClient(
              adminClient,
              mapType.resource === "roles" ? "clients" : mapType.resource
            )[mapType.functions.list[0]]({
              id: mapType.resource === "roles" ? client.id : id,
              client: client.id,
              clientUniqueId: client.id,
            });

            return clientAvailableRoles.map((role) => {
              return {
                id: role.id,
                role,
                client,
              };
            });
          })
        )
      ).flat();

      return [
        ...(realmRolesSelected || selected.length === 0 ? realmRoles : []),
        ...roles,
      ];
    },
    setData,
    [selectedClients]
  );

  const removeClient = (client: ClientRole) => {
    setSelectedClients(selectedClients.filter((item) => item.id !== client.id));
  };

  if (!data) {
    return <KeycloakSpinner />;
  }

  const createSelectGroup = (clients: ClientRepresentation[]) => [
    <SelectGroup key="role" label={t("realmRoles")}>
      <SelectOption key="realmRoles" value={realmRole}>
        {t("realmRoles")}
      </SelectOption>
    </SelectGroup>,
    <Divider key="divider" />,
    <SelectGroup key="group" label={t("clients")}>
      {clients.map((client) => (
        <SelectOption key={client.id} value={client}>
          {client.clientId}
        </SelectOption>
      ))}
    </SelectGroup>,
  ];

  return (
    <Modal
      variant={ModalVariant.large}
      title={
        isLDAPmapper ? t("assignRole") : t("assignRolesTo", { client: name })
      }
      isOpen={true}
      onClose={onClose}
      actions={[
        <Button
          data-testid="assign"
          key="confirm"
          isDisabled={selectedRows.length === 0}
          variant="primary"
          onClick={() => {
            onAssign(selectedRows);
            onClose();
          }}
        >
          {t("assign")}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant="link"
          onClick={onClose}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        onSelect={(rows) => setSelectedRows([...rows])}
        searchPlaceholderKey="clients:searchByRoleName"
        searchTypeComponent={
          <ToolbarItem>
            <Select
              toggleId="role"
              onToggle={setSearchToggle}
              isOpen={searchToggle}
              variant={isRadio ? SelectVariant.single : SelectVariant.checkbox}
              hasInlineFilter
              placeholderText={
                <>
                  <FilterIcon /> {t("clients:filterByOrigin")}
                </>
              }
              isGrouped
              isCheckboxSelectionBadgeHidden
              onFilter={(evt) => {
                const value = evt?.target.value || "";
                return createSelectGroup(
                  clients.filter((client) => client.clientId?.includes(value))
                );
              }}
              selections={selectedClients}
              onClear={() => setSelectedClients([])}
              onSelect={(_, selection) => {
                const client = selection as ClientRole;
                if (selectedClients.includes(client)) {
                  removeClient(client);
                } else {
                  setSelectedClients([...selectedClients, client]);
                }
              }}
            >
              {createSelectGroup(clients)}
            </Select>
          </ToolbarItem>
        }
        subToolbar={
          <ToolbarItem widths={{ default: "100%" }}>
            <ChipGroup>
              {selectedClients.map((client) => (
                <Chip
                  key={`chip-${client.id}`}
                  onClick={() => removeClient(client)}
                >
                  {client.clientId || t("realmRoles")}
                  <Badge isRead={true}>{client.numberOfRoles}</Badge>
                </Chip>
              ))}
            </ChipGroup>
          </ToolbarItem>
        }
        canSelectAll
        isRadio={isRadio}
        loader={data}
        ariaLabelKey="clients:roles"
        columns={[
          {
            name: "name",
            cellRenderer: ServiceRole,
          },
          {
            name: "role.description",
            displayKey: t("description"),
          },
        ]}
      />
    </Modal>
  );
};
