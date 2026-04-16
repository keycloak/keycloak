import { Action, KeycloakDataTable, ListEmptyState, useAlerts } from "@keycloak/keycloak-ui-shared";
import type EventHookProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookProviderRepresentation";
import type EventHookTargetRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTargetRepresentation";
import type EventHookTestResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTestResultRepresentation";
import { Alert, AlertVariant, Button, ButtonVariant, Switch, Tooltip } from "@patternfly/react-core";
import { InfoCircleIcon, WarningTriangleIcon } from "@patternfly/react-icons";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../../context/realm-context/RealmContext";
import useFormatDate from "../../utils/useFormatDate";
import { EventHookTargetDialog } from "./EventHookTargetDialog";
import { toEvents } from "../routes/Events";

export const EventHookTargets = () => {
    const { adminClient } = useAdminClient();
    const { realm } = useRealm();
    const { t } = useTranslation();
    const formatDate = useFormatDate();
    const navigate = useNavigate();
    const { addAlert, addError } = useAlerts();
    const [refreshCount, setRefreshCount] = useState(0);
    const [selectedTarget, setSelectedTarget] = useState<EventHookTargetRepresentation>();
    const [dialogOpen, setDialogOpen] = useState(false);
    const [providers, setProviders] = useState<EventHookProviderRepresentation[]>([]);
    const [unknownTargets, setUnknownTargets] = useState<EventHookTargetRepresentation[]>([]);

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

        const loadedProviderIds = new Set(loadedProviders.map((provider) => provider.id));
        const sortedTargets = [...loadedTargets].sort((a, b) =>
            (a.name || "").localeCompare(b.name || ""),
        ) as EventHookTargetRepresentation[];

        setProviders(
            [...loadedProviders].sort((a, b) => a.id.localeCompare(b.id)),
        );
        setUnknownTargets(
            sortedTargets.filter(
                (target) => Boolean(target.type && !loadedProviderIds.has(target.type)),
            ),
        );

        return sortedTargets;
    };

    const isUnknownTargetType = (target: EventHookTargetRepresentation) =>
        Boolean(target.type && !providerIds.has(target.type));

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
        refresh();
    };

    const onDialogClose = () => {
        setDialogOpen(false);
        setSelectedTarget(undefined);
    };

    const testFailureMessage = (result: EventHookTestResultRepresentation) => {
        const parts = [result.statusCode, result.details].filter(
            (value): value is string => Boolean(value && value.trim()),
        );

        return parts.join(" - ");
    };

    const testTarget = async (target: EventHookTargetRepresentation) => {
        if (isUnknownTargetType(target)) {
            notifyUnknownTargetType(target);
            return;
        }

        try {
            const result = await adminClient.eventHooks.testTarget({
                realm,
                ...target,
            });
            const details = testFailureMessage(result);

            addAlert(
                result.success
                    ? t("eventHookTargetTestSucceeded")
                    : details
                        ? t("eventHookTargetTestFailed", { details })
                        : t("eventHookTargetTestFailedWithoutDetails"),
                result.success ? AlertVariant.success : AlertVariant.danger,
            );
        } catch (error) {
            addError("eventHookTargetTestError", error);
        }
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
        () => [
            {
                title: t("edit"),
                onRowClick: (target: EventHookTargetRepresentation) => openEditDialog(target),
            } as Action<EventHookTargetRepresentation>,
            {
                title: t("test"),
                onRowClick: (target: EventHookTargetRepresentation) => testTarget(target),
            } as Action<EventHookTargetRepresentation>,
            {
                title: t("delete"),
                onRowClick: (target: EventHookTargetRepresentation) => {
                    setSelectedTarget(target);
                    toggleDeleteDialog();
                },
            } as Action<EventHookTargetRepresentation>,
        ],
        [t, testTarget, toggleDeleteDialog],
    );

    const toggleEnabled = async (target: EventHookTargetRepresentation) => {
        if (isUnknownTargetType(target)) {
            notifyUnknownTargetType(target);
            return;
        }

        try {
            await adminClient.eventHooks.updateTarget({
                realm,
                targetId: target.id!,
            }, {
                ...target,
                enabled: !(target.enabled ?? false),
            });
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
                            .map((target) => `${target.name || target.id || target.type} (${target.type || "?"})`)
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
            <KeycloakDataTable
                key={refreshCount}
                loader={loader}
                ariaLabelKey="eventHookTargets"
                toolbarItem={
                    <Button data-testid="create-event-hook-target" onClick={openCreateDialog}>
                        {t("createEventHookTarget")}
                    </Button>
                }
                columns={[
                    {
                        name: "name",
                        displayKey: "name",
                        cellRenderer: (target: EventHookTargetRepresentation) => (
                            <div className="pf-v5-u-display-flex pf-v5-u-align-items-center pf-v5-u-gap-sm">
                                <span>{target.name || ""}</span>
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
                                            icon={<WarningTriangleIcon color="var(--pf-v5-global--warning-color--100)" />}
                                            onClick={(event) => {
                                                event.stopPropagation();
                                                openLogsForTarget(target);
                                            }}
                                            variant="plain"
                                        />
                                    </Tooltip>
                                )}
                            </div>
                        ),
                    },
                    {
                        name: "type",
                        displayKey: "provider",
                        cellRenderer: (target: EventHookTargetRepresentation) => (
                            <div>
                                <div>{target.type || ""}</div>
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
                            <Switch
                                id={`event-hook-enabled-${target.id}`}
                                label={t("enabled")}
                                labelOff={t("disabled")}
                                isDisabled={isUnknownTargetType(target)}
                                isChecked={target.enabled ?? false}
                                onChange={() => toggleEnabled(target)}
                            />
                        ),
                    },
                    {
                        name: "updatedAt",
                        displayKey: "lastUpdated",
                        cellRenderer: (target: EventHookTargetRepresentation) =>
                            target.updatedAt ? formatDate(new Date(target.updatedAt)) : "",
                    },
                ]}
                actions={targetActions}
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
