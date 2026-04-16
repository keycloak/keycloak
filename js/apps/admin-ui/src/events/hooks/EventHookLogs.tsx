import type EventHookLogRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookLogRepresentation";
import type EventHookTargetRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTargetRepresentation";
import { Action, KeycloakDataTable, KeycloakSelect, ListEmptyState, useAlerts } from "@keycloak/keycloak-ui-shared";
import {
    AlertVariant,
    Button,
    Popover,
    SearchInput,
    SelectOption,
    Spinner,
    ToolbarItem,
} from "@patternfly/react-core";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toEvents } from "../routes/Events";
import useFormatDate from "../../utils/useFormatDate";

type EventHookLogRecord = EventHookLogRepresentation & {
    executionId?: string;
    batchExecution?: boolean;
    sourceType?: string;
    sourceEventId?: string;
    sourceEventName?: string;
    targetName?: string;
};

type EventHookLogRow = EventHookLogRecord & {
    eventCount: number;
    groupedLogs: EventHookLogRecord[];
};

const eventTab = (sourceType?: string) =>
    sourceType === "ADMIN" ? "admin-events" : "user-events";

const EventLink = ({
    realm,
    log,
}: {
    realm: string;
    log: EventHookLogRecord;
}) => {
    const label = log.sourceEventName || log.sourceEventId || log.messageId || "-";

    if (!log.sourceType) {
        return <>{label}</>;
    }

    return (
        <Link to={toEvents({ realm, tab: eventTab(log.sourceType) })}>
            {label}
        </Link>
    );
};

const BatchExecutionDetails = ({
    realm,
    executionId,
    fallbackLogs,
}: {
    realm: string;
    executionId: string;
    fallbackLogs: EventHookLogRecord[];
}) => {
    const { adminClient } = useAdminClient();
    const { t } = useTranslation();
    const [logs, setLogs] = useState<EventHookLogRecord[]>(fallbackLogs);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let active = true;

        adminClient.eventHooks
            .findLogs({ realm, executionId, max: 100 } as any)
            .then((loadedLogs) => {
                if (active) {
                    setLogs(loadedLogs as EventHookLogRecord[]);
                }
            })
            .finally(() => {
                if (active) {
                    setLoading(false);
                }
            });

        return () => {
            active = false;
        };
    }, [adminClient, executionId, realm]);

    if (loading) {
        return <Spinner size="md" />;
    }

    if (logs.length === 0) {
        return <div>{t("emptyEventHookBatchExecution")}</div>;
    }

    return (
        <div>
            <div>{t("eventHookBatchExecutionPreviewLimit", { count: 100 })}</div>
            <ul>
                {logs.map((log) => (
                    <li key={log.id || `${log.executionId}-${log.messageId}`}>
                        <EventLink realm={realm} log={log} />
                        {log.messageId && ` (${log.messageId})`}
                    </li>
                ))}
            </ul>
        </div>
    );
};

const BatchExecutionCell = ({
    realm,
    log,
    eventCount,
    groupedLogs,
}: {
    realm: string;
    log: EventHookLogRecord;
    eventCount: number;
    groupedLogs: EventHookLogRecord[];
}) => {
    const { t } = useTranslation();

    if (!log.executionId) {
        return <>{t("eventHookBatchExecution", { count: eventCount })}</>;
    }

    return (
        <Popover
            headerContent={t("eventHookBatchExecution", { count: eventCount })}
            bodyContent={
                <BatchExecutionDetails
                    realm={realm}
                    executionId={log.executionId}
                    fallbackLogs={groupedLogs}
                />
            }
        >
            <Button isInline variant="link">
                {t("eventHookBatchExecution", { count: eventCount })}
            </Button>
        </Popover>
    );
};

export const EventHookLogs = () => {
    const { adminClient } = useAdminClient();
    const { realm } = useRealm();
    const { t } = useTranslation();
    const formatDate = useFormatDate();
    const { addAlert, addError } = useAlerts();
    const [searchParams, setSearchParams] = useSearchParams();
    const [targetTypeOpen, setTargetTypeOpen] = useState(false);
    const [targetOpen, setTargetOpen] = useState(false);
    const [targets, setTargets] = useState<EventHookTargetRepresentation[]>([]);
    const [searchValue, setSearchValue] = useState(searchParams.get("search") || "");
    const [refreshCount, setRefreshCount] = useState(0);

    const targetId = searchParams.get("targetId") || "";
    const targetType = searchParams.get("targetType") || "";
    const search = searchParams.get("search") || "";

    useEffect(() => {
        setSearchValue(search);
    }, [search]);

    useEffect(() => {
        adminClient.eventHooks.findTargets({ realm }).then(setTargets);
    }, [adminClient, realm]);

    const refresh = () => setRefreshCount((count) => count + 1);

    const isRetryableMessageStatus = (status?: string) =>
        status === "FAILED" || status === "EXHAUSTED" || status === "DEAD";

    const getRetryableMessageIds = (log: EventHookLogRow) =>
        [...new Set(
            (log.groupedLogs.length > 0 ? log.groupedLogs : [log])
                .filter((entry) => entry.messageId && isRetryableMessageStatus(entry.messageStatus))
                .map((entry) => entry.messageId as string),
        )];

    const retryLogs = async (log: EventHookLogRow) => {
        const messageIds = getRetryableMessageIds(log);

        if (messageIds.length === 0) {
            addAlert(t("eventHookRetryUnavailable"), AlertVariant.warning);
            return;
        }

        const results = await Promise.allSettled(
            messageIds.map((messageId) => adminClient.eventHooks.retryMessage({ realm, messageId })),
        );

        const successfulRetries = results.filter((result) => result.status === "fulfilled").length;

        if (successfulRetries === messageIds.length) {
            addAlert(t("eventHookRetryQueued", { count: successfulRetries }), AlertVariant.success);
            refresh();
            return;
        }

        if (successfulRetries > 0) {
            addAlert(
                t("eventHookRetryPartiallyQueued", {
                    count: successfulRetries,
                    total: messageIds.length,
                }),
                AlertVariant.warning,
            );
            refresh();
            return;
        }

        addError("eventHookRetryError", (results.find((result) => result.status === "rejected") as PromiseRejectedResult).reason);
    };

    const updateFilters = (updates: Record<string, string>) => {
        const next = new URLSearchParams(searchParams);

        Object.entries(updates).forEach(([key, value]) => {
            if (value) {
                next.set(key, value);
            } else {
                next.delete(key);
            }
        });

        setSearchParams(next, { replace: true });
    };

    const targetTypes = useMemo(
        () =>
            [...new Set(targets.map((target) => target.type).filter((value): value is string => Boolean(value)))].sort((a, b) =>
                a.localeCompare(b),
            ),
        [targets],
    );

    const visibleTargets = useMemo(
        () =>
            [...targets]
                .filter((target) => !targetType || target.type === targetType)
                .sort((a, b) => (a.name || "").localeCompare(b.name || "")),
        [targetType, targets],
    );

    const loader = async () => {
        const [logs, loadedTargets] = await Promise.all([
            adminClient.eventHooks.findLogs({
                realm,
                targetId: targetId || undefined,
                targetType: targetType || undefined,
                search: search || undefined,
            }),
            adminClient.eventHooks.findTargets({ realm }),
        ]);
        const targetNames = new Map(
            loadedTargets.map((target: EventHookTargetRepresentation) => [target.id, target.name]),
        );

        const grouped = new Map<string, EventHookLogRow>();

        [...logs]
            .sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
            .forEach((log) => {
                const key = log.executionId || log.id || `${log.messageId}-${log.createdAt}`;
                const decoratedLog: EventHookLogRecord = {
                    ...log,
                    targetName: targetNames.get(log.targetId) || log.targetId,
                };
                const existing = grouped.get(key);

                if (existing) {
                    existing.groupedLogs.push(decoratedLog);
                    existing.eventCount += 1;
                    existing.batchExecution = true;
                    return;
                }

                grouped.set(key, {
                    ...decoratedLog,
                    eventCount: 1,
                    groupedLogs: [decoratedLog],
                });
            });

        return [...grouped.values()].sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
    };

    return (
        <KeycloakDataTable
            key={refreshCount}
            loader={loader}
            ariaLabelKey="eventHookLogs"
            isSearching={Boolean(targetId || targetType || search)}
            toolbarItem={
                <>
                    <ToolbarItem>
                        <KeycloakSelect
                            aria-label={t("eventHookTargetTypeFilter")}
                            isOpen={targetTypeOpen}
                            onSelect={(value) => {
                                updateFilters({ targetType: value.toString(), targetId: "" });
                                setTargetTypeOpen(false);
                            }}
                            onToggle={() => setTargetTypeOpen(!targetTypeOpen)}
                            placeholderText={t("eventHookTargetTypeFilter")}
                            selections={targetType || t("allEventHookTargetTypes")}
                            variant="single"
                        >
                            <SelectOption value="">{t("allEventHookTargetTypes")}</SelectOption>
                            {targetTypes.map((option) => (
                                <SelectOption key={option} value={option}>
                                    {option}
                                </SelectOption>
                            ))}
                        </KeycloakSelect>
                    </ToolbarItem>
                    <ToolbarItem>
                        <KeycloakSelect
                            aria-label={t("eventHookTargetFilter")}
                            isOpen={targetOpen}
                            onSelect={(value) => {
                                updateFilters({ targetId: value.toString() });
                                setTargetOpen(false);
                            }}
                            onToggle={() => setTargetOpen(!targetOpen)}
                            placeholderText={t("eventHookTargetFilter")}
                            selections={
                                visibleTargets.find((target) => target.id === targetId)?.name ||
                                targetId ||
                                t("allEventHookTargets")
                            }
                            variant="single"
                        >
                            <SelectOption value="">{t("allEventHookTargets")}</SelectOption>
                            {visibleTargets.map((target) => (
                                <SelectOption key={target.id} value={target.id || ""}>
                                    {target.name || target.id || ""}
                                </SelectOption>
                            ))}
                        </KeycloakSelect>
                    </ToolbarItem>
                    <ToolbarItem>
                        <SearchInput
                            aria-label={t("searchEventHookLogs")}
                            onChange={(_, value) => setSearchValue(value)}
                            onClear={() => {
                                setSearchValue("");
                                updateFilters({ search: "" });
                            }}
                            onSearch={() => updateFilters({ search: searchValue.trim() })}
                            placeholder={t("searchEventHookLogs")}
                            value={searchValue}
                        />
                    </ToolbarItem>
                </>
            }
            columns={[
                {
                    name: "createdAt",
                    displayKey: "time",
                    cellRenderer: (log: EventHookLogRepresentation) =>
                        log.createdAt ? formatDate(new Date(log.createdAt)) : "",
                },
                {
                    name: "targetName",
                    displayKey: "target",
                },
                {
                    name: "event",
                    displayKey: "eventHookEvent",
                    cellRenderer: (log: EventHookLogRow) =>
                        log.batchExecution ? (
                            <BatchExecutionCell
                                realm={realm}
                                log={log}
                                eventCount={log.eventCount}
                                groupedLogs={log.groupedLogs}
                            />
                        ) : (
                            <EventLink realm={realm} log={log} />
                        ),
                },
                {
                    name: "status",
                    displayKey: "status",
                    cellRenderer: (log: EventHookLogRepresentation) =>
                        log.status ? t(log.status) : "",
                },
                {
                    name: "attemptNumber",
                    displayKey: "attempt",
                },
                {
                    name: "statusCode",
                    displayKey: "statusCode",
                },
                {
                    name: "durationMs",
                    displayKey: "durationMs",
                },
                {
                    name: "details",
                    displayKey: "details",
                },
            ]}
            actionResolver={({ data }) => {
                const log = data as EventHookLogRow;
                if (getRetryableMessageIds(log).length === 0) {
                    return [];
                }

                return [
                    {
                        title: t("eventHookRetryAction"),
                        onClick: () => retryLogs(log),
                    } as Action<EventHookLogRow>,
                ];
            }}
            emptyState={
                <ListEmptyState
                    message={t("emptyEventHookLogs")}
                    instructions={t("emptyEventHookLogsInstructions")}
                />
            }
        />
    );
};
