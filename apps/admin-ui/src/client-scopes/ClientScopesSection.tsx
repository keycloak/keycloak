import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom-v5-compat";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  KebabToggle,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";

import { useAdminClient } from "../context/auth/AdminClient";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../context/realm-context/RealmContext";
import { emptyFormatter } from "../util";
import useLocaleSort, { mapByKey } from "../utils/useLocaleSort";
import {
  CellDropdown,
  ClientScope,
  AllClientScopes,
  ClientScopeDefaultOptionalType,
  changeScope,
  removeScope,
  AllClientScopeType,
} from "../components/client-scope/ClientScopeTypes";
import { ChangeTypeDropdown } from "./ChangeTypeDropdown";
import { toNewClientScope } from "./routes/NewClientScope";

import { toClientScope } from "./routes/ClientScope";
import {
  nameFilter,
  protocolFilter,
  ProtocolType,
  SearchDropdown,
  SearchToolbar,
  SearchType,
  typeFilter,
} from "./details/SearchFilter";
import type { Row } from "../clients/scopes/ClientScopes";
import { getProtocolName } from "../clients/utils";
import helpUrls from "../help-urls";

import "./client-scope.css";

export default function ClientScopesSection() {
  const { realm } = useRealm();
  const { t } = useTranslation("client-scopes");

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [kebabOpen, setKebabOpen] = useState(false);
  const [selectedScopes, setSelectedScopes] = useState<
    ClientScopeDefaultOptionalType[]
  >([]);

  const [searchType, setSearchType] = useState<SearchType>("name");
  const [searchTypeType, setSearchTypeType] = useState<AllClientScopes>(
    AllClientScopes.none
  );
  const [searchProtocol, setSearchProtocol] = useState<ProtocolType>("all");
  const localeSort = useLocaleSort();

  const [key, setKey] = useState(0);
  const refresh = () => {
    setSelectedScopes([]);
    setKey(key + 1);
  };

  const loader = async (first?: number, max?: number, search?: string) => {
    const defaultScopes =
      await adminClient.clientScopes.listDefaultClientScopes();
    const optionalScopes =
      await adminClient.clientScopes.listDefaultOptionalClientScopes();
    const clientScopes = await adminClient.clientScopes.find();

    const filter =
      searchType === "name"
        ? nameFilter(search)
        : searchType === "type"
        ? typeFilter(searchTypeType)
        : protocolFilter(searchProtocol);

    const transformed = clientScopes
      .map((scope) => {
        const row: Row = {
          ...scope,
          type: defaultScopes.find(
            (defaultScope) => defaultScope.name === scope.name
          )
            ? ClientScope.default
            : optionalScopes.find(
                (optionalScope) => optionalScope.name === scope.name
              )
            ? ClientScope.optional
            : AllClientScopes.none,
        };
        return row;
      })
      .filter(filter);

    return localeSort(transformed, mapByKey("name")).slice(
      first,
      Number(first) + Number(max)
    );
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteClientScope", {
      count: selectedScopes.length,
      name: selectedScopes[0]?.name,
    }),
    messageKey: "client-scopes:deleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        for (const scope of selectedScopes) {
          try {
            await removeScope(adminClient, scope);
          } catch (error: any) {
            console.warn(
              "could not remove scope",
              error.response?.data?.errorMessage || error
            );
          }
          await adminClient.clientScopes.del({ id: scope.id! });
        }
        addAlert(t("deletedSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("client-scopes:deleteError", error);
      }
    },
  });

  const TypeSelector = (scope: ClientScopeDefaultOptionalType) => (
    <CellDropdown
      clientScope={scope}
      type={scope.type}
      all
      onSelect={async (value) => {
        try {
          await changeScope(adminClient, scope, value as AllClientScopeType);
          addAlert(t("clientScopeSuccess"), AlertVariant.success);
          refresh();
        } catch (error) {
          addError("client-scopes:clientScopeError", error);
        }
      }}
    />
  );

  const ClientScopeDetailLink = ({
    id,
    name,
  }: ClientScopeDefaultOptionalType) => (
    <Link key={id} to={toClientScope({ realm, id: id!, tab: "settings" })}>
      {name}
    </Link>
  );
  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey="clientScopes"
        subKey="client-scopes:clientScopeExplain"
        helpUrl={helpUrls.clientScopesUrl}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <KeycloakDataTable
          key={key}
          loader={loader}
          ariaLabelKey="client-scopes:clientScopeList"
          searchPlaceholderKey={
            searchType === "name" ? "client-scopes:searchFor" : undefined
          }
          isSearching={searchType !== "name"}
          searchTypeComponent={
            <SearchDropdown
              searchType={searchType}
              onSelect={(searchType) => setSearchType(searchType)}
              withProtocol
            />
          }
          isPaginated
          onSelect={(clientScopes) => setSelectedScopes([...clientScopes])}
          canSelectAll
          toolbarItem={
            <>
              <SearchToolbar
                searchType={searchType}
                type={searchTypeType}
                onSelect={(searchType) => {
                  setSearchType(searchType);
                  setSearchProtocol("all");
                  setSearchTypeType(AllClientScopes.none);
                  refresh();
                }}
                onType={(value) => {
                  setSearchTypeType(value);
                  setSearchProtocol("all");
                  refresh();
                }}
                protocol={searchProtocol}
                onProtocol={(protocol) => {
                  setSearchProtocol(protocol);
                  setSearchTypeType(AllClientScopes.none);
                  refresh();
                }}
              />

              <ToolbarItem>
                <Button
                  component={(props) => (
                    <Link {...props} to={toNewClientScope({ realm })} />
                  )}
                >
                  {t("createClientScope")}
                </Button>
              </ToolbarItem>
              <ToolbarItem>
                <ChangeTypeDropdown
                  selectedRows={selectedScopes}
                  refresh={refresh}
                />
              </ToolbarItem>
              <ToolbarItem>
                <Dropdown
                  toggle={<KebabToggle onToggle={setKebabOpen} />}
                  isOpen={kebabOpen}
                  isPlain
                  dropdownItems={[
                    <DropdownItem
                      key="action"
                      component="button"
                      isDisabled={selectedScopes.length === 0}
                      onClick={() => {
                        toggleDeleteDialog();
                        setKebabOpen(false);
                      }}
                    >
                      {t("common:delete")}
                    </DropdownItem>,
                  ]}
                />
              </ToolbarItem>
            </>
          }
          actions={[
            {
              title: t("common:delete"),
              onRowClick: (clientScope) => {
                setSelectedScopes([clientScope]);
                toggleDeleteDialog();
              },
            },
          ]}
          columns={[
            {
              name: "name",
              cellRenderer: ClientScopeDetailLink,
            },
            {
              name: "type",
              displayKey: "client-scopes:assignedType",
              cellRenderer: TypeSelector,
            },
            {
              name: "protocol",
              displayKey: "client-scopes:protocol",
              cellRenderer: (client) =>
                getProtocolName(t, client.protocol ?? "openid-connect"),
              transforms: [cellWidth(15)],
            },
            {
              name: "attributes['gui.order']",
              displayKey: "client-scopes:displayOrder",
              cellFormatters: [emptyFormatter()],
              transforms: [cellWidth(15)],
            },
            { name: "description", cellFormatters: [emptyFormatter()] },
          ]}
        />
      </PageSection>
    </>
  );
}
