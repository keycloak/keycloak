import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  ToolbarItem,
} from "@patternfly/react-core";
import { EllipsisVIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { ChangeTypeDropdown } from "../../client-scopes/ChangeTypeDropdown";
import {
  SearchDropdown,
  SearchToolbar,
  SearchType,
  nameFilter,
  typeFilter,
} from "../../client-scopes/details/SearchFilter";
import {
  AllClientScopeType,
  AllClientScopes,
  CellDropdown,
  ClientScope,
  addClientScope,
  changeClientScope,
  removeClientScope,
} from "../../components/client-scope/ClientScopeTypes";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { Action, KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";
import { translationFormatter } from "../../utils/translationFormatter";
import useLocaleSort, { mapByKey } from "../../utils/useLocaleSort";
import { toDedicatedScope } from "../routes/DedicatedScopeDetails";
import { AddScopeDialog } from "./AddScopeDialog";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { PROTOCOL_OIDC, PROTOCOL_OID4VC } from "../constants";

import "./client-scopes.css";

export type ClientScopesProps = {
  clientId: string;
  protocol: string;
  clientName: string;
  fineGrainedAccess?: boolean;
};

export type Row = ClientScopeRepresentation & {
  type: AllClientScopeType;
  description?: string;
};

const DEDICATED_ROW = "dedicated";

type TypeSelectorProps = Row & {
  clientId: string;
  fineGrainedAccess?: boolean;
  refresh: () => void;
};

const TypeSelector = ({
  clientId,
  refresh,
  fineGrainedAccess,
  ...scope
}: TypeSelectorProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const { hasAccess } = useAccess();

  const isDedicatedRow = (value: Row) => value.id === DEDICATED_ROW;
  const isManager = hasAccess("manage-clients") || fineGrainedAccess;

  return (
    <CellDropdown
      isDisabled={isDedicatedRow(scope) || !isManager}
      clientScope={scope}
      type={scope.type}
      onSelect={async (value) => {
        try {
          await changeClientScope(
            adminClient,
            clientId,
            scope,
            scope.type,
            value as ClientScope,
          );
          addAlert(t("clientScopeSuccess"), AlertVariant.success);
          refresh();
        } catch (error) {
          addError("clientScopeError", error);
        }
      }}
    />
  );
};

export const ClientScopes = ({
  clientId,
  protocol,
  clientName,
  fineGrainedAccess,
}: ClientScopesProps) => {
  const { adminClient } = useAdminClient();
  const isFeatureEnabled = useIsFeatureEnabled();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const localeSort = useLocaleSort();

  const [searchType, setSearchType] = useState<SearchType>("name");

  const [searchTypeType, setSearchTypeType] = useState<AllClientScopes>(
    AllClientScopes.none,
  );

  const [addDialogOpen, setAddDialogOpen] = useState(false);

  const [rest, setRest] = useState<ClientScopeRepresentation[]>();
  const [selectedRows, setSelectedRowState] = useState<Row[]>([]);
  const setSelectedRows = (rows: Row[]) =>
    setSelectedRowState(rows.filter(({ id }) => id !== DEDICATED_ROW));
  const [kebabOpen, setKebabOpen] = useState(false);

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const isDedicatedRow = (value: Row) => value.id === DEDICATED_ROW;

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients") || fineGrainedAccess;
  const isViewer = hasAccess("view-clients") || fineGrainedAccess;

  const loader = async (first?: number, max?: number, search?: string) => {
    const defaultClientScopes =
      await adminClient.clients.listDefaultClientScopes({ id: clientId });
    const optionalClientScopes =
      await adminClient.clients.listOptionalClientScopes({ id: clientId });
    const clientScopes = await adminClient.clientScopes.find();

    const find = (id: string) =>
      clientScopes.find((clientScope) => id === clientScope.id);

    const optional = optionalClientScopes.map((c) => {
      const scope = find(c.id!);
      const row: Row = {
        ...c,
        type: ClientScope.optional,
        description: scope?.description,
      };
      return row;
    });

    const defaultScopes = defaultClientScopes.map((c) => {
      const scope = find(c.id!);
      const row: Row = {
        ...c,
        type: ClientScope.default,
        description: scope?.description,
      };
      return row;
    });

    let rows = [...optional, ...defaultScopes];
    const names = rows.map((row) => row.name);

    const allowedProtocols = (() => {
      if (protocol === PROTOCOL_OIDC) {
        return isFeatureEnabled(Feature.OpenId4VCI)
          ? [PROTOCOL_OIDC, PROTOCOL_OID4VC]
          : [PROTOCOL_OIDC];
      }
      return [protocol];
    })();

    setRest(
      clientScopes
        .filter((scope) => !names.includes(scope.name))
        .filter(
          (scope) =>
            scope.protocol && allowedProtocols.includes(scope.protocol),
        ),
    );

    rows = localeSort(rows, mapByKey("name"));

    if (isViewer) {
      rows.unshift({
        id: DEDICATED_ROW,
        name: t("dedicatedScopeName", { clientName }),
        type: AllClientScopes.none,
        description: t("dedicatedScopeDescription"),
      });
    }

    const filter =
      searchType === "name" ? nameFilter(search) : typeFilter(searchTypeType);
    const firstNum = Number(first);

    return rows.filter(filter).slice(firstNum, firstNum + Number(max));
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteClientScope", {
      count: selectedRows.length,
      name: selectedRows[0]?.name,
    }),
    messageKey: "deleteConfirmClientScopes",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await removeClientScope(
          adminClient,
          clientId,
          selectedRows[0],
          selectedRows[0].type as ClientScope,
        );
        addAlert(t("clientScopeRemoveSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("clientScopeRemoveError", error);
      }
    },
  });

  return (
    <>
      {rest && (
        <AddScopeDialog
          clientScopes={rest}
          clientName={clientName!}
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
                      scope.type!,
                    ),
                ),
              );
              addAlert(t("clientScopeSuccess"), AlertVariant.success);
              refresh();
            } catch (error) {
              addError("clientScopeError", error);
            }
          }}
        />
      )}

      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="clientScopeList"
        searchPlaceholderKey={
          searchType === "name" ? "searchByName" : undefined
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
            {isManager && (
              <>
                <DeleteConfirm />
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
                    onOpenChange={(isOpen) => setKebabOpen(isOpen)}
                    toggle={(ref) => (
                      <MenuToggle
                        data-testid="kebab"
                        aria-label="Kebab toggle"
                        ref={ref}
                        variant="plain"
                        onClick={() => setKebabOpen(!kebabOpen)}
                        isExpanded={kebabOpen}
                      >
                        <EllipsisVIcon />
                      </MenuToggle>
                    )}
                    isOpen={kebabOpen}
                  >
                    <DropdownList>
                      <DropdownItem
                        key="deleteAll"
                        isDisabled={selectedRows.length === 0}
                        onClick={async () => {
                          try {
                            await Promise.all(
                              selectedRows.map((row) =>
                                removeClientScope(
                                  adminClient,
                                  clientId,
                                  { ...row },
                                  row.type as ClientScope,
                                ),
                              ),
                            );

                            setKebabOpen(false);
                            setSelectedRows([]);
                            addAlert(t("clientScopeRemoveSuccess"));
                            refresh();
                          } catch (error) {
                            addError("clientScopeRemoveError", error);
                          }
                        }}
                      >
                        {t("remove")}
                      </DropdownItem>
                    </DropdownList>
                  </Dropdown>
                </ToolbarItem>
              </>
            )}
          </>
        }
        columns={[
          {
            name: "name",
            displayKey: "assignedClientScope",
            cellRenderer: (row) => {
              if (isDedicatedRow(row)) {
                return (
                  <Link to={toDedicatedScope({ realm, clientId })}>
                    {row.name}
                  </Link>
                );
              }
              return row.name!;
            },
          },
          {
            name: "type",
            displayKey: "assignedType",
            cellRenderer: (row) => (
              <TypeSelector clientId={clientId} refresh={refresh} {...row} />
            ),
          },
          { name: "description", cellFormatters: [translationFormatter(t)] },
        ]}
        actions={
          isManager
            ? [
                {
                  title: t("remove"),
                  onRowClick: async (row) => {
                    setSelectedRows([row]);
                    toggleDeleteDialog();
                    return true;
                  },
                } as Action<Row>,
              ]
            : []
        }
        emptyState={
          <ListEmptyState
            message={t("emptyClientScopes")}
            instructions={t("emptyClientScopesInstructions")}
            primaryActionText={t("emptyClientScopesPrimaryAction")}
            onPrimaryAction={() => setAddDialogOpen(true)}
          />
        }
      />
    </>
  );
};
