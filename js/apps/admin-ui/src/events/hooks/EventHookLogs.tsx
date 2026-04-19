import type EventHookLogRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookLogRepresentation";
import type EventHookMessageRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventHookMessageRepresentation";
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
  Label,
  SelectOption,
  Spinner,
  Switch,
  Text,
  TextContent,
} from "@patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  InfoCircleIcon,
} from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { pickBy } from "lodash-es";
import { useEffect, useMemo, useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import DropdownPanel from "../../components/dropdown-panel/DropdownPanel";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toEvents } from "../routes/Events";
import { toEventHookLogs } from "./routes";
import useFormatDate from "../../utils/useFormatDate";

type EventHookLogRecord = EventHookLogRepresentation;

type EventHookMessageRecord = EventHookMessageRepresentation & {
  targetName?: string;
};

type EventHookExecutionRow = {
  executionId?: string;
  targetName?: string;
  message: EventHookMessageRecord;
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
  executionId: string;
};

const sourceTypeOptions = ["USER", "ADMIN"];
const logStatusOptions = ["WAITING", "PENDING", "SUCCESS", "FAILED"];
const messageStatusOptions = [
  "PENDING",
  "EXECUTING",
  "WAITING",
  "SUCCESS",
  "FAILED",
  "PARSE_FAILED",
  "EXHAUSTED",
  "DEAD",
];

const eventTab = (sourceType?: string) =>
  sourceType === "ADMIN" ? "admin-events" : "user-events";

const sourceEventLabel = (
  record: Pick<
    EventHookMessageRecord,
    "sourceEventName" | "sourceEventId" | "sourceType" | "resourcePath"
  >,
) => {
  const eventName = record.sourceEventName || record.sourceEventId || "-";

  if (record.sourceType === "ADMIN" && record.resourcePath) {
    return `${record.resourcePath} - ${eventName}`;
  }

  return eventName;
};

const SourceEventLink = ({
  realm,
  record,
}: {
  realm: string;
  record: Pick<
    EventHookMessageRecord,
    | "id"
    | "sourceEventName"
    | "sourceEventId"
    | "sourceType"
    | "resourcePath"
    | "test"
  >;
}) => {
  const { t } = useTranslation();
  const label = sourceEventLabel(record) || record.id || "-";

  if (!record.sourceType) {
    return label;
  }

  return record.test ? (
    <Flex
      spaceItems={{ default: "spaceItemsSm" }}
      alignItems={{ default: "alignItemsCenter" }}
    >
      <FlexItem>
        <Link to={toEvents({ realm, tab: eventTab(record.sourceType) })}>
          {label}
        </Link>
      </FlexItem>
      <FlexItem>
        <Chip isReadOnly>{t("eventHookTestLog")}</Chip>
      </FlexItem>
    </Flex>
  ) : (
    <Link to={toEvents({ realm, tab: eventTab(record.sourceType) })}>
      {label}
    </Link>
  );
};

const BatchLink = ({
  realm,
  executionId,
}: {
  realm: string;
  executionId: string;
}) => {
  const { t } = useTranslation();
  return (
    <Link to={toEventHookLogs({ realm, executionId })}>
      {t("eventHookBatchExecutionLink")}
    </Link>
  );
};

const MessageStatusLabel = ({ status }: { status?: string }) => {
  const { t } = useTranslation();

  if (!status) {
    return null;
  }

  switch (status) {
    case "WAITING":
    case "PENDING":
    case "EXECUTING":
      return (
        <Label color="orange" icon={<InfoCircleIcon />} isCompact>
          {t(status)}
        </Label>
      );
    case "SUCCESS":
      return (
        <Label color="green" icon={<CheckCircleIcon />} isCompact>
          {t(status)}
        </Label>
      );
    case "FAILED":
    case "PARSE_FAILED":
    case "EXHAUSTED":
    case "DEAD":
      return (
        <Label color="red" icon={<ExclamationCircleIcon />} isCompact>
          {t(status)}
        </Label>
      );
    default:
      return t(status);
  }
};

const EventHookExecutionDetails = ({
  realm,
  row,
  adminClient,
  refreshKey,
}: {
  realm: string;
  row: EventHookExecutionRow;
  adminClient: ReturnType<typeof useAdminClient>["adminClient"];
  refreshKey: number;
}) => {
  const { t } = useTranslation();
  const [logs, setLogs] = useState<EventHookLogRecord[]>([]);
  const [loadingLogs, setLoadingLogs] = useState(false);

  useEffect(() => {
    let active = true;

    if (!row.executionId) {
      setLogs([]);
      setLoadingLogs(false);
      return () => {
        active = false;
      };
    }

    setLoadingLogs(true);
    adminClient.eventHooks
      .findLogs({ realm, executionId: row.executionId })
      .then((loadedLogs) => {
        if (!active) {
          return;
        }

        setLogs(
          [...loadedLogs].sort(
            (a, b) => (b.createdAt || 0) - (a.createdAt || 0),
          ),
        );
        setLoadingLogs(false);
      })
      .catch(() => {
        if (!active) {
          return;
        }

        setLogs([]);
        setLoadingLogs(false);
      });

    return () => {
      active = false;
    };
  }, [adminClient, realm, row.executionId, refreshKey]);

  return (
    <>
      <TextContent className="pf-v5-u-mb-md">
        <Text component="h3">{t("eventHookMessageLogs")}</Text>
      </TextContent>
      <Table aria-label={t("eventHookMessageLogs")} variant="compact">
        <Thead>
          <Tr>
            <Th>{t("time")}</Th>
            <Th>{t("status")}</Th>
            <Th>{t("attempt")}</Th>
            <Th>{t("statusCode")}</Th>
            <Th>{t("durationMs")}</Th>
            <Th>{t("eventHookBatch")}</Th>
            <Th>{t("details")}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {logs.length > 0 &&
            logs.map((log) => (
              <Tr
                key={
                  log.id ||
                  `${log.executionId}-${log.createdAt}-${log.statusCode}`
                }
              >
                <Td>
                  {log.createdAt
                    ? new Date(log.createdAt).toLocaleString()
                    : ""}
                </Td>
                <Td>{log.status ? t(log.status) : ""}</Td>
                <Td>{log.attemptNumber ?? ""}</Td>
                <Td>{log.statusCode || ""}</Td>
                <Td>{log.durationMs ?? ""}</Td>
                <Td>
                  {row.message.executionBatch && log.executionId ? (
                    <BatchLink realm={realm} executionId={log.executionId} />
                  ) : (
                    "-"
                  )}
                </Td>
                <Td>{log.details || ""}</Td>
              </Tr>
            ))}
          {logs.length === 0 && (
            <Tr>
              <Td colSpan={7}>{loadingLogs ? <Spinner size="md" /> : "-"}</Td>
            </Tr>
          )}
        </Tbody>
      </Table>
    </>
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
  const [retryingExecutionIds, setRetryingExecutionIds] = useState<string[]>(
    [],
  );
  const [autoRefreshEnabled, setAutoRefreshEnabled] = useState(false);

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
  const executionId = searchParams.get("executionId") || "";
  const messageId = searchParams.get("messageId") || "";
  const defaultValues = useMemo<EventHookLogSearchForm>(
    () => ({
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
      executionId,
    }),
    [
      client,
      dateFrom,
      dateTo,
      event,
      executionId,
      ipAddress,
      messageStatus,
      resourcePath,
      resourceType,
      search,
      sourceType,
      status,
      targetId,
      targetType,
      user,
    ],
  );
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
    executionId: t("eventHookExecutionFilter"),
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
      pickBy(defaultValues, (value) =>
        Boolean(value.trim()),
      ) as Partial<EventHookLogSearchForm>,
    [defaultValues],
  );

  useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  useEffect(() => {
    void adminClient.eventHooks.findTargets({ realm }).then(setTargets);
  }, [adminClient, realm]);

  useEffect(() => {
    if (!autoRefreshEnabled) {
      return;
    }

    const interval = window.setInterval(() => {
      setRefreshCount((count) => count + 1);
    }, 5000);

    return () => window.clearInterval(interval);
  }, [autoRefreshEnabled]);

  const refresh = () => setRefreshCount((count) => count + 1);

  const isRetryableMessageStatus = (status?: string) =>
    status === "FAILED" ||
    status === "PARSE_FAILED" ||
    status === "EXHAUSTED" ||
    status === "DEAD";

  const withRetryingExecutions = async <T,>(
    executionIds: string[],
    action: () => Promise<T>,
  ) => {
    setRetryingExecutionIds((current) => [
      ...new Set([...current, ...executionIds]),
    ]);

    try {
      return await action();
    } finally {
      setRetryingExecutionIds((current) =>
        current.filter((executionId) => !executionIds.includes(executionId)),
      );
    }
  };

  const isRetryableExecutionRow = (row: EventHookExecutionRow) =>
    Boolean(row.executionId) && isRetryableMessageStatus(row.message.status);

  const retryLogs = async (row: EventHookExecutionRow) => {
    if (!isRetryableExecutionRow(row) || !row.executionId) {
      addAlert(t("eventHookRetryUnavailable"), AlertVariant.warning);
      return;
    }

    try {
      await withRetryingExecutions([row.executionId], () =>
        adminClient.eventHooks.retryExecution({
          realm,
          executionId: row.executionId!,
        }),
      );
      addAlert(t("eventHookRetryQueued", { count: 1 }), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("eventHookRetryError", error);
    }
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
    const nextFilters = pickBy(values, (value) =>
      Boolean(value.trim()),
    ) as Partial<EventHookLogSearchForm>;

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
      executionId: nextFilters.executionId || "",
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
      executionId: "",
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
      [
        ...new Set(
          targets
            .map((target) => target.type)
            .filter((value): value is string => Boolean(value)),
        ),
      ].sort((a, b) => a.localeCompare(b)),
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
                      <SelectOption value="">
                        {t("allEventHookSourceTypes")}
                      </SelectOption>
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
                      <SelectOption value="">
                        {t("allEventHookTargetTypes")}
                      </SelectOption>
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
                        visibleTargets.find(
                          (target) => target.id === field.value,
                        )?.name ||
                        field.value ||
                        t("allEventHookTargets")
                      }
                      variant="single"
                    >
                      <SelectOption value="">
                        {t("allEventHookTargets")}
                      </SelectOption>
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
                      <SelectOption value="">
                        {t("allEventHookStatuses")}
                      </SelectOption>
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
                      selections={
                        field.value || t("allEventHookMessageStatuses")
                      }
                      variant="single"
                    >
                      <SelectOption value="">
                        {t("allEventHookMessageStatuses")}
                      </SelectOption>
                      {messageStatusOptions.map((option) => (
                        <SelectOption key={option} value={option}>
                          {option}
                        </SelectOption>
                      ))}
                    </KeycloakSelect>
                  )}
                />
              </FormGroup>
              <FormGroup
                label={t("dateFrom")}
                fieldId="event-hook-log-date-from"
              >
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
                  categoryName={
                    filterLabels[key as keyof EventHookLogSearchForm]
                  }
                  onClick={() =>
                    removeFilter(key as keyof EventHookLogSearchForm)
                  }
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

  const loader = async (): Promise<EventHookExecutionRow[]> => {
    const [messages, loadedTargets] = await Promise.all([
      adminClient.eventHooks.findMessages({
        realm,
        status: messageStatus || undefined,
        sourceType: sourceType || undefined,
        targetId: targetId || undefined,
        targetType: targetType || undefined,
        event: event || undefined,
        client: client || undefined,
        user: user || undefined,
        ipAddress: ipAddress || undefined,
        resourceType: resourceType || undefined,
        resourcePath: resourcePath || undefined,
        executionId: executionId || undefined,
        search: search || undefined,
      }),
      adminClient.eventHooks.findTargets({ realm }),
    ]);

    const filteredExecutionIds =
      status || dateFrom || dateTo || messageId
        ? new Set(
            (
              await adminClient.eventHooks.findLogs({
                realm,
                messageId: messageId || undefined,
                targetId: targetId || undefined,
                targetType: targetType || undefined,
                sourceType: sourceType || undefined,
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
                executionId: executionId || undefined,
                search: search || undefined,
              })
            )
              .map((log) => log.executionId)
              .filter((value): value is string => Boolean(value)),
          )
        : undefined;

    const targetNames = new Map(
      loadedTargets.map((target: EventHookTargetRepresentation) => [
        target.id,
        target.name,
      ]),
    );

    return messages
      .filter((message) => {
        if (messageId && message.id !== messageId) {
          return false;
        }

        if (
          filteredExecutionIds &&
          (!message.executionId ||
            !filteredExecutionIds.has(message.executionId))
        ) {
          return false;
        }

        return true;
      })
      .map((message) => ({
        executionId: message.executionId,
        targetName: targetNames.get(message.targetId) || message.targetId,
        message: {
          ...message,
          targetName: targetNames.get(message.targetId) || message.targetId,
        },
      }))
      .sort((a, b) => (b.message.createdAt || 0) - (a.message.createdAt || 0));
  };

  return (
    <KeycloakDataTable
      loader={loader}
      refreshKey={refreshCount}
      onRefresh={refresh}
      ariaLabelKey="eventHookMessages"
      detailColumns={[
        {
          name: "logs",
          enabled: (row) => Boolean((row as EventHookExecutionRow).executionId),
          cellRenderer: (row) => (
            <EventHookExecutionDetails
              realm={realm}
              row={row as EventHookExecutionRow}
              adminClient={adminClient}
              refreshKey={refreshCount}
            />
          ),
        },
      ]}
      isSearching={Object.keys(activeFilters).length > 0}
      toolbarItem={
        <>
          {searchFormDisplay()}
          <Switch
            id="event-hook-logs-auto-refresh"
            label="Automatically update (5s)"
            labelOff="Automatically update (5s)"
            isChecked={autoRefreshEnabled}
            onChange={(_, checked) => setAutoRefreshEnabled(checked)}
          />
        </>
      }
      columns={[
        {
          name: "createdAt",
          displayKey: "time",
          cellRenderer: (row: EventHookExecutionRow) =>
            row.message.createdAt
              ? formatDate(new Date(row.message.createdAt))
              : "",
        },
        {
          name: "targetName",
          displayKey: "target",
        },
        {
          name: "sourceEventName",
          displayKey: "eventHookEvent",
          cellRenderer: (row: EventHookExecutionRow) => (
            <SourceEventLink realm={realm} record={row.message} />
          ),
        },
        {
          name: "userId",
          displayKey: "userId",
          cellRenderer: (row: EventHookExecutionRow) =>
            row.message.userId || "",
        },
        {
          name: "resourcePath",
          displayKey: "resourcePath",
          cellRenderer: (row: EventHookExecutionRow) =>
            row.message.resourcePath || "",
        },
        {
          name: "messageStatus",
          displayKey: "eventHookMessageStatusFilter",
          cellRenderer: (row: EventHookExecutionRow) => (
            <MessageStatusLabel status={row.message.status} />
          ),
        },
        {
          name: "executionId",
          displayKey: "eventHookExecutionFilter",
          cellRenderer: (row: EventHookExecutionRow) => row.executionId || "-",
        },
        {
          name: "executionBatch",
          displayKey: "eventHookBatch",
          cellRenderer: (row: EventHookExecutionRow) =>
            row.executionId && row.message.executionBatch ? (
              <BatchLink realm={realm} executionId={row.executionId} />
            ) : (
              "-"
            ),
        },
      ]}
      actionResolver={({ data }) => {
        const row = data as EventHookExecutionRow;
        if (
          !isRetryableExecutionRow(row) ||
          (row.executionId && retryingExecutionIds.includes(row.executionId))
        ) {
          return [];
        }

        return [
          {
            title: t("eventHookRetryAction"),
            onClick: () => retryLogs(row),
          } as Action<EventHookExecutionRow>,
        ];
      }}
      emptyState={
        <ListEmptyState
          message={t("emptyEventHookMessages")}
          instructions={t("emptyEventHookMessagesInstructions")}
        />
      }
    />
  );
};
