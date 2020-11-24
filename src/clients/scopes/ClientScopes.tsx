import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { TFunction } from "i18next";
import {
  IFormatter,
  IFormatterValueType,
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Select,
  Spinner,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";
import ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";
import KeycloakAdminClient from "keycloak-admin";

import { useAdminClient } from "../../context/auth/AdminClient";
import { TableToolbar } from "../../components/table-toolbar/TableToolbar";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { AddScopeDialog } from "./AddScopeDialog";
import {
  clientScopeTypesSelectOptions,
  ClientScopeType,
  ClientScope,
} from "./ClientScopeTypes";

export type ClientScopesProps = {
  clientId: string;
  protocol: string;
};

const firstUpperCase = (name: string) =>
  name.charAt(0).toUpperCase() + name.slice(1);

const changeScope = async (
  adminClient: KeycloakAdminClient,
  clientId: string,
  clientScope: ClientScopeRepresentation,
  type: ClientScopeType,
  changeTo: ClientScopeType
) => {
  const typeToName = firstUpperCase(type);
  const changeToName = firstUpperCase(changeTo);

  const indexedAdminClient = (adminClient.clients as unknown) as {
    [index: string]: Function;
  };
  await indexedAdminClient[`del${typeToName}ClientScope`]({
    id: clientId,
    clientScopeId: clientScope.id!,
  });
  await indexedAdminClient[`add${changeToName}ClientScope`]({
    id: clientId,
    clientScopeId: clientScope.id!,
  });
};

type CellDropdownProps = {
  clientId: string;
  clientScope: ClientScopeRepresentation;
  type: ClientScopeType;
};

const CellDropdown = ({ clientId, clientScope, type }: CellDropdownProps) => {
  const adminClient = useAdminClient();
  const { t } = useTranslation("clients");
  const [open, setOpen] = useState(false);

  return (
    <Select
      key={clientScope.id}
      onToggle={() => setOpen(!open)}
      isOpen={open}
      selections={[type]}
      onSelect={(_, value) => {
        changeScope(
          adminClient,
          clientId,
          clientScope,
          type,
          value as ClientScopeType
        );
        setOpen(false);
      }}
    >
      {clientScopeTypesSelectOptions(t)}
    </Select>
  );
};

type SearchType = "client" | "assigned";

type TableRow = {
  clientScope: ClientScopeRepresentation;
  type: ClientScopeType;
  cells: (string | undefined)[];
};

export const ClientScopes = ({ clientId, protocol }: ClientScopesProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const [searchToggle, setSearchToggle] = useState(false);
  const [searchType, setSearchType] = useState<SearchType>("client");
  const [addToggle, setAddToggle] = useState(false);
  const [addDialogOpen, setAddDialogOpen] = useState(false);

  const [rows, setRows] = useState<TableRow[]>();
  const [rest, setRest] = useState<ClientScopeRepresentation[]>();

  const loader = async () => {
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
        clientScope: c,
        type: ClientScope.optional,
        cells: [c.name, c.id, scope.description],
      };
    });

    const defaultScopes = defaultClientScopes.map((c) => {
      const scope = find(c.id!);
      return {
        clientScope: c,
        type: ClientScope.default,
        cells: [c.name, c.id, scope.description],
      };
    });

    setRows([...optional, ...defaultScopes]);
  };

  useEffect(() => {
    loader();
  }, []);

  useEffect(() => {
    if (rows) {
      loadRest(rows);
    }
  }, [rows]);

  const loadRest = async (rows: { cells: (string | undefined)[] }[]) => {
    const clientScopes = await adminClient.clientScopes.find();
    const names = rows.map((row) => row.cells[0]);

    setRest(
      clientScopes
        .filter((scope) => !names.includes(scope.name))
        .filter((scope) => scope.protocol === protocol)
    );
  };

  const dropdown = (): IFormatter => (data?: IFormatterValueType) => {
    if (!data) {
      return <></>;
    }
    const row = rows?.find((row) => row.clientScope.id === data.toString())!;
    return (
      <CellDropdown
        clientId={clientId}
        clientScope={row.clientScope}
        type={row.type}
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

      {rows && rows.length > 0 && (
        <>
          {rest && (
            <AddScopeDialog
              clientScopes={rest}
              open={addDialogOpen}
              toggleDialog={() => setAddDialogOpen(!addDialogOpen)}
            />
          )}

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
              <Split hasGutter>
                <SplitItem>
                  <Button onClick={() => setAddDialogOpen(true)}>
                    {t("addClientScope")}
                  </Button>
                </SplitItem>
                <SplitItem>
                  <Select
                    id="add-dropdown"
                    key="add-dropdown"
                    isOpen={addToggle}
                    selections={[]}
                    placeholderText={t("changeTypeTo")}
                    onToggle={() => setAddToggle(!addToggle)}
                    onSelect={(_, value) => {
                      console.log(value);
                      setAddToggle(false);
                    }}
                  >
                    {clientScopeTypesSelectOptions(t)}
                  </Select>
                </SplitItem>
              </Split>
            }
          >
            <Table
              onSelect={() => {}}
              variant={TableVariant.compact}
              cells={[
                t("name"),
                { title: t("description"), cellFormatters: [dropdown()] },
                t("protocol"),
              ]}
              rows={rows}
              actions={[
                {
                  title: t("common:remove"),
                  onClick: () => {},
                },
              ]}
              aria-label={t("clientScopeList")}
            >
              <TableHeader />
              <TableBody />
            </Table>
          </TableToolbar>
        </>
      )}
      {rows && rows.length === 0 && (
        <ListEmptyState
          message={t("clients:emptyClientScopes")}
          instructions={t("clients:emptyClientScopesInstructions")}
          primaryActionText={t("clients:emptyClientScopesPrimaryAction")}
          onPrimaryAction={() => {}}
        />
      )}
    </>
  );
};
