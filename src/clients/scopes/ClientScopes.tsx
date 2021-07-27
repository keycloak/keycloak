import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  KebabToggle,
  Select,
  ToolbarItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";
import type ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";
import type KeycloakAdminClient from "keycloak-admin";

import { useAdminClient } from "../../context/auth/AdminClient";
import { toUpperCase } from "../../util";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { AddScopeDialog } from "./AddScopeDialog";
import {
  clientScopeTypesSelectOptions,
  ClientScopeType,
  ClientScope,
  CellDropdown,
} from "../../components/client-scope/ClientScopeTypes";
import { useAlerts } from "../../components/alert/Alerts";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";

import "./client-scopes.css";

export type ClientScopesProps = {
  clientId: string;
  protocol: string;
};

type Row = ClientScopeRepresentation & {
  type: ClientScopeType;
  description: string;
};

const castAdminClient = (adminClient: KeycloakAdminClient) =>
  adminClient.clients as unknown as {
    [index: string]: Function;
  };

const changeScope = async (
  adminClient: KeycloakAdminClient,
  clientId: string,
  clientScope: ClientScopeRepresentation,
  type: ClientScopeType,
  changeTo: ClientScopeType
) => {
  await removeScope(adminClient, clientId, clientScope, type);
  await addScope(adminClient, clientId, clientScope, changeTo);
};

const removeScope = async (
  adminClient: KeycloakAdminClient,
  clientId: string,
  clientScope: ClientScopeRepresentation,
  type: ClientScopeType
) => {
  const typeToName = toUpperCase(type);
  await castAdminClient(adminClient)[`del${typeToName}ClientScope`]({
    id: clientId,
    clientScopeId: clientScope.id!,
  });
};

const addScope = async (
  adminClient: KeycloakAdminClient,
  clientId: string,
  clientScope: ClientScopeRepresentation,
  type: ClientScopeType
) => {
  const typeToName = toUpperCase(type);
  await castAdminClient(adminClient)[`add${typeToName}ClientScope`]({
    id: clientId,
    clientScopeId: clientScope.id!,
  });
};

type SearchType = "client" | "assigned";

export const ClientScopes = ({ clientId, protocol }: ClientScopesProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const [searchToggle, setSearchToggle] = useState(false);
  const [searchType, setSearchType] = useState<SearchType>("client");
  const [addToggle, setAddToggle] = useState(false);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [kebabOpen, setKebabOpen] = useState(false);

  const [rest, setRest] = useState<ClientScopeRepresentation[]>();
  const [selectedRows, setSelectedRows] = useState<Row[]>([]);

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const loader = async () => {
    const defaultClientScopes =
      await adminClient.clients.listDefaultClientScopes({ id: clientId });
    const optionalClientScopes =
      await adminClient.clients.listOptionalClientScopes({ id: clientId });
    const clientScopes = await adminClient.clientScopes.find();

    const find = (id: string) =>
      clientScopes.find((clientScope) => id === clientScope.id)!;

    const optional = optionalClientScopes.map((c) => {
      const scope = find(c.id!);
      return {
        ...c,
        type: ClientScope.optional,
        description: scope.description,
      } as Row;
    });

    const defaultScopes = defaultClientScopes.map((c) => {
      const scope = find(c.id!);
      return {
        ...c,
        type: ClientScope.default,
        description: scope.description,
      } as Row;
    });

    const rows = [...optional, ...defaultScopes];
    const names = rows.map((row) => row.name);
    setRest(
      clientScopes
        .filter((scope) => !names.includes(scope.name))
        .filter((scope) => scope.protocol === protocol)
    );

    return rows;
  };

  const TypeSelector = (scope: Row) => (
    <>
      <CellDropdown
        clientScope={scope}
        type={scope.type}
        onSelect={async (value) => {
          try {
            await changeScope(
              adminClient,
              clientId,
              scope,
              scope.type,
              value as ClientScope
            );
            addAlert(t("clientScopeSuccess"), AlertVariant.success);
            refresh();
          } catch (error) {
            addAlert(t("clientScopeError", { error }), AlertVariant.danger);
          }
        }}
      />
    </>
  );

  return (
    <>
      {rest && (
        <AddScopeDialog
          clientScopes={rest}
          open={addDialogOpen}
          toggleDialog={() => setAddDialogOpen(!addDialogOpen)}
          onAdd={async (scopes) => {
            try {
              await Promise.all(
                scopes.map(
                  async (scope) =>
                    await addScope(
                      adminClient,
                      clientId,
                      scope.scope,
                      scope.type
                    )
                )
              );
              addAlert(t("clientScopeSuccess"), AlertVariant.success);
              refresh();
            } catch (error) {
              addAlert(t("clientScopeError", { error }), AlertVariant.danger);
            }
          }}
        />
      )}

      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="clients:clientScopeList"
        searchPlaceholderKey="clients:searchByName"
        canSelectAll
        onSelect={(rows) => setSelectedRows([...rows])}
        searchTypeComponent={
          <Dropdown
            className="keycloak__client-scopes__searchtype"
            toggle={
              <DropdownToggle
                id="toggle-id"
                onToggle={() => setSearchToggle(!searchToggle)}
              >
                <FilterIcon /> {t(`clientScopeSearch.${searchType}`)}
              </DropdownToggle>
            }
            aria-label="Select Input"
            isOpen={searchToggle}
            dropdownItems={[
              <DropdownItem
                key="client"
                onClick={() => {
                  setSearchType("client");
                  setSearchToggle(false);
                }}
              >
                {t("clientScopeSearch.client")}
              </DropdownItem>,
              <DropdownItem
                key="assigned"
                onClick={() => {
                  setSearchType("assigned");
                  setSearchToggle(false);
                }}
              >
                {t("clientScopeSearch.assigned")}
              </DropdownItem>,
            ]}
          />
        }
        toolbarItem={
          <>
            <ToolbarItem>
              <Button onClick={() => setAddDialogOpen(true)}>
                {t("addClientScope")}
              </Button>
            </ToolbarItem>
            <ToolbarItem>
              <Select
                id="add-dropdown"
                key="add-dropdown"
                isOpen={addToggle}
                selections={[]}
                isDisabled={selectedRows.length === 0}
                placeholderText={t("changeTypeTo")}
                onToggle={() => setAddToggle(!addToggle)}
                onSelect={async (_, value) => {
                  try {
                    await Promise.all(
                      selectedRows.map((row) => {
                        return changeScope(
                          adminClient,
                          clientId,
                          { ...row },
                          row.type,
                          value as ClientScope
                        );
                      })
                    );
                    setAddToggle(false);
                    refresh();
                    addAlert(t("clientScopeSuccess"), AlertVariant.success);
                  } catch (error) {
                    addAlert(
                      t("clientScopeError", { error }),
                      AlertVariant.danger
                    );
                  }
                }}
              >
                {clientScopeTypesSelectOptions(t)}
              </Select>
            </ToolbarItem>
            <ToolbarItem>
              <Dropdown
                toggle={
                  <KebabToggle onToggle={() => setKebabOpen(!kebabOpen)} />
                }
                isOpen={kebabOpen}
                isPlain
                dropdownItems={[
                  <DropdownItem
                    key="deleteAll"
                    isDisabled={selectedRows.length === 0}
                    onClick={async () => {
                      try {
                        await Promise.all(
                          selectedRows.map(async (row) => {
                            await removeScope(
                              adminClient,
                              clientId,
                              { ...row },
                              row.type
                            );
                          })
                        );

                        setKebabOpen(false);
                        addAlert(
                          t("clientScopeRemoveSuccess"),
                          AlertVariant.success
                        );
                        refresh();
                      } catch (error) {
                        addAlert(
                          t("clientScopeRemoveError", { error }),
                          AlertVariant.danger
                        );
                      }
                    }}
                  >
                    {t("common:remove")}
                  </DropdownItem>,
                ]}
              />
            </ToolbarItem>
          </>
        }
        columns={[
          {
            name: "name",
            displayKey: "clients:assignedClientScope",
          },
          {
            name: "type",
            displayKey: "clients:assignedType",
            cellRenderer: TypeSelector,
          },
          { name: "description" },
        ]}
        actions={[
          {
            title: t("common:remove"),
            onRowClick: async (row) => {
              try {
                await removeScope(adminClient, clientId, row, row.type);
                addAlert(t("clientScopeRemoveSuccess"), AlertVariant.success);
                refresh();
              } catch (error) {
                addAlert(
                  t("clientScopeRemoveError", { error }),
                  AlertVariant.danger
                );
              }
              return true;
            },
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("clients:emptyClientScopes")}
            instructions={t("clients:emptyClientScopesInstructions")}
            primaryActionText={t("clients:emptyClientScopesPrimaryAction")}
            onPrimaryAction={() => setAddDialogOpen(true)}
          />
        }
      />
    </>
  );
};
