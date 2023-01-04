import ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
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
import { useNavigate } from "react-router-dom-v5-compat";
import { useAlerts } from "../components/alert/Alerts";

import {
  AllClientScopes,
  changeScope,
  ClientScope,
  ClientScopeDefaultOptionalType,
} from "../components/client-scope/ClientScopeTypes";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { RoleMapping, Row } from "../components/role-mapping/RoleMapping";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
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
import { toMapper } from "./routes/Mapper";

export default function EditClientScope() {
  const { t } = useTranslation("client-scopes");
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { adminClient } = useAdminClient();
  const { id } = useParams<ClientScopeParams>();
  const { addAlert, addError } = useAlerts();
  const [clientScope, setClientScope] =
    useState<ClientScopeDefaultOptionalType>();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  useFetch(
    async () => {
      const clientScope = await adminClient.clientScopes.findOne({ id });

      if (!clientScope) {
        throw new Error(t("common:notFound"));
      }

      return {
        ...clientScope,
        type: await determineScopeType(clientScope),
      };
    },
    (clientScope) => {
      setClientScope(clientScope);
    },
    [key, id]
  );

  async function determineScopeType(clientScope: ClientScopeRepresentation) {
    const defaultScopes =
      await adminClient.clientScopes.listDefaultClientScopes();
    const hasDefaultScope = defaultScopes.find(
      (defaultScope) => defaultScope.name === clientScope.name
    );

    if (hasDefaultScope) {
      return ClientScope.default;
    }

    const optionalScopes =
      await adminClient.clientScopes.listDefaultOptionalClientScopes();
    const hasOptionalScope = optionalScopes.find(
      (optionalScope) => optionalScope.name === clientScope.name
    );

    return hasOptionalScope ? ClientScope.optional : AllClientScopes.none;
  }

  const useTab = (tab: ClientScopeTab) =>
    useRoutableTab(
      toClientScope({
        realm,
        id,
        tab,
      })
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

      addAlert(t("updateSuccess"), AlertVariant.success);
    } catch (error) {
      addError("client-scopes:updateError", error);
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
            {t("common:delete")}
          </DropdownItem>,
        ]}
        badges={[{ text: clientScope.protocol }]}
        divider={false}
      />

      <PageSection variant="light" className="pf-u-p-0">
        <RoutableTabs isBox>
          <Tab
            id="settings"
            data-testid="settings"
            title={<TabTitleText>{t("common:settings")}</TabTitleText>}
            {...settingsTab}
          >
            <PageSection variant="light">
              <ScopeForm save={onSubmit} clientScope={clientScope} />
            </PageSection>
          </Tab>
          <Tab
            id="mappers"
            data-testid="mappers"
            title={<TabTitleText>{t("common:mappers")}</TabTitleText>}
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
