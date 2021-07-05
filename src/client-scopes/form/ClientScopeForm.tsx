import React, { useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Spinner,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";

import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { KeycloakTabs } from "../../components/keycloak-tabs/KeycloakTabs";
import { useAlerts } from "../../components/alert/Alerts";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { convertFormValuesToObject } from "../../util";
import { MapperList } from "../details/MapperList";
import { ScopeForm } from "../details/ScopeForm";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import {
  mapRoles,
  RoleMapping,
  Row,
} from "../../components/role-mapping/RoleMapping";
import type { RoleMappingPayload } from "keycloak-admin/lib/defs/roleRepresentation";
import {
  AllClientScopes,
  changeScope,
  ClientScopeDefaultOptionalType,
} from "../../components/client-scope/ClientScopeTypes";
import { useRealm } from "../../context/realm-context/RealmContext";

export const ClientScopeForm = () => {
  const { t } = useTranslation("client-scopes");
  const [
    clientScope,
    setClientScope,
  ] = useState<ClientScopeDefaultOptionalType>();
  const history = useHistory();
  const { realm } = useRealm();
  const [hide, setHide] = useState(false);

  const adminClient = useAdminClient();
  const { id, type } = useParams<{ id: string; type: AllClientScopes }>();

  const { addAlert } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  useFetch(
    async () => {
      if (id) {
        return {
          ...(await adminClient.clientScopes.findOne({ id })),
          type,
        } as ClientScopeDefaultOptionalType;
      }
    },
    (clientScope) => {
      setClientScope(clientScope);
    },
    [key, id]
  );

  const loader = async () => {
    const assignedRoles = (
      await adminClient.clientScopes.listRealmScopeMappings({ id })
    ).map((role) => ({ role }));
    const effectiveRoles = (
      await adminClient.clientScopes.listCompositeRealmScopeMappings({ id })
    ).map((role) => ({ role }));
    const clients = await adminClient.clients.find();

    const clientRoles = (
      await Promise.all(
        clients.map(async (client) => {
          const clientAssignedRoles = (
            await adminClient.clientScopes.listClientScopeMappings({
              id,
              client: client.id!,
            })
          ).map((role) => ({ role, client }));
          const clientEffectiveRoles = (
            await adminClient.clientScopes.listCompositeClientScopeMappings({
              id,
              client: client.id!,
            })
          ).map((role) => ({ role, client }));
          return mapRoles(clientAssignedRoles, clientEffectiveRoles, hide);
        })
      )
    ).flat();

    return [...mapRoles(assignedRoles, effectiveRoles, hide), ...clientRoles];
  };

  const save = async (clientScopes: ClientScopeDefaultOptionalType) => {
    try {
      clientScopes.attributes = convertFormValuesToObject(
        clientScopes.attributes!
      );

      if (id) {
        await adminClient.clientScopes.update({ id }, clientScopes);
        changeScope(
          adminClient,
          { ...clientScopes, id, type },
          clientScopes.type
        );
      } else {
        await adminClient.clientScopes.create(clientScopes);
        const scope = await adminClient.clientScopes.findOneByName({
          name: clientScopes.name!,
        });
        changeScope(
          adminClient,
          { ...clientScopes, id: scope.id },
          clientScopes.type
        );
        history.push(
          `/${realm}/client-scopes/${scope.id}/${
            clientScopes.type || "none"
          }/settings`
        );
      }
      addAlert(t((id ? "update" : "create") + "Success"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t((id ? "update" : "create") + "Error", { error }),
        AlertVariant.danger
      );
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteClientScope", {
      count: 1,
      name: clientScope?.name,
    }),
    messageKey: "client-scopes:deleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clientScopes.del({ id });
        addAlert(t("deletedSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(t("deleteError", { error }), AlertVariant.danger);
      }
    },
  });

  const assignRoles = async (rows: Row[]) => {
    try {
      const realmRoles = rows
        .filter((row) => row.client === undefined)
        .map((row) => row.role as RoleMappingPayload)
        .flat();
      await adminClient.clientScopes.addRealmScopeMappings(
        {
          id,
        },
        realmRoles
      );
      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.clientScopes.addClientScopeMappings(
              {
                id,
                client: row.client!.id!,
              },
              [row.role as RoleMappingPayload]
            )
          )
      );
      addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t("roleMappingUpdatedError", {
          error: error.response?.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };

  if (id && !clientScope) {
    return (
      <div className="pf-u-text-align-center">
        <Spinner />
      </div>
    );
  }

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={
          clientScope ? clientScope.name! : "client-scopes:createClientScope"
        }
        dropdownItems={
          clientScope
            ? [
                <DropdownItem key="delete" onClick={() => toggleDeleteDialog()}>
                  {t("common:delete")}
                </DropdownItem>,
              ]
            : undefined
        }
        badge={clientScope ? clientScope.protocol : undefined}
        divider={!id}
      />

      <PageSection variant="light" className="pf-u-p-0">
        {!id && (
          <PageSection variant="light">
            <ScopeForm save={save} clientScope={{}} />
          </PageSection>
        )}
        {id && clientScope && (
          <KeycloakTabs isBox>
            <Tab
              eventKey="settings"
              title={<TabTitleText>{t("common:settings")}</TabTitleText>}
            >
              <PageSection variant="light">
                <ScopeForm save={save} clientScope={clientScope} />
              </PageSection>
            </Tab>
            <Tab
              eventKey="mappers"
              title={<TabTitleText>{t("common:mappers")}</TabTitleText>}
            >
              <MapperList clientScope={clientScope} refresh={refresh} />
            </Tab>
            <Tab
              data-testid="scopeTab"
              eventKey="scope"
              title={<TabTitleText>{t("scope")}</TabTitleText>}
            >
              <RoleMapping
                id={id}
                name={clientScope.name!}
                type={"client-scope"}
                loader={loader}
                save={assignRoles}
                onHideRolesToggle={() => setHide(!hide)}
              />
            </Tab>
          </KeycloakTabs>
        )}
      </PageSection>
    </>
  );
};
