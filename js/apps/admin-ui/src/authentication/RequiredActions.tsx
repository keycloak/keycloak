import { fetchWithError } from "@keycloak/keycloak-admin-client";
import type RequiredActionProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import type RequiredActionProviderSimpleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderSimpleRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import { AlertVariant, Button, Switch } from "@patternfly/react-core";
import { CogIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { addTrailingSlash, toKey } from "../util";
import { getAuthorizationHeaders } from "../utils/getAuthorizationHeaders";
import { DraggableTable } from "./components/DraggableTable";
import { RequiredActionConfigModal } from "./components/RequiredActionConfigModal";

type DataType = RequiredActionProviderRepresentation &
  RequiredActionProviderSimpleRepresentation & {
    configurable?: boolean;
  };

type Row = {
  name?: string;
  enabled: boolean;
  defaultAction: boolean;
  data: DataType;
};

export const RequiredActions = () => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [actions, setActions] = useState<Row[]>();
  const [selectedAction, setSelectedAction] = useState<DataType>();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const { realm: realmName } = useRealm();

  const loadActions = async (): Promise<
    RequiredActionProviderRepresentation[]
  > => {
    const requiredActionsRequest = await fetchWithError(
      `${addTrailingSlash(
        adminClient.baseUrl,
      )}admin/realms/${realmName}/ui-ext/authentication-management/required-actions`,
      {
        method: "GET",
        headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
      },
    );

    return (await requiredActionsRequest.json()) as DataType[];
  };

  useFetch(
    async () => {
      const [requiredActions, unregisteredRequiredActions] = await Promise.all([
        loadActions(),
        adminClient.authenticationManagement.getUnregisteredRequiredActions(),
      ]);
      return [
        ...requiredActions.map((action) => ({
          name: action.name!,
          enabled: action.enabled!,
          defaultAction: action.defaultAction!,
          data: action,
        })),
        ...unregisteredRequiredActions.map((action) => ({
          name: action.name!,
          enabled: false,
          defaultAction: false,
          data: action,
        })),
      ];
    },
    (actions) => setActions(actions),
    [key],
  );

  const isUnregisteredAction = (data: DataType): boolean => {
    return !("alias" in data);
  };

  const updateAction = async (
    action: DataType,
    field: "enabled" | "defaultAction",
  ) => {
    try {
      if (field in action) {
        action[field] = !action[field];
        // remove configurable property from action which only exists for the admin ui
        delete action.configurable;
        await adminClient.authenticationManagement.updateRequiredAction(
          { alias: action.alias! },
          action,
        );
      } else if (isUnregisteredAction(action)) {
        await adminClient.authenticationManagement.registerRequiredAction({
          name: action.name,
          providerId: action.providerId,
        });
      }
      refresh();
      addAlert(t("updatedRequiredActionSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updatedRequiredActionError", error);
    }
  };

  const executeMove = async (
    action: RequiredActionProviderRepresentation,
    times: number,
  ) => {
    try {
      const alias = action.alias!;
      for (let index = 0; index < Math.abs(times); index++) {
        if (times > 0) {
          await adminClient.authenticationManagement.lowerRequiredActionPriority(
            {
              alias,
            },
          );
        } else {
          await adminClient.authenticationManagement.raiseRequiredActionPriority(
            {
              alias,
            },
          );
        }
      }
      refresh();

      addAlert(t("updatedRequiredActionSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updatedRequiredActionError", error);
    }
  };

  if (!actions) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      {selectedAction && (
        <RequiredActionConfigModal
          requiredAction={selectedAction}
          onClose={() => setSelectedAction(undefined)}
        />
      )}
      <DraggableTable
        keyField="name"
        onDragFinish={async (nameDragged, items) => {
          const keys = actions.map((e) => e.name);
          const newIndex = items.indexOf(nameDragged);
          const oldIndex = keys.indexOf(nameDragged);
          const dragged = actions[oldIndex].data;
          if (!dragged.alias) return;

          const times = newIndex - oldIndex;
          await executeMove(dragged, times);
        }}
        columns={[
          {
            name: "name",
            displayKey: "action",
            width: 50,
          },
          {
            name: "enabled",
            displayKey: "enabled",
            cellRenderer: (row) => (
              <Switch
                id={`enable-${toKey(row.name || "")}`}
                label={t("on")}
                labelOff={t("off")}
                isChecked={row.enabled}
                onChange={async () => {
                  await updateAction(row.data, "enabled");
                }}
                aria-label={row.name}
              />
            ),
            width: 20,
          },
          {
            name: "default",
            displayKey: "setAsDefaultAction",
            thTooltipText: "authDefaultActionTooltip",
            cellRenderer: (row) => (
              <Switch
                id={`default-${toKey(row.name || "")}`}
                label={t("on")}
                isDisabled={!row.enabled}
                labelOff={!row.enabled ? t("disabledOff") : t("off")}
                isChecked={row.defaultAction}
                onChange={async () => {
                  await updateAction(row.data, "defaultAction");
                }}
                aria-label={row.name}
              />
            ),
            width: 20,
          },
          {
            name: "config",
            displayKey: "configure",
            cellRenderer: (row) =>
              row.data.configurable ? (
                <Button
                  variant="plain"
                  aria-label={t("settings")}
                  onClick={() => setSelectedAction(row.data)}
                >
                  <CogIcon />
                </Button>
              ) : undefined,
            width: 10,
          },
        ]}
        data={actions}
      />
    </>
  );
};
