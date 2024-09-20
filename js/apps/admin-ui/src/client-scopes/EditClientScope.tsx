import ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import { useAlerts, useFetch, useHelp } from "@keycloak/keycloak-ui-shared";
import {
  Alert,
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import {
  AllClientScopes,
  ClientScope,
  ClientScopeDefaultOptionalType,
  changeScope,
} from "../components/client-scope/ClientScopeTypes";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { RoleMapping, Row } from "../components/role-mapping/RoleMapping";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { convertFormValuesToObject } from "../util";
import { useParams } from "../utils/useParams";
import { MapperList } from "./details/MapperList";
import { ScopeForm } from "./details/ScopeForm";
import {
  ClientScopeParams,
  ClientScopeTab,
  toClientScope,
} from "./routes/ClientScope";
import { toClientScopes } from "./routes/ClientScopes";
import { toMapper } from "./routes/Mapper";

export default function EditClientScope() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { id } = useParams<ClientScopeParams>();
  const { addAlert, addError } = useAlerts();
  const { enabled } = useHelp();
  const [clientScope, setClientScope] =
    useState<ClientScopeDefaultOptionalType>();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  useFetch(
    async () => {
      const clientScope = await adminClient.clientScopes.findOne({ id });

      if (!clientScope) {
        throw new Error(t("notFound"));
      }

      return {
        ...clientScope,
        type: await determineScopeType(clientScope),
      };
    },
    (clientScope) => {
      setClientScope(clientScope);
    },
    [key, id],
  );

  async function determineScopeType(clientScope: ClientScopeRepresentation) {
    const defaultScopes =
      await adminClient.clientScopes.listDefaultClientScopes();
    const hasDefaultScope = defaultScopes.find(
      (defaultScope) => defaultScope.name === clientScope.name,
    );

    if (hasDefaultScope) {
      return ClientScope.default;
    }

    const optionalScopes =
      await adminClient.clientScopes.listDefaultOptionalClientScopes();
    const hasOptionalScope = optionalScopes.find(
      (optionalScope) => optionalScope.name === clientScope.name,
    );

    return hasOptionalScope ? ClientScope.optional : AllClientScopes.none;
  }

  const useTab = (tab: ClientScopeTab) =>
    useRoutableTab(
      toClientScope({
        realm,
        id,
        tab,
      }),
    );

  const settingsTab = useTab("settings");
  const mappersTab = useTab("mappers");
  const scopeTab = useTab("scope");

  const onSubmit = async (formData: ClientScopeDefaultOptionalType) => {
    const clientScope = convertFormValuesToObject({
      ...formData,
      name: formData.name?.trim().replace(/ /g, "_"),
    });

    try {
      await adminClient.clientScopes.update({ id }, clientScope);
      await changeScope(adminClient, { ...clientScope, id }, clientScope.type);

      addAlert(t("updateSuccessClientScope"), AlertVariant.success);
    } catch (error) {
      addError("updateErrorClientScope", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteClientScope", {
      count: 1,
      name: clientScope?.name,
    }),
    messageKey: "deleteConfirmClientScopes",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clientScopes.del({ id });
        addAlert(t("deletedSuccessClientScope"), AlertVariant.success);
        navigate(toClientScopes({ realm }));
      } catch (error) {
        addError("deleteErrorClientScope", error);
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
        realmRoles,
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
              [row.role as RoleMappingPayload],
            ),
          ),
      );
      addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
    } catch (error) {
      addError("roleMappingUpdatedError", error);
    }
  };

  const addMappers = async (
    mappers: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[],
  ): Promise<void> => {
    if (!Array.isArray(mappers)) {
      const mapper = mappers as ProtocolMapperTypeRepresentation;
      navigate(
        toMapper({
          realm,
          id: clientScope!.id!,
          mapperId: mapper.id!,
        }),
      );
    } else {
      try {
        await adminClient.clientScopes.addMultipleProtocolMappers(
          { id: clientScope!.id! },
          mappers as ProtocolMapperRepresentation[],
        );
        refresh();
        addAlert(t("mappingCreatedSuccess"), AlertVariant.success);
      } catch (error) {
        addError("mappingCreatedError", error);
      }
    }
  };

  const onDelete = async (mapper: ProtocolMapperRepresentation) => {
    try {
      await adminClient.clientScopes.delProtocolMapper({
        id: clientScope!.id!,
        mapperId: mapper.id!,
      });
      addAlert(t("mappingDeletedSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("mappingDeletedError", error);
    }
    return true;
  };

  if (!clientScope) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={clientScope.name!}
        dropdownItems={[
          <DropdownItem key="delete" onClick={toggleDeleteDialog}>
            {t("delete")}
          </DropdownItem>,
        ]}
        badges={[{ text: clientScope.protocol }]}
        divider={false}
      />

      <PageSection variant="light" className="pf-v5-u-p-0">
        <RoutableTabs isBox>
          <Tab
            id="settings"
            data-testid="settings"
            title={<TabTitleText>{t("settings")}</TabTitleText>}
            {...settingsTab}
          >
            <PageSection variant="light">
              <ScopeForm save={onSubmit} clientScope={clientScope} />
            </PageSection>
          </Tab>
          <Tab
            id="mappers"
            data-testid="mappers"
            title={<TabTitleText>{t("mappers")}</TabTitleText>}
            {...mappersTab}
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
            {...scopeTab}
          >
            {enabled && (
              <PageSection>
                <Alert
                  variant="info"
                  isInline
                  title={t("clientScopesRolesScope")}
                  component="h2"
                />
              </PageSection>
            )}
            <RoleMapping
              id={clientScope.id!}
              name={clientScope.name!}
              type="clientScopes"
              save={assignRoles}
            />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
}
