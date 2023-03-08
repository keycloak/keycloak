import { useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertVariant, Switch } from "@patternfly/react-core";

import type RequiredActionProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import type RequiredActionProviderSimpleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderSimpleRepresentation";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { DraggableTable } from "./components/DraggableTable";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { toKey } from "../util";

type DataType = RequiredActionProviderRepresentation &
  RequiredActionProviderSimpleRepresentation;

type Row = {
  name: string;
  enabled: boolean;
  defaultAction: boolean;
  data: DataType;
};

export const RequiredActions = () => {
  const { t } = useTranslation("authentication");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [actions, setActions] = useState<Row[]>();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  useFetch(
    async () => {
      const [requiredActions, unregisteredRequiredActions] = await Promise.all([
        adminClient.authenticationManagement.getRequiredActions(),
        adminClient.authenticationManagement.getUnregisteredRequiredActions(),
      ]);
      return [
        ...requiredActions.map((a) => ({
          name: a.name!,
          enabled: a.enabled!,
          defaultAction: a.defaultAction!,
          data: a,
        })),
        ...unregisteredRequiredActions.map((a) => ({
          name: a.name!,
          enabled: false,
          defaultAction: false,
          data: a,
        })),
      ];
    },
    (actions) => setActions(actions),
    [key]
  );

  const isUnregisteredAction = (data: DataType): boolean => {
    return !("alias" in data);
  };

  const updateAction = async (
    action: DataType,
    field: "enabled" | "defaultAction"
  ) => {
    try {
      if (field in action) {
        action[field] = !action[field];
        await adminClient.authenticationManagement.updateRequiredAction(
          { alias: action.alias! },
          action
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
      addError("authentication:updatedRequiredActionError", error);
    }
  };

  const executeMove = async (
    action: RequiredActionProviderRepresentation,
    times: number
  ) => {
    try {
      const alias = action.alias!;
      for (let index = 0; index < Math.abs(times); index++) {
        if (times > 0) {
          await adminClient.authenticationManagement.lowerRequiredActionPriority(
            {
              alias,
            }
          );
        } else {
          await adminClient.authenticationManagement.raiseRequiredActionPriority(
            {
              alias,
            }
          );
        }
      }
      refresh();

      addAlert(t("updatedRequiredActionSuccess"), AlertVariant.success);
    } catch (error) {
      addError("authentication:updatedRequiredActionError", error);
    }
  };

  if (!actions) {
    return <KeycloakSpinner />;
  }

  return (
    <DraggableTable
      keyField="name"
      onDragFinish={async (nameDragged, items) => {
        const keys = actions.map((e) => e.name);
        const newIndex = items.indexOf(nameDragged);
        const oldIndex = keys.indexOf(nameDragged);
        const dragged = actions[oldIndex].data;
        if (!dragged.alias) return;

        const times = newIndex - oldIndex;
        executeMove(dragged, times);
      }}
      columns={[
        {
          name: "name",
          displayKey: "authentication:requiredActions",
        },
        {
          name: "enabled",
          displayKey: "common:enabled",
          cellRenderer: (row) => (
            <Switch
              id={`enable-${toKey(row.name)}`}
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={row.enabled}
              onChange={() => {
                updateAction(row.data, "enabled");
              }}
              aria-label={toKey(row.name)}
            />
          ),
        },
        {
          name: "default",
          displayKey: "authentication:setAsDefaultAction",
          thTooltipText: "authentication-help:authDefaultActionTooltip",
          cellRenderer: (row) => (
            <Switch
              id={`default-${toKey(row.name)}`}
              label={t("common:on")}
              isDisabled={!row.enabled}
              labelOff={!row.enabled ? t("disabledOff") : t("common:off")}
              isChecked={row.defaultAction}
              onChange={() => {
                updateAction(row.data, "defaultAction");
              }}
              aria-label={toKey(row.name)}
            />
          ),
        },
      ]}
      data={actions}
    />
  );
};
