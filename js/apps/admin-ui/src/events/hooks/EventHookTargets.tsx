import {
  Action,
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import type EventHookProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookProviderRepresentation";
import type EventHookTargetRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTargetRepresentation";
import type EventHookTestResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTestResultRepresentation";
import {
  Alert,
  AlertVariant,
  Button,
  ButtonVariant,
  Switch,
  Tooltip,
} from "@patternfly/react-core";
import { InfoCircleIcon, WarningTriangleIcon } from "@patternfly/react-icons";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../../context/realm-context/RealmContext";
import useFormatDate from "../../utils/useFormatDate";
import { EventHookTargetDialog } from "./EventHookTargetDialog";
import { EventHookTargetTestDialog } from "./EventHookTargetTestDialog";
import { EventHookTargetTypeIcon } from "./EventHookTargetTypeIcon";
import { toEvents } from "../routes/Events";

export const EventHookTargets = () => {
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const formatDate = useFormatDate();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { addAlert, addError } = useAlerts();
  const [refreshCount, setRefreshCount] = useState(0);
  const [selectedTarget, setSelectedTarget] =
    useState<EventHookTargetRepresentation>();
  const [testTarget, setTestTarget] = useState<EventHookTargetRepresentation>();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [providers, setProviders] = useState<EventHookProviderRepresentation[]>(
    [],
  );
  const [providersLoaded, setProvidersLoaded] = useState(false);
  const [unknownTargets, setUnknownTargets] = useState<
    EventHookTargetRepresentation[]
  >([]);
  const [targets, setTargets] = useState<EventHookTargetRepresentation[]>([]);

  const targetId = searchParams.get("targetId") || "";

  const providerIds = useMemo(
    () => new Set(providers.map((provider) => provider.id)),
    [providers],
  );

  const refresh = () => setRefreshCount((count) => count + 1);

  const loader = async () => {
    const [loadedTargets, loadedProviders] = await Promise.all([
      adminClient.eventHooks.findTargets({ realm }),
      adminClient.eventHooks.findProviders({ realm }),
    ]);

    const loadedProviderIds = new Set(
      loadedProviders.map((provider) => provider.id),
    );
    const sortedTargets = [...loadedTargets].sort((a, b) =>
      (a.name || "").localeCompare(b.name || ""),
    ) as EventHookTargetRepresentation[];

    setProviders([...loadedProviders].sort((a, b) => a.id.localeCompare(b.id)));
    setTargets(sortedTargets);
    setProvidersLoaded(true);
    setUnknownTargets(
      sortedTargets.filter((target) =>
        Boolean(target.type && !loadedProviderIds.has(target.type)),
      ),
    );

    return sortedTargets;
  };

  const isUnknownTargetType = (target: EventHookTargetRepresentation) =>
    providersLoaded && Boolean(target.type && !providerIds.has(target.type));

  const notifyUnknownTargetType = (target: EventHookTargetRepresentation) => {
    addAlert(
      t("eventHookTargetUnknownTypeActionError", {
        name: target.name || target.id || target.type || "",
        type: target.type || "",
      }),
      AlertVariant.warning,
    );
  };

  const openCreateDialog = () => {
    setSelectedTarget(undefined);
    setDialogOpen(true);
  };

  const openEditDialog = (target: EventHookTargetRepresentation) => {
    if (isUnknownTargetType(target)) {
      notifyUnknownTargetType(target);
      return;
    }

    setSelectedTarget(target);
    setDialogOpen(true);
  };

  const onDialogSaved = () => {
    setDialogOpen(false);
    setSelectedTarget(undefined);
    clearTargetSearchParam();
    refresh();
  };

  const onDialogClose = () => {
    setDialogOpen(false);
    setSelectedTarget(undefined);
    clearTargetSearchParam();
  };

  const clearTargetSearchParam = () => {
    if (!searchParams.has("targetId")) {
      return;
    }

    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete("targetId");
    setSearchParams(nextSearchParams, { replace: true });
  };

  useEffect(() => {
    if (!targetId || dialogOpen || targets.length === 0) {
      return;
    }

    const target = targets.find((entry) => entry.id === targetId);
    if (!target) {
      return;
    }

    openEditDialog(target);
  }, [dialogOpen, targetId, targets]);

  const openTestDialog = (target: EventHookTargetRepresentation) => {
    if (isUnknownTargetType(target)) {
      notifyUnknownTargetType(target);
      return;
    }

    setTestTarget(target);
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "eventHookTargetDeleteConfirmTitle",
    messageKey: t("eventHookTargetDeleteConfirm", {
      name: selectedTarget?.name || "",
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.eventHooks.delTarget({
          realm,
          targetId: selectedTarget!.id!,
        });
        setSelectedTarget(undefined);
        addAlert(t("eventHookTargetDeleted"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("eventHookTargetDeleteError", error);
      }
    },
  });

  const targetActions = useMemo(
    () => (target: EventHookTargetRepresentation) => [
      {
        title: t("edit"),
        onClick: () => openEditDialog(target),
      } as Action<EventHookTargetRepresentation>,
      {
        title: t("test"),
        onClick: () => openTestDialog(target),
      } as Action<EventHookTargetRepresentation>,
      {
        title: t("delete"),
        onClick: () => {
          setSelectedTarget(target);
          toggleDeleteDialog();
        },
      } as Action<EventHookTargetRepresentation>,
    ],
    [t, toggleDeleteDialog],
  );

  const toggleEnabled = async (target: EventHookTargetRepresentation) => {
    if (isUnknownTargetType(target)) {
      notifyUnknownTargetType(target);
      return;
    }

    try {
      await adminClient.eventHooks.updateTarget(
        {
          realm,
          targetId: target.id!,
        },
        {
          ...target,
          enabled: !(target.enabled ?? false),
        },
      );
      addAlert(t("eventHookTargetUpdated"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("eventHookTargetUpdateError", error);
    }
  };

  const openLogsForTarget = (target: EventHookTargetRepresentation) => {
    if (!target.id) {
      return;
    }

    navigate({
      ...toEvents({ realm, tab: "hooks", subTab: "logs" }),
      search: new URLSearchParams({ targetId: target.id }).toString(),
    });
  };

  const isAutoDisabled = (target: EventHookTargetRepresentation) =>
    Boolean(
      target.autoDisabled &&
        target.autoDisabledUntil &&
        target.autoDisabledUntil > Date.now(),
    );

  const autoDisabledLabel = (target: EventHookTargetRepresentation) =>
    target.autoDisabledUntil
      ? t("eventHookTargetAutoDisabledUntil", {
          date: formatDate(new Date(target.autoDisabledUntil)),
        })
      : t("eventHookTargetAutoDisabled");

  return (
    <>
      <DeleteConfirm />
      {unknownTargets.length > 0 && (
        <Alert
          isInline
          title={t("eventHookTargetUnknownTypeAlertTitle")}
          variant="warning"
        >
          {t("eventHookTargetUnknownTypeAlertDescription", {
            targets: unknownTargets
              .map(
                (target) =>
                  `${target.name || target.id || target.type} (${target.type || "?"})`,
              )
              .join(", "),
          })}
        </Alert>
      )}
      {dialogOpen && providers.length > 0 && (
        <EventHookTargetDialog
          target={selectedTarget}
          providers={providers}
          onClose={onDialogClose}
          onSaved={onDialogSaved}
        />
      )}
      {testTarget && (
        <EventHookTargetTestDialog
          target={testTarget}
          onClose={() => setTestTarget(undefined)}
        />
      )}
      <KeycloakDataTable
        key={refreshCount}
        loader={loader}
        ariaLabelKey="eventHookTargets"
        toolbarItem={
          <Button
            data-testid="create-event-hook-target"
            onClick={openCreateDialog}
          >
            {t("createEventHookTarget")}
          </Button>
        }
        columns={[
          {
            name: "name",
            displayKey: "name",
            cellRenderer: (target: EventHookTargetRepresentation) => (
              <div className="pf-v5-u-display-flex pf-v5-u-align-items-center pf-v5-u-gap-sm">
                {target.status === "NOT_USED" && (
                  <Tooltip content={t("eventHookTargetNotUsed")}>
                    <InfoCircleIcon color="var(--pf-v5-global--info-color--100)" />
                  </Tooltip>
                )}
                {target.status === "HAS_PROBLEMS" && (
                  <Tooltip content={t("eventHookTargetHasProblems")}>
                    <Button
                      aria-label={t("openEventHookTargetProblemLogs", {
                        name: target.name || target.id || "",
                      })}
                      className="pf-v5-u-p-0"
                      icon={
                        <WarningTriangleIcon color="var(--pf-v5-global--warning-color--100)" />
                      }
                      onClick={(event) => {
                        event.stopPropagation();
                        openLogsForTarget(target);
                      }}
                      variant="plain"
                    />
                  </Tooltip>
                )}
                <span>{target.name || ""}</span>
              </div>
            ),
          },
          {
            name: "type",
            displayKey: "provider",
            cellRenderer: (target: EventHookTargetRepresentation) => (
              <div>
                <div className="pf-v5-u-display-flex pf-v5-u-align-items-center pf-v5-u-gap-sm">
                  <EventHookTargetTypeIcon type={target.type} />
                  <span>{target.type || ""}</span>
                </div>
                {isUnknownTargetType(target) && (
                  <div className="pf-v5-u-color-warning-200 pf-v5-u-display-flex pf-v5-u-align-items-center pf-v5-u-gap-sm pf-v5-u-font-size-sm">
                    <WarningTriangleIcon color="var(--pf-v5-global--warning-color--100)" />
                    <span>{t("eventHookTargetUnknownType")}</span>
                  </div>
                )}
                {target.displayInfo && (
                  <div
                    className="pf-v5-u-color-200 pf-v5-u-font-size-sm"
                    style={{ wordBreak: "break-word" }}
                  >
                    {target.displayInfo}
                  </div>
                )}
              </div>
            ),
          },
          {
            name: "enabled",
            displayKey: "status",
            cellRenderer: (target: EventHookTargetRepresentation) => (
              <div>
                <Switch
                  id={`event-hook-enabled-${target.id}`}
                  label={t("enabled")}
                  labelOff={t("disabled")}
                  isDisabled={isUnknownTargetType(target)}
                  isChecked={target.enabled ?? false}
                  onChange={() => toggleEnabled(target)}
                />
                {isAutoDisabled(target) && (
                  <Tooltip
                    content={
                      target.autoDisabledReason ||
                      t("eventHookTargetAutoDisabledReason")
                    }
                  >
                    <div className="pf-v5-u-color-warning-200 pf-v5-u-display-flex pf-v5-u-align-items-center pf-v5-u-gap-sm pf-v5-u-font-size-sm pf-v5-u-mt-xs">
                      <WarningTriangleIcon color="var(--pf-v5-global--warning-color--100)" />
                      <span>{autoDisabledLabel(target)}</span>
                    </div>
                  </Tooltip>
                )}
              </div>
            ),
          },
          {
            name: "updatedAt",
            displayKey: "lastUpdated",
            cellRenderer: (target: EventHookTargetRepresentation) =>
              target.updatedAt ? formatDate(new Date(target.updatedAt)) : "",
          },
        ]}
        actionResolver={({ data }) =>
          targetActions(data as EventHookTargetRepresentation)
        }
        emptyState={
          <ListEmptyState
            message={t("emptyEventHookTargets")}
            instructions={t("emptyEventHookTargetsInstructions")}
            primaryActionText={t("createEventHookTarget")}
            onPrimaryAction={openCreateDialog}
          />
        }
      />
    </>
  );
};
