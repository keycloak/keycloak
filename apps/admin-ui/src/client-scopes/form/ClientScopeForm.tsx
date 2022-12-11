import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { useNavigate } from "react-router-dom-v5-compat";

import { useAlerts } from "../../components/alert/Alerts";
import {
  AllClientScopes,
  changeScope,
  ClientScope,
  ClientScopeDefaultOptionalType,
} from "../../components/client-scope/ClientScopeTypes";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { RoleMapping, Row } from "../../components/role-mapping/RoleMapping";
import {
  routableTab,
  RoutableTabs,
} from "../../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject } from "../../util";
import { useParams } from "../../utils/useParams";
import { MapperList } from "../details/MapperList";
import { ScopeForm } from "../details/ScopeForm";
import { ClientScopeTab, toClientScope } from "../routes/ClientScope";
import { toMapper } from "../routes/Mapper";

export default function ClientScopeForm() {
  const { t } = useTranslation("client-scopes");
  const [clientScope, setClientScope] =
    useState<ClientScopeDefaultOptionalType>();
  const history = useHistory();
  const navigate = useNavigate();
  const { realm } = useRealm();

  const { adminClient } = useAdminClient();
  const { id } = useParams<{ id: string }>();

  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  useFetch(
    async () => {
      if (id) {
        const clientScope = await adminClient.clientScopes.findOne({ id });
        if (!clientScope) {
          throw new Error(t("common:notFound"));
        }

        const defaultScopes =
          await adminClient.clientScopes.listDefaultClientScopes();
        const optionalScopes =
          await adminClient.clientScopes.listDefaultOptionalClientScopes();

        return {
          ...clientScope,
          type: defaultScopes.find(
            (defaultScope) => defaultScope.name === clientScope.name
          )
            ? ClientScope.default
            : optionalScopes.find(
                (optionalScope) => optionalScope.name === clientScope.name
              )
            ? ClientScope.optional
            : AllClientScopes.none,
        };
      }
    },
    (clientScope) => {
      setClientScope(clientScope);
    },
    [key, id]
  );

  const save = async (clientScopes: ClientScopeDefaultOptionalType) => {
    try {
      clientScopes.name = clientScopes.name?.trim().replace(/ /g, "_");
      clientScopes = convertFormValuesToObject(
        clientScopes
      ) as ClientScopeDefaultOptionalType;

      if (id) {
        await adminClient.clientScopes.update({ id }, clientScopes);
        changeScope(adminClient, { ...clientScopes, id }, clientScopes.type);
      } else {
        await adminClient.clientScopes.create(clientScopes);
        const scope = await adminClient.clientScopes.findOneByName({
          name: clientScopes.name!,
        });
        if (!scope) {
          throw new Error(t("common:notFound"));
        }

        changeScope(
          adminClient,
          { ...clientScopes, id: scope.id },
          clientScopes.type
        );
        navigate(
          toClientScope({
            realm,
            id: scope.id!,
            tab: "settings",
          })
        );
      }
      addAlert(t((id ? "update" : "create") + "Success"), AlertVariant.success);
    } catch (error) {
      addError(`client-scopes:${id ? "update" : "create"}Error`, error);
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
        addError("client-scopes:deleteError", error);
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
      addError("client-scopes:roleMappingUpdatedError", error);
    }
  };

  const addMappers = async (
    mappers: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[]
  ): Promise<void> => {
    if (!Array.isArray(mappers)) {
      const mapper = mappers as ProtocolMapperTypeRepresentation;
      navigate(
        toMapper({
          realm,
          id: clientScope!.id!,
          mapperId: mapper.id!,
        })
      );
    } else {
      try {
        await adminClient.clientScopes.addMultipleProtocolMappers(
          { id: clientScope!.id! },
          mappers as ProtocolMapperRepresentation[]
        );
        refresh();
        addAlert(t("common:mappingCreatedSuccess"), AlertVariant.success);
      } catch (error) {
        addError("common:mappingCreatedError", error);
      }
    }
  };

  const onDelete = async (mapper: ProtocolMapperRepresentation) => {
    try {
      await adminClient.clientScopes.delProtocolMapper({
        id: clientScope!.id!,
        mapperId: mapper.id!,
      });
      addAlert(t("common:mappingDeletedSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("common:mappingDeletedError", error);
    }
    return true;
  };

  if (id && !clientScope) {
    return <KeycloakSpinner />;
  }

  const clientRoute = (tab: ClientScopeTab) =>
    routableTab({
      to: toClientScope({
        realm,
        id,
        tab,
      }),
      history,
    });

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
                <DropdownItem key="delete" onClick={toggleDeleteDialog}>
                  {t("common:delete")}
                </DropdownItem>,
              ]
            : undefined
        }
        badges={[{ text: clientScope ? clientScope.protocol : undefined }]}
        divider={!id}
      />

      <PageSection variant="light" className="pf-u-p-0">
        {!id && (
          <PageSection variant="light">
            <ScopeForm save={save} />
          </PageSection>
        )}
        {id && clientScope && (
          <RoutableTabs isBox>
            <Tab
              id="settings"
              data-testid="settings"
              title={<TabTitleText>{t("common:settings")}</TabTitleText>}
              {...clientRoute("settings")}
            >
              <PageSection variant="light">
                <ScopeForm save={save} clientScope={clientScope} />
              </PageSection>
            </Tab>
            <Tab
              id="mappers"
              data-testid="mappers"
              title={<TabTitleText>{t("common:mappers")}</TabTitleText>}
              {...clientRoute("mappers")}
            >
              <MapperList
                model={clientScope}
                onAdd={addMappers}
                onDelete={onDelete}
                detailLink={(id) =>
                  toMapper({ realm, id: clientScope.id!, mapperId: id! })
                }
              />
            </Tab>
            <Tab
              id="scope"
              data-testid="scopeTab"
              title={<TabTitleText>{t("scope")}</TabTitleText>}
              {...clientRoute("scope")}
            >
              <RoleMapping
                id={id}
                name={clientScope.name!}
                type="clientScopes"
                save={assignRoles}
              />
            </Tab>
          </RoutableTabs>
        )}
      </PageSection>
    </>
  );
}
