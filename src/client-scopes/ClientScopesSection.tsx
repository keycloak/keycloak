import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useHistory, useRouteMatch } from "react-router-dom";
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
import ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";

import { useAdminClient } from "../context/auth/AdminClient";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { upperCaseFormatter, emptyFormatter } from "../util";
import {
  CellDropdown,
  ClientScope,
  AllClientScopes,
  AllClientScopeType,
} from "../components/client-scope/ClientScopeTypes";
import KeycloakAdminClient from "keycloak-admin";
import { ChangeTypeDialog } from "./ChangeTypeDialog";

import "./client-scope.css";

type ClientScopeDefaultOptionalType = ClientScopeRepresentation & {
  type: AllClientScopeType;
};

const castAdminClient = (adminClient: KeycloakAdminClient) =>
  (adminClient.clientScopes as unknown) as {
    [index: string]: Function;
  };

const changeScope = async (
  adminClient: KeycloakAdminClient,
  clientScope: ClientScopeDefaultOptionalType,
  changeTo: AllClientScopeType
) => {
  await removeScope(adminClient, clientScope);
  await addScope(adminClient, clientScope, changeTo);
};

const removeScope = async (
  adminClient: KeycloakAdminClient,
  clientScope: ClientScopeDefaultOptionalType
) => {
  if (clientScope.type !== AllClientScopes.none)
    await castAdminClient(adminClient)[
      `delDefault${
        clientScope.type === ClientScope.optional ? "Optional" : ""
      }ClientScope`
    ]({
      id: clientScope.id!,
    });
};

const addScope = async (
  adminClient: KeycloakAdminClient,
  clientScope: ClientScopeDefaultOptionalType,
  type: AllClientScopeType
) => {
  if (type !== AllClientScopes.none)
    await castAdminClient(adminClient)[
      `addDefault${type === ClientScope.optional ? "Optional" : ""}ClientScope`
    ]({
      id: clientScope.id!,
    });
};

export const ClientScopesSection = () => {
  const { t } = useTranslation("client-scopes");
  const history = useHistory();
  const { url } = useRouteMatch();

  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [kebabOpen, setKebabOpen] = useState(false);
  const [changeTypeOpen, setChangeTypeOpen] = useState(false);
  const [selectedScopes, setSelectedScopes] = useState<
    ClientScopeDefaultOptionalType[]
  >([]);

  const loader = async () => {
    const defaultScopes = await adminClient.clientScopes.listDefaultClientScopes();
    const optionalScopes = await adminClient.clientScopes.listDefaultOptionalClientScopes();

    const clientScopes = (await adminClient.clientScopes.find()).map(
      (scope) => {
        return {
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
      }
    );

    return clientScopes;
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
          await adminClient.clientScopes.del({ id: scope.id! });
        }
        addAlert(t("deletedSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addAlert(
          t("deleteError", {
            error: error.response?.data?.errorMessage || error,
          }),
          AlertVariant.danger
        );
      }
    },
  });

  const TypeSelector = (scope: ClientScopeDefaultOptionalType) => (
    <>
      <CellDropdown
        clientScope={scope}
        type={scope.type}
        all
        onSelect={async (value) => {
          try {
            await changeScope(adminClient, scope, value);
            addAlert(t("clientScopeSuccess"), AlertVariant.success);
            refresh();
          } catch (error) {
            addAlert(t("clientScopeError", { error }), AlertVariant.danger);
          }
        }}
      />
    </>
  );

  const ClientScopeDetailLink = (clientScope: ClientScopeRepresentation) => (
    <>
      <Link key={clientScope.id} to={`${url}/${clientScope.id}/settings`}>
        {clientScope.name}
      </Link>
    </>
  );
  return (
    <>
      <DeleteConfirm />
      {changeTypeOpen && (
        <ChangeTypeDialog
          selectedClientScopes={selectedScopes.length}
          onConfirm={(type) => {
            selectedScopes.map(async (scope) => {
              try {
                await changeScope(adminClient, scope, type);
                addAlert(t("clientScopeSuccess"), AlertVariant.success);
                refresh();
              } catch (error) {
                addAlert(t("clientScopeError", { error }), AlertVariant.danger);
              }
            });
            setChangeTypeOpen(false);
          }}
          onClose={() => setChangeTypeOpen(false)}
        />
      )}
      <ViewHeader
        titleKey="clientScopes"
        subKey="client-scopes:clientScopeExplain"
      />
      <PageSection variant="light" className="pf-u-p-0">
        <KeycloakDataTable
          key={key}
          loader={loader}
          ariaLabelKey="client-scopes:clientScopeList"
          searchPlaceholderKey="client-scopes:searchFor"
          onSelect={(clientScopes) => setSelectedScopes([...clientScopes])}
          canSelectAll
          toolbarItem={
            <>
              <ToolbarItem>
                <Button onClick={() => history.push(`${url}/new`)}>
                  {t("createClientScope")}
                </Button>
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
                      key="changeType"
                      component="button"
                      isDisabled={selectedScopes.length === 0}
                      onClick={() => {
                        setChangeTypeOpen(true);
                        setKebabOpen(false);
                      }}
                    >
                      {t("changeType")}
                    </DropdownItem>,

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
              title: t("common:export"),
              onRowClick: () => {},
            },
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
            { name: "description", cellFormatters: [emptyFormatter()] },
            { name: "type", cellRenderer: TypeSelector },
            {
              name: "protocol",
              displayKey: "client-scopes:protocol",
              cellFormatters: [upperCaseFormatter()],
              transforms: [cellWidth(15)],
            },
            {
              name: "attributes['gui.order']",
              displayKey: "client-scopes:displayOrder",
              cellFormatters: [emptyFormatter()],
              transforms: [cellWidth(15)],
            },
          ]}
        />
      </PageSection>
    </>
  );
};
