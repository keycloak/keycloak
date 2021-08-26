import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import _ from "lodash";
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

import { KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";
import { useFetch, useAdminClient } from "../../context/auth/AdminClient";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { FilterIcon } from "@patternfly/react-icons";
import { Row, ServiceRole } from "./RoleMapping";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";

export type MappingType = "service-account" | "client-scope" | "user-fed";

type AddRoleMappingModalProps = {
  id: string;
  type: MappingType;
  name?: string;
  isRadio?: boolean;
  onAssign: (rows: Row[]) => void;
  onClose: () => void;
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
  onAssign,
  onClose,
}: AddRoleMappingModalProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();

  const [clients, setClients] = useState<ClientRole[]>([]);
  const [searchToggle, setSearchToggle] = useState(false);

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [selectedClients, setSelectedClients] = useState<ClientRole[]>([]);
  const [selectedRows, setSelectedRows] = useState<Row[]>([]);

  useFetch(
    async () => {
      const clients = await adminClient.clients.find();
      return (
        await Promise.all(
          clients.map(async (client) => {
            let roles: RoleRepresentation[] = [];

            switch (type) {
              case "service-account":
                roles = await adminClient.users.listAvailableClientRoleMappings(
                  {
                    id: id,
                    clientUniqueId: client.id!,
                  }
                );
                break;

              case "client-scope":
                roles =
                  await adminClient.clientScopes.listAvailableClientScopeMappings(
                    {
                      id,
                      client: client.id!,
                    }
                  );
                break;
              case "user-fed":
                roles = await adminClient.roles.find();
                break;
            }
            return {
              roles,
              client,
            };
          })
        )
      )
        .flat()
        .filter((row) => row.roles.length !== 0)
        .map((row) => {
          return { ...row.client, numberOfRoles: row.roles.length };
        });
    },
    (clients) => {
      setClients(clients);
    },
    []
  );

  useEffect(refresh, [searchToggle]);

  const removeClient = (client: ClientRole) => {
    setSelectedClients(selectedClients.filter((item) => item.id !== client.id));
  };

  const loader = async () => {
    const realmRolesSelected = _.findIndex(
      selectedClients,
      (client) => client.name === "realmRoles"
    );
    let selected = selectedClients;
    if (realmRolesSelected !== -1) {
      selected = selectedClients.filter(
        (client) => client.name !== "realmRoles"
      );
    }

    let availableRoles: RoleRepresentation[] = [];

    switch (type) {
      case "service-account":
        availableRoles = await adminClient.users.listAvailableRealmRoleMappings(
          {
            id,
          }
        );
        break;
      case "client-scope":
        availableRoles =
          await adminClient.clientScopes.listAvailableRealmScopeMappings({
            id,
          });
        break;
      case "user-fed":
        availableRoles = await adminClient.roles.find();
        break;
    }

    const realmRoles = availableRoles.map((role) => {
      return {
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
          let clientAvailableRoles: RoleRepresentation[] = [];

          switch (type) {
            case "service-account":
              clientAvailableRoles =
                await adminClient.users.listAvailableClientRoleMappings({
                  id,
                  clientUniqueId: client.id!,
                });
              break;
            case "client-scope":
              clientAvailableRoles =
                await adminClient.clientScopes.listAvailableClientScopeMappings(
                  { id, client: client.id! }
                );
              break;
            case "user-fed":
              clientAvailableRoles = await adminClient.clients.listRoles({
                id: client.id!,
              });
              break;
          }

          return clientAvailableRoles.map((role) => {
            return {
              role,
              client,
            };
          });
        })
      )
    ).flat();

    return [
      ...(realmRolesSelected !== -1 || selected.length === 0 ? realmRoles : []),
      ...roles,
    ];
  };

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
      title={t("assignRolesTo", { client: name })}
      isOpen={true}
      onClose={onClose}
      actions={[
        <Button
          data-testid="assign"
          key="confirm"
          isDisabled={selectedRows?.length === 0}
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
        key={key}
        onSelect={(rows) => setSelectedRows([...rows])}
        searchPlaceholderKey="clients:searchByRoleName"
        searchTypeComponent={
          <ToolbarItem>
            <Select
              toggleId="role"
              onToggle={() => setSearchToggle(!searchToggle)}
              isOpen={searchToggle}
              variant={isRadio ? SelectVariant.single : SelectVariant.checkbox}
              hasInlineFilter
              placeholderText={
                <>
                  <FilterIcon /> {t("filterByOrigin")}
                </>
              }
              isGrouped
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
                  onClick={() => {
                    removeClient(client);
                    refresh();
                  }}
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
        loader={loader}
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
