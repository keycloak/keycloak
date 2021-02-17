import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useErrorHandler } from "react-error-boundary";
import {
  IFormatter,
  IFormatterValueType,
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import {
  AlertVariant,
  Button,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  KebabToggle,
  Select,
  Spinner,
  ToolbarItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";
import ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";
import KeycloakAdminClient from "keycloak-admin";

import {
  useAdminClient,
  asyncStateFetch,
} from "../../context/auth/AdminClient";
import { toUpperCase } from "../../util";
import { TableToolbar } from "../../components/table-toolbar/TableToolbar";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { AddScopeDialog } from "./AddScopeDialog";
import {
  clientScopeTypesSelectOptions,
  ClientScopeType,
  ClientScope,
} from "./ClientScopeTypes";
import { useAlerts } from "../../components/alert/Alerts";

export type ClientScopesProps = {
  clientId: string;
  protocol: string;
};

const castAdminClient = (adminClient: KeycloakAdminClient) =>
  (adminClient.clients as unknown) as {
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

type CellDropdownProps = {
  clientScope: ClientScopeRepresentation;
  type: ClientScopeType;
  onSelect: (value: ClientScopeType) => void;
};

const CellDropdown = ({ clientScope, type, onSelect }: CellDropdownProps) => {
  const { t } = useTranslation("clients");
  const [open, setOpen] = useState(false);

  return (
    <Select
      key={clientScope.id}
      onToggle={() => setOpen(!open)}
      isOpen={open}
      selections={[type]}
      onSelect={(_, value) => {
        onSelect(value as ClientScopeType);
        setOpen(false);
      }}
    >
      {clientScopeTypesSelectOptions(t)}
    </Select>
  );
};

type SearchType = "client" | "assigned";

type TableRow = {
  selected: boolean;
  clientScope: ClientScopeRepresentation;
  type: ClientScopeType;
  cells: (string | undefined)[];
};

export const ClientScopes = ({ clientId, protocol }: ClientScopesProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const handleError = useErrorHandler();
  const { addAlert } = useAlerts();

  const [searchToggle, setSearchToggle] = useState(false);
  const [searchType, setSearchType] = useState<SearchType>("client");
  const [addToggle, setAddToggle] = useState(false);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [kebabOpen, setKebabOpen] = useState(false);

  const [rows, setRows] = useState<TableRow[]>();
  const [rest, setRest] = useState<ClientScopeRepresentation[]>();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  useEffect(() => {
    return asyncStateFetch(
      async () => {
        const defaultClientScopes = await adminClient.clients.listDefaultClientScopes(
          { id: clientId }
        );
        const optionalClientScopes = await adminClient.clients.listOptionalClientScopes(
          { id: clientId }
        );
        const clientScopes = await adminClient.clientScopes.find();

        const find = (id: string) =>
          clientScopes.find((clientScope) => id === clientScope.id)!;

        const optional = optionalClientScopes.map((c) => {
          const scope = find(c.id!);
          return {
            selected: false,
            clientScope: c,
            type: ClientScope.optional,
            cells: [c.name, c.id, scope.description],
          };
        });

        const defaultScopes = defaultClientScopes.map((c) => {
          const scope = find(c.id!);
          return {
            selected: false,
            clientScope: c,
            type: ClientScope.default,
            cells: [c.name, c.id, scope.description],
          };
        });

        const rows = [...optional, ...defaultScopes];
        const names = rows.map((row) => row.cells[0]);

        const rest = clientScopes
          .filter((scope) => !names.includes(scope.name))
          .filter((scope) => scope.protocol === protocol);
        return { rows, rest };
      },
      ({ rows, rest }) => {
        setRows(rows);
        setRest(rest);
      },
      handleError
    );
  }, [key]);

  const dropdown = (): IFormatter => (data?: IFormatterValueType) => {
    if (!data) {
      return <></>;
    }
    const row = rows?.find((row) => row.clientScope.id === data.toString())!;
    return (
      <CellDropdown
        clientScope={row.clientScope}
        type={row.type}
        onSelect={async (value) => {
          try {
            await changeScope(
              adminClient,
              clientId,
              row.clientScope,
              row.type,
              value
            );
            addAlert(t("clientScopeSuccess"), AlertVariant.success);
            await refresh();
          } catch (error) {
            addAlert(t("clientScopeError", { error }), AlertVariant.danger);
          }
        }}
      />
    );
  };

  const filterData = () => {};

  return (
    <>
      {!rows && (
        <div className="pf-u-text-align-center">
          <Spinner />
        </div>
      )}

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

      {rows && rows.length > 0 && (
        <TableToolbar
          searchTypeComponent={
            <Dropdown
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
          inputGroupName="clientsScopeToolbarTextInput"
          inputGroupPlaceholder={t("searchByName")}
          inputGroupOnChange={filterData}
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
                  placeholderText={t("changeTypeTo")}
                  onToggle={() => setAddToggle(!addToggle)}
                  onSelect={async (_, value) => {
                    try {
                      await Promise.all(
                        rows.map((row) => {
                          if (row.selected) {
                            return changeScope(
                              adminClient,
                              clientId,
                              row.clientScope,
                              row.type,
                              value as ClientScope
                            );
                          }
                          return Promise.resolve();
                        })
                      );
                      setAddToggle(false);
                      await refresh();
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
                  onSelect={() => {}}
                  toggle={
                    <KebabToggle onToggle={() => setKebabOpen(!kebabOpen)} />
                  }
                  isOpen={kebabOpen}
                  isPlain
                  dropdownItems={[
                    <DropdownItem
                      key="deleteAll"
                      isDisabled={
                        rows.filter((row) => row.selected).length === 0
                      }
                      onClick={async () => {
                        try {
                          await Promise.all(
                            rows.map(async (row) => {
                              if (row.selected) {
                                await removeScope(
                                  adminClient,
                                  clientId,
                                  row.clientScope,
                                  row.type
                                );
                              }
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
        >
          <Table
            onSelect={(_, isSelected, rowIndex) => {
              if (rowIndex === -1) {
                setRows(
                  rows.map((row) => {
                    row.selected = isSelected;
                    return row;
                  })
                );
              } else {
                rows[rowIndex].selected = isSelected;
                setRows([...rows]);
              }
            }}
            variant={TableVariant.compact}
            cells={[
              t("common:name"),
              { title: t("assignedType"), cellFormatters: [dropdown()] },
              t("common:description"),
            ]}
            rows={rows}
            actions={[
              {
                title: t("common:remove"),
                onClick: async (_, rowId) => {
                  try {
                    await removeScope(
                      adminClient,
                      clientId,
                      rows[rowId].clientScope,
                      rows[rowId].type
                    );
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
                },
              },
            ]}
            aria-label={t("clientScopeList")}
          >
            <TableHeader />
            <TableBody />
          </Table>
        </TableToolbar>
      )}
      {rows && rows.length === 0 && (
        <ListEmptyState
          message={t("clients:emptyClientScopes")}
          instructions={t("clients:emptyClientScopesInstructions")}
          primaryActionText={t("clients:emptyClientScopesPrimaryAction")}
          onPrimaryAction={() => setAddDialogOpen(true)}
        />
      )}
    </>
  );
};
