import type EventHookLogRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookLogRepresentation";
import type EventHookTargetRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookTargetRepresentation";
import {
    Action,
    KeycloakDataTable,
    KeycloakSelect,
    ListEmptyState,
    TextControl,
    useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
    ActionGroup,
    AlertVariant,
    Button,
    Chip,
    ChipGroup,
    DatePicker,
    Flex,
    FlexItem,
    Form,
    FormGroup,
    Popover,
    SelectOption,
    Spinner,
} from "@patternfly/react-core";
import { pickBy } from "lodash-es";
import { useEffect, useMemo, useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import DropdownPanel from "../../components/dropdown-panel/DropdownPanel";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toEvents } from "../routes/Events";
import useFormatDate from "../../utils/useFormatDate";

type EventHookLogRecord = EventHookLogRepresentation & {
    targetName?: string;
};

type EventHookLogRow = EventHookLogRecord & {
    eventCount: number;
    groupedLogs: EventHookLogRecord[];
};

type EventHookLogSearchForm = {
    sourceType: string;
    targetType: string;
    targetId: string;
    event: string;
    client: string;
    user: string;
    ipAddress: string;
    resourceType: string;
    resourcePath: string;
    status: string;
    messageStatus: string;
    dateFrom: string;
    dateTo: string;
    search: string;
};

const sourceTypeOptions = ["USER", "ADMIN"];
const logStatusOptions = ["WAITING", "PENDING", "SUCCESS", "FAILED"];
const messageStatusOptions = [
    "PENDING",
    "CLAIMED",
    "WAITING",
    "SUCCESS",
    "FAILED",
    "EXHAUSTED",
    "DEAD",
];

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
    const [searchDropdownOpen, setSearchDropdownOpen] = useState(false);
    const [sourceTypeOpen, setSourceTypeOpen] = useState(false);
    const [targetTypeOpen, setTargetTypeOpen] = useState(false);
    const [targetOpen, setTargetOpen] = useState(false);
    const [statusOpen, setStatusOpen] = useState(false);
    const [messageStatusOpen, setMessageStatusOpen] = useState(false);
    const [targets, setTargets] = useState<EventHookTargetRepresentation[]>([]);
    const [refreshCount, setRefreshCount] = useState(0);

    const sourceType = searchParams.get("sourceType") || "";
    const targetId = searchParams.get("targetId") || "";
    const targetType = searchParams.get("targetType") || "";
    const event = searchParams.get("event") || "";
    const client = searchParams.get("client") || "";
    const user = searchParams.get("user") || "";
    const ipAddress = searchParams.get("ipAddress") || "";
    const resourceType = searchParams.get("resourceType") || "";
    const resourcePath = searchParams.get("resourcePath") || "";
    const status = searchParams.get("status") || "";
    const messageStatus = searchParams.get("messageStatus") || "";
    const dateFrom = searchParams.get("dateFrom") || "";
    const dateTo = searchParams.get("dateTo") || "";
    const search = searchParams.get("search") || "";
    const defaultValues: EventHookLogSearchForm = {
        sourceType,
        targetType,
        targetId,
        event,
        client,
        user,
        ipAddress,
        resourceType,
        resourcePath,
        status,
        messageStatus,
        dateFrom,
        dateTo,
        search,
    };
    const filterLabels: Record<keyof EventHookLogSearchForm, string> = {
        sourceType: t("eventHookSourceTypeFilter"),
        targetType: t("eventHookTargetTypeFilter"),
        targetId: t("eventHookTargetFilter"),
        event: t("eventHookEventFilter"),
        client: t("client"),
        user: t("userId"),
        ipAddress: t("ipAddress"),
        resourceType: t("resourceType"),
        resourcePath: t("resourcePath"),
        status: t("eventHookStatusFilter"),
        messageStatus: t("eventHookMessageStatusFilter"),
        dateFrom: t("dateFrom"),
        dateTo: t("dateTo"),
        search: t("searchEventHookLogs"),
    };
    const form = useForm<EventHookLogSearchForm>({
        mode: "onChange",
        defaultValues,
    });
    const {
        control,
        getValues,
        handleSubmit,
        reset,
        setValue,
        watch,
        formState: { isDirty },
    } = form;
    const formTargetType = watch("targetType");
    const activeFilters = useMemo(
        () =>
            pickBy(defaultValues, (value) => Boolean(value.trim())) as Partial<EventHookLogSearchForm>,
        [defaultValues],
    );

    useEffect(() => {
        reset(defaultValues);
    }, [reset, sourceType, targetId, targetType, event, client, user, ipAddress, resourceType, resourcePath, status, messageStatus, dateFrom, dateTo, search]);

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

    const updateFilters = (updates: Partial<EventHookLogSearchForm>) => {
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

    const commitFilters = (values = getValues()) => {
        const nextFilters = pickBy(values, (value) => Boolean(value.trim())) as Partial<EventHookLogSearchForm>;

        updateFilters({
            sourceType: nextFilters.sourceType || "",
            targetType: nextFilters.targetType || "",
            targetId: nextFilters.targetId || "",
            event: nextFilters.event || "",
            client: nextFilters.client || "",
            user: nextFilters.user || "",
            ipAddress: nextFilters.ipAddress || "",
            resourceType: nextFilters.resourceType || "",
            resourcePath: nextFilters.resourcePath || "",
            status: nextFilters.status || "",
            messageStatus: nextFilters.messageStatus || "",
            dateFrom: nextFilters.dateFrom || "",
            dateTo: nextFilters.dateTo || "",
            search: nextFilters.search || "",
        });
    };

    const onSubmit = () => {
        setSearchDropdownOpen(false);
        commitFilters();
    };

    const resetSearch = () => {
        const emptyValues: EventHookLogSearchForm = {
            sourceType: "",
            targetType: "",
            targetId: "",
            event: "",
            client: "",
            user: "",
            ipAddress: "",
            resourceType: "",
            resourcePath: "",
            status: "",
            messageStatus: "",
            dateFrom: "",
            dateTo: "",
            search: "",
        };

        reset(emptyValues);
        commitFilters(emptyValues);
    };

    const removeFilter = (key: keyof EventHookLogSearchForm) => {
        const formValues = { ...getValues(), [key]: "" };

        if (key === "targetType") {
            formValues.targetId = "";
        }

        reset(formValues);
        commitFilters(formValues);
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
                .filter((target) => !formTargetType || target.type === formTargetType)
                .sort((a, b) => (a.name || "").localeCompare(b.name || "")),
        [formTargetType, targets],
    );

    const selectedTargetName =
        targets.find((target) => target.id === targetId)?.name || targetId;

    const searchFormDisplay = () => (
        <FormProvider {...form}>
            <Flex
                direction={{ default: "column" }}
                spaceItems={{ default: "spaceItemsNone" }}
            >
                <FlexItem>
                    <DropdownPanel
                        buttonText={t("search")}
                        setSearchDropdownOpen={setSearchDropdownOpen}
                        searchDropdownOpen={searchDropdownOpen}
                        marginRight="2.5rem"
                        width="15vw"
                    >
                        <Form onSubmit={handleSubmit(onSubmit)} isHorizontal>
                            <FormGroup
                                label={t("eventHookSourceTypeFilter")}
                                fieldId="event-hook-log-source-type-filter"
                            >
                                <Controller
                                    name="sourceType"
                                    control={control}
                                    render={({ field }) => (
                                        <KeycloakSelect
                                            aria-label={t("eventHookSourceTypeFilter")}
                                            isOpen={sourceTypeOpen}
                                            onSelect={(value) => {
                                                field.onChange(value.toString());
                                                setSourceTypeOpen(false);
                                            }}
                                            onToggle={() => setSourceTypeOpen(!sourceTypeOpen)}
                                            placeholderText={t("eventHookSourceTypeFilter")}
                                            selections={field.value || t("allEventHookSourceTypes")}
                                            variant="single"
                                        >
                                            <SelectOption value="">{t("allEventHookSourceTypes")}</SelectOption>
                                            {sourceTypeOptions.map((option) => (
                                                <SelectOption key={option} value={option}>
                                                    {option}
                                                </SelectOption>
                                            ))}
                                        </KeycloakSelect>
                                    )}
                                />
                            </FormGroup>
                            <FormGroup
                                label={t("eventHookTargetTypeFilter")}
                                fieldId="event-hook-log-target-type-filter"
                            >
                                <Controller
                                    name="targetType"
                                    control={control}
                                    render={({ field }) => (
                                        <KeycloakSelect
                                            aria-label={t("eventHookTargetTypeFilter")}
                                            isOpen={targetTypeOpen}
                                            onSelect={(value) => {
                                                field.onChange(value.toString());
                                                setValue("targetId", "");
                                                setTargetTypeOpen(false);
                                            }}
                                            onToggle={() => setTargetTypeOpen(!targetTypeOpen)}
                                            placeholderText={t("eventHookTargetTypeFilter")}
                                            selections={field.value || t("allEventHookTargetTypes")}
                                            variant="single"
                                        >
                                            <SelectOption value="">{t("allEventHookTargetTypes")}</SelectOption>
                                            {targetTypes.map((option) => (
                                                <SelectOption key={option} value={option}>
                                                    {option}
                                                </SelectOption>
                                            ))}
                                        </KeycloakSelect>
                                    )}
                                />
                            </FormGroup>
                            <FormGroup
                                label={t("eventHookTargetFilter")}
                                fieldId="event-hook-log-target-filter"
                            >
                                <Controller
                                    name="targetId"
                                    control={control}
                                    render={({ field }) => (
                                        <KeycloakSelect
                                            aria-label={t("eventHookTargetFilter")}
                                            isOpen={targetOpen}
                                            onSelect={(value) => {
                                                field.onChange(value.toString());
                                                setTargetOpen(false);
                                            }}
                                            onToggle={() => setTargetOpen(!targetOpen)}
                                            placeholderText={t("eventHookTargetFilter")}
                                            selections={
                                                visibleTargets.find((target) => target.id === field.value)?.name ||
                                                field.value ||
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
                                    )}
                                />
                            </FormGroup>
                            <TextControl
                                name="event"
                                label={t("eventHookEventFilter")}
                                data-testid="event-hook-logs-event-field"
                            />
                            <TextControl
                                name="client"
                                label={t("client")}
                                data-testid="event-hook-logs-client-field"
                            />
                            <TextControl
                                name="user"
                                label={t("userId")}
                                data-testid="event-hook-logs-user-field"
                            />
                            <TextControl
                                name="ipAddress"
                                label={t("ipAddress")}
                                data-testid="event-hook-logs-ip-field"
                            />
                            <TextControl
                                name="resourceType"
                                label={t("resourceType")}
                                data-testid="event-hook-logs-resource-type-field"
                            />
                            <TextControl
                                name="resourcePath"
                                label={t("resourcePath")}
                                data-testid="event-hook-logs-resource-path-field"
                            />
                            <FormGroup
                                label={t("eventHookStatusFilter")}
                                fieldId="event-hook-log-status-filter"
                            >
                                <Controller
                                    name="status"
                                    control={control}
                                    render={({ field }) => (
                                        <KeycloakSelect
                                            aria-label={t("eventHookStatusFilter")}
                                            isOpen={statusOpen}
                                            onSelect={(value) => {
                                                field.onChange(value.toString());
                                                setStatusOpen(false);
                                            }}
                                            onToggle={() => setStatusOpen(!statusOpen)}
                                            placeholderText={t("eventHookStatusFilter")}
                                            selections={field.value || t("allEventHookStatuses")}
                                            variant="single"
                                        >
                                            <SelectOption value="">{t("allEventHookStatuses")}</SelectOption>
                                            {logStatusOptions.map((option) => (
                                                <SelectOption key={option} value={option}>
                                                    {option}
                                                </SelectOption>
                                            ))}
                                        </KeycloakSelect>
                                    )}
                                />
                            </FormGroup>
                            <FormGroup
                                label={t("eventHookMessageStatusFilter")}
                                fieldId="event-hook-log-message-status-filter"
                            >
                                <Controller
                                    name="messageStatus"
                                    control={control}
                                    render={({ field }) => (
                                        <KeycloakSelect
                                            aria-label={t("eventHookMessageStatusFilter")}
                                            isOpen={messageStatusOpen}
                                            onSelect={(value) => {
                                                field.onChange(value.toString());
                                                setMessageStatusOpen(false);
                                            }}
                                            onToggle={() => setMessageStatusOpen(!messageStatusOpen)}
                                            placeholderText={t("eventHookMessageStatusFilter")}
                                            selections={field.value || t("allEventHookMessageStatuses")}
                                            variant="single"
                                        >
                                            <SelectOption value="">{t("allEventHookMessageStatuses")}</SelectOption>
                                            {messageStatusOptions.map((option) => (
                                                <SelectOption key={option} value={option}>
                                                    {option}
                                                </SelectOption>
                                            ))}
                                        </KeycloakSelect>
                                    )}
                                />
                            </FormGroup>
                            <FormGroup label={t("dateFrom")} fieldId="event-hook-log-date-from">
                                <Controller
                                    name="dateFrom"
                                    control={control}
                                    render={({ field }) => (
                                        <DatePicker
                                            className="pf-v5-u-w-100"
                                            value={field.value}
                                            onChange={(_, value) => field.onChange(value)}
                                            inputProps={{ id: "event-hook-log-date-from" }}
                                        />
                                    )}
                                />
                            </FormGroup>
                            <FormGroup label={t("dateTo")} fieldId="event-hook-log-date-to">
                                <Controller
                                    name="dateTo"
                                    control={control}
                                    render={({ field }) => (
                                        <DatePicker
                                            className="pf-v5-u-w-100"
                                            value={field.value}
                                            onChange={(_, value) => field.onChange(value)}
                                            inputProps={{ id: "event-hook-log-date-to" }}
                                        />
                                    )}
                                />
                            </FormGroup>
                            <TextControl
                                name="search"
                                label={t("searchEventHookLogs")}
                                data-testid="event-hook-logs-search-field"
                            />
                            <ActionGroup>
                                <Button type="submit" isDisabled={!isDirty}>
                                    {t("search")}
                                </Button>
                                <Button
                                    variant="secondary"
                                    onClick={resetSearch}
                                    isDisabled={!isDirty}
                                >
                                    {t("resetBtn")}
                                </Button>
                            </ActionGroup>
                        </Form>
                    </DropdownPanel>
                </FlexItem>
                <FlexItem>
                    {Object.entries(activeFilters).length > 0 && (
                        <div className="keycloak__searchChips pf-v5-u-ml-md">
                            {Object.entries(activeFilters).map(([key, value]) => (
                                <ChipGroup
                                    className="pf-v5-u-mt-md pf-v5-u-mr-md"
                                    key={key}
                                    categoryName={filterLabels[key as keyof EventHookLogSearchForm]}
                                    onClick={() => removeFilter(key as keyof EventHookLogSearchForm)}
                                    isClosable
                                >
                                    <Chip isReadOnly>
                                        {key === "targetId" ? selectedTargetName : value}
                                    </Chip>
                                </ChipGroup>
                            ))}
                        </div>
                    )}
                </FlexItem>
            </Flex>
        </FormProvider>
    );

    const loader = async () => {
        const [logs, loadedTargets] = await Promise.all([
            adminClient.eventHooks.findLogs({
                realm,
                sourceType: sourceType || undefined,
                targetId: targetId || undefined,
                targetType: targetType || undefined,
                event: event || undefined,
                client: client || undefined,
                user: user || undefined,
                ipAddress: ipAddress || undefined,
                resourceType: resourceType || undefined,
                resourcePath: resourcePath || undefined,
                status: status || undefined,
                messageStatus: messageStatus || undefined,
                dateFrom: dateFrom || undefined,
                dateTo: dateTo || undefined,
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
            key={`${refreshCount}-${searchParams.toString()}`}
            loader={loader}
            ariaLabelKey="eventHookLogs"
            isSearching={Object.keys(activeFilters).length > 0}
            toolbarItem={searchFormDisplay()}
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
