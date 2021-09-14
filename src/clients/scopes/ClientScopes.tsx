import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  Dropdown,
  DropdownItem,
  KebabToggle,
  ToolbarItem,
} from "@patternfly/react-core";
import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";

import { useAdminClient } from "../../context/auth/AdminClient";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { AddScopeDialog } from "./AddScopeDialog";
import {
  ClientScope,
  CellDropdown,
  AllClientScopes,
  AllClientScopeType,
  changeClientScope,
  addClientScope,
  removeClientScope,
} from "../../components/client-scope/ClientScopeTypes";
import { useAlerts } from "../../components/alert/Alerts";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import {
  nameFilter,
  SearchDropdown,
  SearchToolbar,
  SearchType,
  typeFilter,
} from "../../client-scopes/details/SearchFilter";

import "./client-scopes.css";
import { ChangeTypeDropdown } from "../../client-scopes/ChangeTypeDropdown";

export type ClientScopesProps = {
  clientId: string;
  protocol: string;
};

export type Row = ClientScopeRepresentation & {
  type: AllClientScopeType;
  description?: string;
};

export const ClientScopes = ({ clientId, protocol }: ClientScopesProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [searchType, setSearchType] = useState<SearchType>("name");

  const [searchTypeType, setSearchTypeType] = useState<AllClientScopes>(
    AllClientScopes.none
  );

  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [kebabOpen, setKebabOpen] = useState(false);

  const [rest, setRest] = useState<ClientScopeRepresentation[]>();
  const [selectedRows, setSelectedRows] = useState<Row[]>([]);

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const loader = async (first?: number, max?: number, search?: string) => {
    const defaultClientScopes =
      await adminClient.clients.listDefaultClientScopes({ id: clientId });
    const optionalClientScopes =
      await adminClient.clients.listOptionalClientScopes({ id: clientId });
    const clientScopes = await adminClient.clientScopes.find();

    const find = (id: string) =>
      clientScopes.find((clientScope) => id === clientScope.id)!;

    const optional = optionalClientScopes.map((c) => {
      const scope = find(c.id!);
      const row: Row = {
        ...c,
        type: ClientScope.optional,
        description: scope.description,
      };
      return row;
    });

    const defaultScopes = defaultClientScopes.map((c) => {
      const scope = find(c.id!);
      const row: Row = {
        ...c,
        type: ClientScope.default,
        description: scope.description,
      };
      return row;
    });

    const rows = [...optional, ...defaultScopes];
    const names = rows.map((row) => row.name);
    setRest(
      clientScopes
        .filter((scope) => !names.includes(scope.name))
        .filter((scope) => scope.protocol === protocol)
    );

    const filter =
      searchType === "name" ? nameFilter(search) : typeFilter(searchTypeType);
    return rows.filter(filter).slice(first, max);
  };

  const TypeSelector = (scope: Row) => (
    <CellDropdown
      clientScope={scope}
      type={scope.type}
      onSelect={async (value) => {
        try {
          await changeClientScope(
            adminClient,
            clientId,
            scope,
            scope.type,
            value as ClientScope
          );
          addAlert(t("clientScopeSuccess"), AlertVariant.success);
          refresh();
        } catch (error) {
          addError("clients:clientScopeError", error);
        }
      }}
    />
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
                    await addClientScope(
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
              addError("clients:clientScopeError", error);
            }
          }}
        />
      )}

      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="clients:clientScopeList"
        searchPlaceholderKey={
          searchType === "name" ? "clients:searchByName" : undefined
        }
        canSelectAll
        isPaginated
        isSearching={searchType === "type"}
        onSelect={(rows) => setSelectedRows([...rows])}
        searchTypeComponent={
          <SearchDropdown
            searchType={searchType}
            onSelect={(searchType) => setSearchType(searchType)}
          />
        }
        toolbarItem={
          <>
            <SearchToolbar
              searchType={searchType}
              type={searchTypeType}
              onSelect={(searchType) => setSearchType(searchType)}
              onType={(value) => {
                setSearchTypeType(value);
                refresh();
              }}
            />
            <ToolbarItem>
              <Button onClick={() => setAddDialogOpen(true)}>
                {t("addClientScope")}
              </Button>
            </ToolbarItem>
            <ToolbarItem>
              <ChangeTypeDropdown
                clientId={clientId}
                selectedRows={selectedRows}
                refresh={refresh}
              />
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
                            await removeClientScope(
                              adminClient,
                              clientId,
                              { ...row },
                              row.type as ClientScope
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
                        addError("clients:clientScopeRemoveError", error);
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
                await removeClientScope(
                  adminClient,
                  clientId,
                  row,
                  row.type as ClientScope
                );
                addAlert(t("clientScopeRemoveSuccess"), AlertVariant.success);
                refresh();
              } catch (error) {
                addError("clients:clientScopeRemoveError", error);
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
