import { fetchWithError } from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  ListEmptyState,
  NumberControl,
  PasswordInput,
  ScrollForm,
  SelectControl,
  SelectVariant,
  TextAreaControl,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import type { SsfClientTab } from "../routes/ClientSsfTab";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Chip,
  FormGroup,
  InputGroup,
  InputGroupItem,
  Label,
  PageSection,
  SelectOption,
  Split,
  SplitItem,
  Text,
  TextContent,
  TextInput,
} from "@patternfly/react-core";
import {
  CheckCircleIcon,
  InfoCircleIcon,
  MinusCircleIcon,
  PauseCircleIcon,
  SyncAltIcon,
  TimesCircleIcon,
} from "@patternfly/react-icons";
import { CopyToClipboardButton } from "../../components/copy-to-clipboard-button/CopyToClipboardButton";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import {
  AddRoleButton,
  AddRoleMappingModal,
  FilterType,
} from "../../components/role-mapping/AddRoleMappingModal";
import { ServiceRole } from "../../components/role-mapping/RoleMapping";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import { addTrailingSlash, convertAttributeNameToForm } from "../../util";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import useFormatDate from "../../utils/useFormatDate";
import type { FormFields, SaveOptions } from "../ClientDetails";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { EventsTab } from "./tabs/EventsTab";
import { SubjectsTab } from "./tabs/SubjectsTab";

const FALLBACK_DEFAULT_SUPPORTED_EVENTS =
  "CaepCredentialChange,CaepSessionRevoked";

// SSF spec delivery method URIs (RFC 8935 / RFC 8936). Kept as
// constants so the admin UI doesn't litter the literal strings across
// every comparison and stream-create body builder.
const DELIVERY_METHOD_PUSH_URI = "urn:ietf:rfc:8935";
const DELIVERY_METHOD_POLL_URI = "urn:ietf:rfc:8936";

const isPollDeliveryMethod = (method: string | undefined): boolean =>
  method === DELIVERY_METHOD_POLL_URI ||
  method === "https://schemas.openid.net/secevent/risc/delivery-method/poll";

const noop = () => {
  // no-op handler for the read-only Events Delivered KeycloakSelect
};

const splitSupportedEvents = (value: unknown): string[] => {
  if (!value || typeof value !== "string") {
    return [];
  }
  return value
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
};

type SsfConfig = {
  defaultSupportedEvents?: string[];
  availableSupportedEvents?: string[];
  nativelyEmittedEvents?: string[];
  defaultPushEndpointConnectTimeoutMillis?: number;
  defaultPushEndpointSocketTimeoutMillis?: number;
  defaultUserSubjectFormat?: string;
};

const FALLBACK_DEFAULT_PUSH_CONNECT_TIMEOUT_MILLIS = 1000;
const FALLBACK_DEFAULT_PUSH_SOCKET_TIMEOUT_MILLIS = 1000;
const FALLBACK_DEFAULT_USER_SUBJECT_FORMAT = "iss_sub";

type SsfClientStreamDelivery = {
  method?: string;
  endpoint_url?: string;
  authorization_header?: string;
};

type SsfClientStream = {
  streamId?: string;
  description?: string;
  status?: string;
  statusReason?: string;
  audience?: string[];
  delivery?: SsfClientStreamDelivery;
  eventsSupported?: string[];
  eventsRequested?: string[];
  eventsDelivered?: string[];
  createdAt?: number;
  updatedAt?: number;
  lastVerifiedAt?: number;
};

export type SsfTabProps = {
  save: (options?: SaveOptions) => void;
  client: ClientRepresentation;
  /**
   * Which sub-tab to render. The parent (ClientDetails) drives this
   * from the URL via RoutableTabs so each section is a deep-linkable
   * page on its own.
   */
  activeTab: SsfClientTab;
};

export const SsfTab = ({ save, client, activeTab }: SsfTabProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const formatDate = useFormatDate();

  const { control, watch, setValue } = useFormContext<FormFields>();

  const [defaultSupportedEvents, setDefaultSupportedEvents] = useState<string>(
    FALLBACK_DEFAULT_SUPPORTED_EVENTS,
  );
  const [availableSupportedEvents, setAvailableSupportedEvents] = useState<
    string[]
  >([]);
  const [nativelyEmittedEvents, setNativelyEmittedEvents] = useState<string[]>(
    [],
  );
  const [defaultPushConnectTimeoutMillis, setDefaultPushConnectTimeoutMillis] =
    useState<number>(FALLBACK_DEFAULT_PUSH_CONNECT_TIMEOUT_MILLIS);
  const [defaultPushSocketTimeoutMillis, setDefaultPushSocketTimeoutMillis] =
    useState<number>(FALLBACK_DEFAULT_PUSH_SOCKET_TIMEOUT_MILLIS);
  const [defaultUserSubjectFormat, setDefaultUserSubjectFormat] =
    useState<string>(FALLBACK_DEFAULT_USER_SUBJECT_FORMAT);
  const [clientStream, setClientStream] = useState<SsfClientStream | null>(
    null,
  );
  const [supportedEventsOpen, setSupportedEventsOpen] = useState(false);
  const [emitOnlyEventsOpen, setEmitOnlyEventsOpen] = useState(false);
  const [streamFetchKey, setStreamFetchKey] = useState(0);
  const [configFetchKey, setConfigFetchKey] = useState(0);

  // Admin-side create-stream form state — used by the empty-state form
  // when no stream is registered yet. Kept as plain useState because the
  // surrounding react-hook-form context is bound to the client
  // representation's attributes, not to stream-create parameters.
  const [createStreamAudience, setCreateStreamAudience] = useState("");
  const [createStreamMethod, setCreateStreamMethod] = useState<"PUSH" | "POLL">(
    "PUSH",
  );
  const [createStreamEndpointUrl, setCreateStreamEndpointUrl] = useState("");
  const [createStreamAuthHeader, setCreateStreamAuthHeader] = useState("");
  const [createStreamEvents, setCreateStreamEvents] = useState<string[]>([]);
  const [createStreamDescription, setCreateStreamDescription] = useState("");
  const [createStreamEventsOpen, setCreateStreamEventsOpen] = useState(false);
  const [createStreamSubmitting, setCreateStreamSubmitting] = useState(false);
  const [createStreamFormOpen, setCreateStreamFormOpen] = useState(false);

  // --- SSF Required Role picker state ---
  const [rolePickerOpen, setRolePickerOpen] = useState(false);
  const [roleFilterType, setRoleFilterType] = useState<FilterType>("clients");

  // --- SSF Emit Events Role picker state ---
  // Independent modal/filter state so both pickers can coexist in the
  // same form without clobbering each other's open state.
  const [emitRolePickerOpen, setEmitRolePickerOpen] = useState(false);
  const [emitRoleFilterType, setEmitRoleFilterType] =
    useState<FilterType>("clients");

  const requiredRoleFieldName = convertAttributeNameToForm<FormFields>(
    "attributes.ssf.requiredRole",
  );

  const emitEventsRoleFieldName = convertAttributeNameToForm<FormFields>(
    "attributes.ssf.emitEventsRole",
  );

  const parseRoleValue = (value: string | undefined) => {
    if (!value?.includes(".")) return ["", value || ""];
    return value.split(".");
  };

  const refresh = () => {
    setStreamFetchKey((k) => k + 1);
    setConfigFetchKey((k) => k + 1);
  };

  const triggerVerifyStream = async () => {
    if (!client.id) {
      return;
    }
    try {
      const response = await fetch(
        `${addTrailingSlash(
          adminClient.baseUrl,
        )}admin/realms/${realm}/ssf/clients/${client.clientId}/stream/verify`,
        {
          method: "POST",
          headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
        },
      );
      if (!response.ok) {
        throw new Error(
          `${response.status} ${response.statusText || "Request failed"}`,
        );
      }
      addAlert(t("ssfVerifyStreamSuccess"), AlertVariant.success);
      // Refetch the stream endpoint so the Last verified field in the UI
      // picks up the new timestamp the backend just stamped.
      refresh();
    } catch (error) {
      addError("ssfVerifyStreamError", error);
    }
  };

  const resetCreateStreamForm = () => {
    setCreateStreamAudience("");
    setCreateStreamEndpointUrl("");
    setCreateStreamAuthHeader("");
    setCreateStreamEvents([]);
    setCreateStreamDescription("");
    setCreateStreamMethod("PUSH");
  };

  const submitCreateStream = async () => {
    if (!client.id) {
      return;
    }
    // PUSH requires the receiver to supply its own endpoint URL; POLL
    // doesn't (the transmitter generates the URL itself per SSF §6.1.2
    // and writes it back on the response).
    if (createStreamMethod === "PUSH" && !createStreamEndpointUrl.trim()) {
      addError(
        "ssfCreateStreamError",
        new Error(t("ssfCreateStreamEndpointUrlRequired")),
      );
      return;
    }
    setCreateStreamSubmitting(true);
    try {
      // Event aliases stored in local state are resolved back to their
      // canonical URIs by the transmitter at create time — the admin UI
      // shows/stores aliases because that's what the admin picks from
      // availableSupportedEvents, and the backend accepts either form.
      const delivery: Record<string, unknown> = {
        method:
          createStreamMethod === "POLL"
            ? DELIVERY_METHOD_POLL_URI
            : DELIVERY_METHOD_PUSH_URI,
      };
      if (createStreamMethod === "PUSH") {
        delivery.endpoint_url = createStreamEndpointUrl.trim();
        if (createStreamAuthHeader.trim()) {
          delivery.authorization_header = createStreamAuthHeader.trim();
        }
      }
      const body: Record<string, unknown> = { delivery };
      if (createStreamAudience.trim()) {
        body.aud = [createStreamAudience.trim()];
      }
      if (createStreamEvents.length > 0) {
        body.events_requested = createStreamEvents;
      }
      if (createStreamDescription.trim()) {
        body.description = createStreamDescription.trim();
      }

      const response = await fetch(
        `${addTrailingSlash(
          adminClient.baseUrl,
        )}admin/realms/${realm}/ssf/clients/${client.clientId}/stream`,
        {
          method: "POST",
          headers: {
            ...getAuthorizationHeaders(await adminClient.getAccessToken()),
            "Content-Type": "application/json",
          },
          body: JSON.stringify(body),
        },
      );
      if (!response.ok) {
        const text = await response.text();
        throw new Error(
          text ||
            `${response.status} ${response.statusText || "Request failed"}`,
        );
      }
      addAlert(t("ssfCreateStreamSuccess"), AlertVariant.success);
      resetCreateStreamForm();
      setCreateStreamFormOpen(false);
      refresh();
    } catch (error) {
      addError("ssfCreateStreamError", error);
    } finally {
      setCreateStreamSubmitting(false);
    }
  };

  const [toggleDeleteStreamDialog, DeleteStreamConfirm] = useConfirmDialog({
    titleKey: "ssfDeleteStreamConfirmTitle",
    messageKey: "ssfDeleteStreamConfirmMessage",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      if (!client.id) {
        return;
      }
      try {
        const response = await fetch(
          `${addTrailingSlash(
            adminClient.baseUrl,
          )}admin/realms/${realm}/ssf/clients/${client.clientId}/stream`,
          {
            method: "DELETE",
            headers: getAuthorizationHeaders(
              await adminClient.getAccessToken(),
            ),
          },
        );
        if (!response.ok) {
          throw new Error(
            `${response.status} ${response.statusText || "Request failed"}`,
          );
        }
        addAlert(t("ssfDeleteStreamSuccess"), AlertVariant.success);
        setClientStream(null);
        refresh();
      } catch (error) {
        addError("ssfDeleteStreamError", error);
      }
    },
  });

  useFetch(
    async () => {
      const response = await fetchWithError(
        `${addTrailingSlash(
          adminClient.baseUrl,
        )}admin/realms/${realm}/ssf/config`,
        {
          headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
        },
      );
      if (!response.ok) {
        return null;
      }
      return (await response.json()) as SsfConfig;
    },
    (config) => {
      if (config?.availableSupportedEvents) {
        setAvailableSupportedEvents(config.availableSupportedEvents);
      }

      if (config?.nativelyEmittedEvents) {
        setNativelyEmittedEvents(config.nativelyEmittedEvents);
      }

      if (typeof config?.defaultPushEndpointConnectTimeoutMillis === "number") {
        setDefaultPushConnectTimeoutMillis(
          config.defaultPushEndpointConnectTimeoutMillis,
        );
        const connectField = convertAttributeNameToForm<FormFields>(
          "attributes.ssf.pushEndpointConnectTimeoutMillis",
        );
        const currentConnect = watch(connectField);
        if (currentConnect === undefined || currentConnect === "") {
          setValue(
            connectField,
            String(config.defaultPushEndpointConnectTimeoutMillis),
            { shouldDirty: false },
          );
        }
      }

      if (typeof config?.defaultPushEndpointSocketTimeoutMillis === "number") {
        setDefaultPushSocketTimeoutMillis(
          config.defaultPushEndpointSocketTimeoutMillis,
        );
        const socketField = convertAttributeNameToForm<FormFields>(
          "attributes.ssf.pushEndpointSocketTimeoutMillis",
        );
        const currentSocket = watch(socketField);
        if (currentSocket === undefined || currentSocket === "") {
          setValue(
            socketField,
            String(config.defaultPushEndpointSocketTimeoutMillis),
            { shouldDirty: false },
          );
        }
      }

      if (
        typeof config?.defaultUserSubjectFormat === "string" &&
        config.defaultUserSubjectFormat.length > 0
      ) {
        setDefaultUserSubjectFormat(config.defaultUserSubjectFormat);
        const userSubjectFormatField = convertAttributeNameToForm<FormFields>(
          "attributes.ssf.userSubjectFormat",
        );
        const currentUserSubjectFormat = watch(userSubjectFormatField);
        if (
          currentUserSubjectFormat === undefined ||
          currentUserSubjectFormat === ""
        ) {
          setValue(userSubjectFormatField, config.defaultUserSubjectFormat, {
            shouldDirty: false,
          });
        }
      }

      const events = config?.defaultSupportedEvents;
      if (!events || events.length === 0) {
        return;
      }
      const joined = events.join(",");
      setDefaultSupportedEvents(joined);

      const fieldName = convertAttributeNameToForm<FormFields>(
        "attributes.ssf.supportedEvents",
      );
      const currentValue = watch(fieldName);
      if (currentValue === undefined || currentValue === "") {
        setValue(fieldName, joined, { shouldDirty: false });
      }
    },
    [configFetchKey],
  );

  useFetch(
    async () => {
      if (!client.id) {
        return null;
      }
      // Use plain fetch instead of fetchWithError so we can handle the
      // expected 404 (no stream registered yet) without surfacing it as a
      // global error and redirecting away from the client details page.
      const response = await fetch(
        `${addTrailingSlash(
          adminClient.baseUrl,
        )}admin/realms/${realm}/ssf/clients/${client.clientId}/stream`,
        {
          headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
        },
      );
      if (!response.ok) {
        return null;
      }
      return (await response.json()) as SsfClientStream;
    },
    (stream) => {
      setClientStream(stream);
      if (stream?.status) {
        // Re-seed the form-bound status field so clicking "Refresh"
        // picks up receiver-driven status changes (e.g. the receiver
        // paused the stream via POST /streams/status) instead of
        // showing the stale value the page loaded with.
        setValue(
          convertAttributeNameToForm<FormFields>("attributes.ssf.status"),
          stream.status,
          { shouldDirty: false },
        );
      }
      // Re-seed the form-bound delivery selector from the actual
      // stream's delivery.method so the dropdown matches reality. The
      // attribute defaults to "PUSH" for new clients and only gets
      // overwritten on save — without this, opening a client whose
      // stream was created as POLL still shows "Push" in the picker.
      if (stream?.delivery?.method) {
        setValue(
          convertAttributeNameToForm<FormFields>("attributes.ssf.delivery"),
          isPollDeliveryMethod(stream.delivery.method) ? "POLL" : "PUSH",
          { shouldDirty: false },
        );
      }
    },
    [client.id, streamFetchKey],
  );

  const ssfDelivery = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.delivery"),
  );
  // DefaultSwitchControl with stringify persists "true" / "false" —
  // compare as string so the delay-millis input toggles in sync with
  // the switch rather than interpreting the raw boolean.
  const ssfAutoVerifyStream = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.autoVerifyStream"),
  );
  // DefaultSwitchControl with stringify persists "true" / "false" —
  // compare as string so the role picker toggles in sync with the
  // switch rather than interpreting the raw boolean.
  const ssfAllowEmitEvents = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.allowEmitEvents"),
  );

  // Drive the emit-only multi-select options off the live value of
  // supportedEvents so adding / removing a supported event immediately
  // adjusts what the operator can mark emit-only. No standalone
  // registry list — the emit-only set is a strict subset.
  const ssfSupportedEvents = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.supportedEvents"),
  );

  const resetFields = (names: string[]) => {
    for (const name of names) {
      setValue(
        convertAttributeNameToForm<FormFields>(`attributes.${name}`),
        client.attributes?.[name] || "",
      );
    }
  };

  const reset = () =>
    resetFields([
      "ssf.description",
      "ssf.streamAudience",
      "ssf.supportedEvents",
      "ssf.emitOnlyEvents",
      "ssf.profile",
      "ssf.userSubjectFormat",
      "ssf.defaultSubjects",
      "ssf.autoNotifyOnLogin",
      "ssf.requireServiceAccount",
      "ssf.requiredRole",
      "ssf.allowEmitEvents",
      "ssf.emitEventsRole",
      "ssf.minVerificationInterval",
      "ssf.autoVerifyStream",
      "ssf.verificationDelayMillis",
      "ssf.status",
      "ssf.delivery",
      "ssf.pushEndpointConnectTimeoutMillis",
      "ssf.pushEndpointSocketTimeoutMillis",
      "ssf.maxEventAgeSeconds",
      "ssf.inactivityTimeoutSeconds",
      "ssf.subjectRemovalGraceSeconds",
    ]);

  const saveActionGroup = (testIdPrefix: string) => (
    <ActionGroup>
      <Button
        variant="secondary"
        onClick={() => save()}
        data-testid={`${testIdPrefix}Save`}
      >
        {t("save")}
      </Button>
      <Button
        variant="link"
        onClick={reset}
        data-testid={`${testIdPrefix}Revert`}
      >
        {t("revert")}
      </Button>
    </ActionGroup>
  );

  return (
    <PageSection variant="light" className="pf-v5-u-py-0">
      <DeleteStreamConfirm />
      {activeTab === "receiver" && (
        <Card isFlat className="pf-v5-u-mt-md">
          <CardHeader>
            <CardTitle>{t("ssfReceiver")}</CardTitle>
          </CardHeader>
          <CardBody>
            <TextContent>
              <Text>{t("ssfReceiverHelp")}</Text>
            </TextContent>
          </CardBody>
          <CardBody>
            <FormAccess
              role="manage-clients"
              fineGrainedAccess={client.access?.configure}
              isHorizontal
            >
              <ScrollForm
                label={t("jumpToSection")}
                sections={[
                  {
                    title: t("ssfSectionGeneral"),
                    panel: (
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          rowGap: "1.5rem",
                        }}
                      >
                        <Text className="pf-v5-u-pb-lg">
                          {t("ssfSectionGeneralHelp")}
                        </Text>
                        <FormGroup
                          label={t("ssfStreamStatusLabel")}
                          fieldId="ssfStreamStatusIndicator"
                        >
                          {!clientStream && (
                            <Label
                              color="grey"
                              icon={<MinusCircleIcon />}
                              data-testid="ssfStreamIndicator.unregistered"
                            >
                              {t("ssfStreamIndicator.unregistered")}
                            </Label>
                          )}
                          {clientStream?.status === "enabled" && (
                            <Label
                              color="green"
                              icon={<CheckCircleIcon />}
                              data-testid="ssfStreamIndicator.enabled"
                            >
                              {t("ssfStreamIndicator.enabled")}
                            </Label>
                          )}
                          {clientStream?.status === "paused" && (
                            <Label
                              color="orange"
                              icon={<PauseCircleIcon />}
                              data-testid="ssfStreamIndicator.paused"
                            >
                              {t("ssfStreamIndicator.paused")}
                            </Label>
                          )}
                          {clientStream?.status === "disabled" && (
                            <Label
                              color="red"
                              icon={<TimesCircleIcon />}
                              data-testid="ssfStreamIndicator.disabled"
                            >
                              {t("ssfStreamIndicator.disabled")}
                            </Label>
                          )}
                          {clientStream && !clientStream.status && (
                            <Label
                              color="blue"
                              icon={<InfoCircleIcon />}
                              data-testid="ssfStreamIndicator.registered"
                            >
                              {t("ssfStreamIndicator.registered")}
                            </Label>
                          )}
                        </FormGroup>
                        <SelectControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.profile",
                          )}
                          label={t("ssfProfile")}
                          labelIcon={t("ssfProfileHelp")}
                          controller={{
                            defaultValue: "SSF_1_0",
                          }}
                          options={[
                            { key: "SSF_1_0", value: t("ssfProfile.SSF_1_0") },
                            {
                              key: "SSE_CAEP",
                              value: t("ssfProfile.SSE_CAEP"),
                            },
                          ]}
                        />
                        <TextAreaControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.description",
                          )}
                          label={t("ssfDescription")}
                          labelIcon={t("ssfDescriptionHelp")}
                          rules={{
                            maxLength: {
                              value: 255,
                              message: t("maxLength", { length: 255 }),
                            },
                          }}
                        />
                        <TextControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.streamAudience",
                          )}
                          label={t("ssfStreamAudience")}
                          labelIcon={t("ssfStreamAudienceHelp")}
                        />
                        <SelectControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.defaultSubjects",
                          )}
                          label={t("ssfDefaultSubjects")}
                          labelIcon={t("ssfDefaultSubjectsHelp")}
                          controller={{
                            defaultValue: "NONE",
                          }}
                          options={[
                            {
                              key: "ALL",
                              value: t("ssfDefaultSubjects.ALL"),
                            },
                            {
                              key: "NONE",
                              value: t("ssfDefaultSubjects.NONE"),
                            },
                          ]}
                        />
                        <SelectControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.userSubjectFormat",
                          )}
                          label={t("ssfUserSubjectFormat")}
                          labelIcon={t("ssfUserSubjectFormatHelp")}
                          controller={{
                            defaultValue: defaultUserSubjectFormat,
                          }}
                          options={[
                            {
                              key: "iss_sub",
                              value: t("ssfUserSubjectFormat.iss_sub"),
                            },
                            {
                              key: "email",
                              value: t("ssfUserSubjectFormat.email"),
                            },
                            {
                              key: "complex.iss_sub+tenant",
                              value: t(
                                "ssfUserSubjectFormat.complex.iss_sub+tenant",
                              ),
                            },
                            {
                              key: "complex.email+tenant",
                              value: t(
                                "ssfUserSubjectFormat.complex.email+tenant",
                              ),
                            },
                          ]}
                        />
                        <DefaultSwitchControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.autoNotifyOnLogin",
                          )}
                          label={t("ssfAutoNotifyOnLogin")}
                          labelIcon={t("ssfAutoNotifyOnLoginHelp")}
                          stringify
                        />
                        <FormGroup
                          label={t("ssfSubjectRemovalGrace")}
                          fieldId="ssfSubjectRemovalGrace"
                          labelIcon={
                            <HelpItem
                              helpText={t("ssfSubjectRemovalGraceHelp")}
                              fieldLabelId="ssfSubjectRemovalGrace"
                            />
                          }
                        >
                          <Controller
                            name={convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.subjectRemovalGraceSeconds",
                            )}
                            defaultValue=""
                            control={control}
                            render={({ field }) => (
                              <TimeSelector
                                data-testid="ssfSubjectRemovalGrace"
                                value={field.value!}
                                onChange={field.onChange}
                                units={["second", "minute", "hour", "day"]}
                              />
                            )}
                          />
                        </FormGroup>
                        <DefaultSwitchControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.autoVerifyStream",
                          )}
                          label={t("ssfAutoVerifyStream")}
                          labelIcon={t("ssfAutoVerifyStreamHelp")}
                          stringify
                        />
                        {ssfAutoVerifyStream === "true" && (
                          <NumberControl
                            name={convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.verificationDelayMillis",
                            )}
                            label={t("ssfVerificationDelay")}
                            labelIcon={t("ssfVerificationDelayHelp")}
                            controller={{
                              defaultValue: 1500,
                              rules: {
                                min: 0,
                              },
                            }}
                          />
                        )}
                        <NumberControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.minVerificationInterval",
                          )}
                          label={t("ssfMinVerificationInterval")}
                          labelIcon={t("ssfMinVerificationIntervalHelp")}
                          controller={{
                            defaultValue: "",
                            rules: {
                              min: 0,
                            },
                          }}
                        />
                        <FormGroup
                          label={t("ssfMaxEventAge")}
                          fieldId="ssfMaxEventAge"
                          labelIcon={
                            <HelpItem
                              helpText={t("ssfMaxEventAgeHelp")}
                              fieldLabelId="ssfMaxEventAge"
                            />
                          }
                        >
                          <Controller
                            name={convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.maxEventAgeSeconds",
                            )}
                            defaultValue=""
                            control={control}
                            render={({ field }) => (
                              <TimeSelector
                                data-testid="ssfMaxEventAge"
                                value={field.value!}
                                onChange={field.onChange}
                                units={["second", "minute", "hour", "day"]}
                              />
                            )}
                          />
                        </FormGroup>
                        <FormGroup
                          label={t("ssfInactivityTimeout")}
                          fieldId="ssfInactivityTimeout"
                          labelIcon={
                            <HelpItem
                              helpText={t("ssfInactivityTimeoutHelp")}
                              fieldLabelId="ssfInactivityTimeout"
                            />
                          }
                        >
                          <Controller
                            name={convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.inactivityTimeoutSeconds",
                            )}
                            defaultValue=""
                            control={control}
                            render={({ field }) => (
                              <TimeSelector
                                data-testid="ssfInactivityTimeout"
                                value={field.value!}
                                onChange={field.onChange}
                                units={["minute", "hour", "day"]}
                              />
                            )}
                          />
                        </FormGroup>
                      </div>
                    ),
                  },
                  {
                    title: t("ssfSectionAuthentication"),
                    panel: (
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          rowGap: "1.5rem",
                        }}
                      >
                        <Text className="pf-v5-u-pb-lg">
                          {t("ssfSectionAuthenticationHelp")}
                        </Text>
                        <DefaultSwitchControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.requireServiceAccount",
                          )}
                          label={t("ssfRequireServiceAccount")}
                          labelIcon={t("ssfRequireServiceAccountHelp")}
                          stringify
                        />
                        <FormGroup
                          label={t("ssfRequiredRole")}
                          fieldId="ssfRequiredRole"
                          labelIcon={
                            <HelpItem
                              helpText={t("ssfRequiredRoleHelp")}
                              fieldLabelId="ssfRequiredRole"
                            />
                          }
                        >
                          <Controller
                            name={requiredRoleFieldName}
                            defaultValue=""
                            control={control}
                            render={({ field }) => (
                              <Split>
                                {rolePickerOpen && (
                                  <AddRoleMappingModal
                                    id="ssfRequiredRolePicker"
                                    type="roles"
                                    filterType={roleFilterType}
                                    name="ssfRequiredRole"
                                    onAssign={(rows) => {
                                      const row = rows[0];
                                      const value = row.client?.clientId
                                        ? `${row.client.clientId}.${row.role.name}`
                                        : row.role.name;
                                      field.onChange(value);
                                      setRolePickerOpen(false);
                                    }}
                                    onClose={() => setRolePickerOpen(false)}
                                    isRadio
                                  />
                                )}
                                {field.value && field.value !== "" && (
                                  <SplitItem>
                                    <Chip
                                      textMaxWidth="500px"
                                      onClick={() => field.onChange("")}
                                    >
                                      <ServiceRole
                                        role={{
                                          name: parseRoleValue(field.value)[1],
                                        }}
                                        client={{
                                          clientId: parseRoleValue(
                                            field.value,
                                          )[0],
                                        }}
                                      />
                                    </Chip>
                                  </SplitItem>
                                )}
                                <SplitItem>
                                  <AddRoleButton
                                    label="selectRole.label"
                                    onFilerTypeChange={(type) => {
                                      setRoleFilterType(type);
                                      setRolePickerOpen(true);
                                    }}
                                    variant="secondary"
                                    data-testid="ssfRequiredRoleSelect"
                                    isDisabled={false}
                                  />
                                </SplitItem>
                              </Split>
                            )}
                          />
                        </FormGroup>
                      </div>
                    ),
                  },
                  {
                    title: t("ssfSectionEvents"),
                    panel: (
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          rowGap: "1.5rem",
                        }}
                      >
                        <Text className="pf-v5-u-pb-lg">
                          {t("ssfSectionEventsHelp")}
                        </Text>
                        <FormGroup
                          label={t("ssfSupportedEvents")}
                          fieldId="ssfSupportedEvents"
                          labelIcon={
                            <HelpItem
                              helpText={t("ssfSupportedEventsHelp")}
                              fieldLabelId="ssfSupportedEvents"
                            />
                          }
                        >
                          <Controller
                            name={convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.supportedEvents",
                            )}
                            control={control}
                            defaultValue={defaultSupportedEvents}
                            render={({ field }) => {
                              const selected = splitSupportedEvents(
                                field.value,
                              );
                              return (
                                <KeycloakSelect
                                  toggleId="ssfSupportedEvents"
                                  data-testid="ssfSupportedEvents"
                                  variant={SelectVariant.typeaheadMulti}
                                  chipGroupProps={{
                                    numChips: 5,
                                    expandedText: t("hide"),
                                    collapsedText: t("showRemaining"),
                                  }}
                                  typeAheadAriaLabel={t("ssfSupportedEvents")}
                                  onToggle={setSupportedEventsOpen}
                                  isOpen={supportedEventsOpen}
                                  selections={selected}
                                  onSelect={(value) => {
                                    const option = value.toString();
                                    if (!option) return;
                                    const next = selected.includes(option)
                                      ? selected.filter((s) => s !== option)
                                      : [...selected, option];
                                    field.onChange(next.join(","));
                                  }}
                                  onClear={() => field.onChange("")}
                                >
                                  {availableSupportedEvents.map((event) => (
                                    <SelectOption key={event} value={event}>
                                      {event}
                                      {nativelyEmittedEvents.includes(
                                        event,
                                      ) && (
                                        <Label
                                          color="blue"
                                          isCompact
                                          className="pf-v5-u-ml-sm"
                                        >
                                          {t("ssfNativelyEmittedBadge")}
                                        </Label>
                                      )}
                                    </SelectOption>
                                  ))}
                                </KeycloakSelect>
                              );
                            }}
                          />
                        </FormGroup>
                        <DefaultSwitchControl
                          name={convertAttributeNameToForm<FormFields>(
                            "attributes.ssf.allowEmitEvents",
                          )}
                          label={t("ssfAllowEmitEvents")}
                          labelIcon={t("ssfAllowEmitEventsHelp")}
                          stringify
                        />
                        <FormGroup
                          label={t("ssfEmitOnlyEvents")}
                          fieldId="ssfEmitOnlyEvents"
                          labelIcon={
                            <HelpItem
                              helpText={t("ssfEmitOnlyEventsHelp")}
                              fieldLabelId="ssfEmitOnlyEvents"
                            />
                          }
                        >
                          <Controller
                            name={convertAttributeNameToForm<FormFields>(
                              "attributes.ssf.emitOnlyEvents",
                            )}
                            control={control}
                            defaultValue=""
                            render={({ field }) => {
                              const supportedEvents =
                                splitSupportedEvents(ssfSupportedEvents);
                              const selected = splitSupportedEvents(
                                field.value,
                              ).filter((e) => supportedEvents.includes(e));
                              return (
                                <KeycloakSelect
                                  toggleId="ssfEmitOnlyEvents"
                                  data-testid="ssfEmitOnlyEvents"
                                  variant={SelectVariant.typeaheadMulti}
                                  chipGroupProps={{
                                    numChips: 5,
                                    expandedText: t("hide"),
                                    collapsedText: t("showRemaining"),
                                  }}
                                  typeAheadAriaLabel={t("ssfEmitOnlyEvents")}
                                  onToggle={setEmitOnlyEventsOpen}
                                  isOpen={emitOnlyEventsOpen}
                                  selections={selected}
                                  onSelect={(value) => {
                                    const option = value.toString();
                                    if (!option) return;
                                    const next = selected.includes(option)
                                      ? selected.filter((s) => s !== option)
                                      : [...selected, option];
                                    field.onChange(next.join(","));
                                  }}
                                  onClear={() => field.onChange("")}
                                  isDisabled={supportedEvents.length === 0}
                                >
                                  {supportedEvents.map((event) => (
                                    <SelectOption key={event} value={event}>
                                      {event}
                                    </SelectOption>
                                  ))}
                                </KeycloakSelect>
                              );
                            }}
                          />
                        </FormGroup>
                        {String(ssfAllowEmitEvents) === "true" && (
                          <FormGroup
                            label={t("ssfEmitEventsRole")}
                            fieldId="ssfEmitEventsRole"
                            labelIcon={
                              <HelpItem
                                helpText={t("ssfEmitEventsRoleHelp")}
                                fieldLabelId="ssfEmitEventsRole"
                              />
                            }
                          >
                            <Controller
                              name={emitEventsRoleFieldName}
                              defaultValue=""
                              control={control}
                              render={({ field }) => (
                                <Split>
                                  {emitRolePickerOpen && (
                                    <AddRoleMappingModal
                                      id="ssfEmitEventsRolePicker"
                                      type="roles"
                                      filterType={emitRoleFilterType}
                                      name="ssfEmitEventsRole"
                                      onAssign={(rows) => {
                                        const row = rows[0];
                                        const value = row.client?.clientId
                                          ? `${row.client.clientId}.${row.role.name}`
                                          : row.role.name;
                                        field.onChange(value);
                                        setEmitRolePickerOpen(false);
                                      }}
                                      onClose={() =>
                                        setEmitRolePickerOpen(false)
                                      }
                                      isRadio
                                    />
                                  )}
                                  {field.value && field.value !== "" && (
                                    <SplitItem>
                                      <Chip
                                        textMaxWidth="500px"
                                        onClick={() => field.onChange("")}
                                      >
                                        <ServiceRole
                                          role={{
                                            name: parseRoleValue(
                                              field.value,
                                            )[1],
                                          }}
                                          client={{
                                            clientId: parseRoleValue(
                                              field.value,
                                            )[0],
                                          }}
                                        />
                                      </Chip>
                                    </SplitItem>
                                  )}
                                  <SplitItem>
                                    <AddRoleButton
                                      label="selectRole.label"
                                      onFilerTypeChange={(type) => {
                                        setEmitRoleFilterType(type);
                                        setEmitRolePickerOpen(true);
                                      }}
                                      variant="secondary"
                                      data-testid="ssfEmitEventsRoleSelect"
                                      isDisabled={false}
                                    />
                                  </SplitItem>
                                </Split>
                              )}
                            />
                          </FormGroup>
                        )}
                      </div>
                    ),
                  },
                ]}
              />
              {saveActionGroup("ssfReceiver")}
            </FormAccess>
          </CardBody>
        </Card>
      )}
      {activeTab === "stream" && (
        <Card isFlat className="pf-v5-u-mt-md">
          <CardHeader>
            <CardTitle>{t("ssfStream")}</CardTitle>
          </CardHeader>
          <CardBody>
            <TextContent>
              <Text>{t("ssfStreamHelp")}</Text>
            </TextContent>
          </CardBody>
          <CardBody>
            <ActionGroup className="pf-v5-u-pb-md">
              <Button variant="link" onClick={refresh} data-testid="ssfRefresh">
                <SyncAltIcon /> {t("refresh")}
              </Button>
            </ActionGroup>
            {!clientStream ? (
              <>
                {!createStreamFormOpen && (
                  <ListEmptyState
                    message={t("ssfStreamNotRegistered")}
                    instructions={t("ssfStreamNotRegisteredHelp")}
                    primaryActionText={t("ssfCreateStream")}
                    onPrimaryAction={() => setCreateStreamFormOpen(true)}
                  />
                )}
                {createStreamFormOpen && (
                  <FormAccess
                    role="manage-clients"
                    fineGrainedAccess={client.access?.configure}
                    isHorizontal
                  >
                    <FormGroup
                      label={t("ssfCreateStreamAudience")}
                      fieldId="ssfCreateStreamAudience"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfCreateStreamAudienceHelp")}
                          fieldLabelId="ssfCreateStreamAudience"
                        />
                      }
                    >
                      <TextInput
                        id="ssfCreateStreamAudience"
                        data-testid="ssfCreateStreamAudience"
                        value={createStreamAudience}
                        onChange={(_e, value) => setCreateStreamAudience(value)}
                      />
                    </FormGroup>
                    <FormGroup
                      label={t("ssfCreateStreamDeliveryMethod")}
                      fieldId="ssfCreateStreamDeliveryMethod"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfCreateStreamDeliveryMethodHelp")}
                          fieldLabelId="ssfCreateStreamDeliveryMethod"
                        />
                      }
                    >
                      <select
                        id="ssfCreateStreamDeliveryMethod"
                        data-testid="ssfCreateStreamDeliveryMethod"
                        className="pf-v5-c-form-control"
                        value={createStreamMethod}
                        onChange={(e) =>
                          setCreateStreamMethod(
                            e.target.value === "POLL" ? "POLL" : "PUSH",
                          )
                        }
                      >
                        <option value="PUSH">{t("ssfDelivery.PUSH")}</option>
                        <option value="POLL">{t("ssfDelivery.POLL")}</option>
                      </select>
                    </FormGroup>
                    {createStreamMethod === "PUSH" && (
                      <>
                        <FormGroup
                          label={t("ssfCreateStreamEndpointUrl")}
                          fieldId="ssfCreateStreamEndpointUrl"
                          isRequired
                          labelIcon={
                            <HelpItem
                              helpText={t("ssfCreateStreamEndpointUrlHelp")}
                              fieldLabelId="ssfCreateStreamEndpointUrl"
                            />
                          }
                        >
                          <TextInput
                            id="ssfCreateStreamEndpointUrl"
                            data-testid="ssfCreateStreamEndpointUrl"
                            isRequired
                            value={createStreamEndpointUrl}
                            onChange={(_e, value) =>
                              setCreateStreamEndpointUrl(value)
                            }
                          />
                        </FormGroup>
                        <FormGroup
                          label={t("ssfStreamPushAuthHeader")}
                          fieldId="ssfCreateStreamAuthHeader"
                          labelIcon={
                            <HelpItem
                              helpText={t("ssfStreamPushAuthHeaderHelp")}
                              fieldLabelId="ssfStreamPushAuthHeader"
                            />
                          }
                        >
                          <InputGroup>
                            <InputGroupItem isFill>
                              <PasswordInput
                                id="ssfCreateStreamAuthHeader"
                                data-testid="ssfCreateStreamAuthHeader"
                                value={createStreamAuthHeader}
                                onChange={(event) =>
                                  setCreateStreamAuthHeader(
                                    (event.target as HTMLInputElement).value,
                                  )
                                }
                              />
                            </InputGroupItem>
                            <InputGroupItem>
                              <CopyToClipboardButton
                                id="ssfCreateStreamAuthHeader"
                                text={createStreamAuthHeader}
                                label="ssfStreamPushAuthHeader"
                                variant="control"
                              />
                            </InputGroupItem>
                          </InputGroup>
                        </FormGroup>
                      </>
                    )}
                    <FormGroup
                      label={t("ssfCreateStreamEventsRequested")}
                      fieldId="ssfCreateStreamEventsRequested"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfCreateStreamEventsRequestedHelp")}
                          fieldLabelId="ssfCreateStreamEventsRequested"
                        />
                      }
                    >
                      <KeycloakSelect
                        toggleId="ssfCreateStreamEventsRequested"
                        data-testid="ssfCreateStreamEventsRequested"
                        variant={SelectVariant.typeaheadMulti}
                        chipGroupProps={{
                          numChips: 5,
                          expandedText: t("hide"),
                          collapsedText: t("showRemaining"),
                        }}
                        typeAheadAriaLabel={t("ssfCreateStreamEventsRequested")}
                        onToggle={setCreateStreamEventsOpen}
                        isOpen={createStreamEventsOpen}
                        selections={createStreamEvents}
                        onSelect={(value) => {
                          const option = value.toString();
                          if (!option) return;
                          setCreateStreamEvents((current) =>
                            current.includes(option)
                              ? current.filter((e) => e !== option)
                              : [...current, option],
                          );
                        }}
                        onClear={() => setCreateStreamEvents([])}
                      >
                        {availableSupportedEvents.map((event) => (
                          <SelectOption key={event} value={event}>
                            {event}
                            {nativelyEmittedEvents.includes(event) && (
                              <Label
                                color="blue"
                                isCompact
                                className="pf-v5-u-ml-sm"
                              >
                                {t("ssfNativelyEmittedBadge")}
                              </Label>
                            )}
                          </SelectOption>
                        ))}
                      </KeycloakSelect>
                    </FormGroup>
                    <FormGroup
                      label={t("ssfCreateStreamDescription")}
                      fieldId="ssfCreateStreamDescription"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfCreateStreamDescriptionHelp")}
                          fieldLabelId="ssfCreateStreamDescription"
                        />
                      }
                    >
                      <TextInput
                        id="ssfCreateStreamDescription"
                        data-testid="ssfCreateStreamDescription"
                        value={createStreamDescription}
                        onChange={(_e, value) =>
                          setCreateStreamDescription(value)
                        }
                      />
                    </FormGroup>
                    <ActionGroup>
                      <Button
                        variant="primary"
                        isDisabled={
                          createStreamSubmitting ||
                          !createStreamEndpointUrl.trim()
                        }
                        onClick={submitCreateStream}
                        data-testid="ssfCreateStreamSubmit"
                      >
                        {t("ssfCreateStream")}
                      </Button>
                      <Button
                        variant="link"
                        onClick={() => {
                          resetCreateStreamForm();
                          setCreateStreamFormOpen(false);
                        }}
                        data-testid="ssfCreateStreamCancel"
                      >
                        {t("cancel")}
                      </Button>
                    </ActionGroup>
                  </FormAccess>
                )}
              </>
            ) : (
              <FormAccess
                role="manage-clients"
                fineGrainedAccess={client.access?.configure}
                isHorizontal
              >
                <FormGroup label={t("ssfStreamId")} fieldId="ssfStreamId">
                  <InputGroup>
                    <InputGroupItem isFill>
                      <TextInput
                        id="ssfStreamId"
                        data-testid="ssfStreamId"
                        readOnlyVariant="default"
                        value={clientStream.streamId ?? ""}
                      />
                    </InputGroupItem>
                    <InputGroupItem>
                      <CopyToClipboardButton
                        id="ssfStreamId"
                        text={clientStream.streamId ?? ""}
                        label="ssfStreamId"
                        variant="control"
                      />
                    </InputGroupItem>
                  </InputGroup>
                </FormGroup>
                {clientStream.description && (
                  <FormGroup
                    label={t("ssfStreamDescription")}
                    fieldId="ssfStreamDescription"
                    labelIcon={
                      <HelpItem
                        helpText={t("ssfStreamDescriptionHelp")}
                        fieldLabelId="ssfStreamDescription"
                      />
                    }
                  >
                    <TextInput
                      id="ssfStreamDescription"
                      data-testid="ssfStreamDescription"
                      readOnlyVariant="default"
                      value={clientStream.description}
                    />
                  </FormGroup>
                )}
                {clientStream.createdAt && (
                  <FormGroup
                    label={t("ssfStreamCreatedAt")}
                    fieldId="ssfStreamCreatedAt"
                    labelIcon={
                      <HelpItem
                        helpText={t("ssfStreamCreatedAtHelp")}
                        fieldLabelId="ssfStreamCreatedAt"
                      />
                    }
                  >
                    <TextInput
                      id="ssfStreamCreatedAt"
                      data-testid="ssfStreamCreatedAt"
                      readOnlyVariant="default"
                      value={formatDate(
                        new Date(clientStream.createdAt * 1000),
                      )}
                    />
                  </FormGroup>
                )}
                {clientStream.updatedAt && (
                  <FormGroup
                    label={t("ssfStreamUpdatedAt")}
                    fieldId="ssfStreamUpdatedAt"
                    labelIcon={
                      <HelpItem
                        helpText={t("ssfStreamUpdatedAtHelp")}
                        fieldLabelId="ssfStreamUpdatedAt"
                      />
                    }
                  >
                    <TextInput
                      id="ssfStreamUpdatedAt"
                      data-testid="ssfStreamUpdatedAt"
                      readOnlyVariant="default"
                      value={formatDate(
                        new Date(clientStream.updatedAt * 1000),
                      )}
                    />
                  </FormGroup>
                )}
                <FormGroup
                  label={t("ssfStreamLastVerifiedAt")}
                  fieldId="ssfStreamLastVerifiedAt"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfStreamLastVerifiedAtHelp")}
                      fieldLabelId="ssfStreamLastVerifiedAt"
                    />
                  }
                >
                  <TextInput
                    id="ssfStreamLastVerifiedAt"
                    data-testid="ssfStreamLastVerifiedAt"
                    readOnlyVariant="default"
                    value={
                      clientStream.lastVerifiedAt
                        ? formatDate(
                            new Date(clientStream.lastVerifiedAt * 1000),
                          )
                        : ""
                    }
                  />
                </FormGroup>
                <FormGroup
                  label={t("ssfStreamAudience")}
                  fieldId="ssfStreamAudienceCurrent"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfStreamAudienceCurrentHelp")}
                      fieldLabelId="ssfStreamAudience"
                    />
                  }
                >
                  <TextInput
                    id="ssfStreamAudienceCurrent"
                    data-testid="ssfStreamAudienceCurrent"
                    readOnlyVariant="default"
                    value={
                      clientStream.audience && clientStream.audience.length > 0
                        ? clientStream.audience.join(", ")
                        : ""
                    }
                  />
                </FormGroup>
                {clientStream.delivery?.endpoint_url && (
                  <FormGroup
                    label={
                      isPollDeliveryMethod(clientStream.delivery.method)
                        ? t("ssfStreamPollEndpointUrl")
                        : t("ssfStreamPushEndpointUrl")
                    }
                    fieldId="ssfStreamEndpointUrl"
                    labelIcon={
                      <HelpItem
                        helpText={
                          isPollDeliveryMethod(clientStream.delivery.method)
                            ? t("ssfStreamPollEndpointUrlHelp")
                            : t("ssfStreamPushEndpointUrlHelp")
                        }
                        fieldLabelId="ssfStreamEndpointUrl"
                      />
                    }
                  >
                    <InputGroup>
                      <InputGroupItem isFill>
                        <TextInput
                          id="ssfStreamEndpointUrl"
                          data-testid="ssfStreamEndpointUrl"
                          readOnlyVariant="default"
                          value={clientStream.delivery.endpoint_url}
                        />
                      </InputGroupItem>
                      <InputGroupItem>
                        <CopyToClipboardButton
                          id="ssfStreamEndpointUrl"
                          text={clientStream.delivery.endpoint_url}
                          label="ssfStreamEndpointUrl"
                          variant="control"
                        />
                      </InputGroupItem>
                    </InputGroup>
                  </FormGroup>
                )}
                {/* Push auth header is irrelevant for POLL — receivers
                  authenticate themselves with their own bearer token to
                  call the transmitter-hosted poll endpoint, and the
                  transmitter doesn't store an outbound auth header for
                  POLL streams. */}
                {clientStream.delivery?.endpoint_url &&
                  !isPollDeliveryMethod(clientStream.delivery.method) && (
                    <FormGroup
                      label={t("ssfStreamPushAuthHeader")}
                      fieldId="ssfStreamPushAuthHeader"
                      labelIcon={
                        <HelpItem
                          helpText={t("ssfStreamPushAuthHeaderHelp")}
                          fieldLabelId="ssfStreamPushAuthHeader"
                        />
                      }
                    >
                      <InputGroup>
                        <InputGroupItem isFill>
                          <PasswordInput
                            id="ssfStreamPushAuthHeader"
                            data-testid="ssfStreamPushAuthHeader"
                            readOnly
                            value={
                              clientStream.delivery.authorization_header ?? ""
                            }
                          />
                        </InputGroupItem>
                        <InputGroupItem>
                          <CopyToClipboardButton
                            id="ssfStreamPushAuthHeader"
                            text={
                              clientStream.delivery.authorization_header ?? ""
                            }
                            label="ssfStreamPushAuthHeader"
                            variant="control"
                          />
                        </InputGroupItem>
                      </InputGroup>
                    </FormGroup>
                  )}
                <SelectControl
                  name={convertAttributeNameToForm<FormFields>(
                    "attributes.ssf.status",
                  )}
                  label={t("ssfStreamStatus")}
                  labelIcon={t("ssfStreamStatusHelp")}
                  controller={{
                    defaultValue: "enabled",
                  }}
                  options={[
                    { key: "enabled", value: t("ssfStreamStatus.enabled") },
                    { key: "paused", value: t("ssfStreamStatus.paused") },
                    {
                      key: "disabled",
                      value: t("ssfStreamStatus.disabled"),
                    },
                  ]}
                />
                <FormGroup
                  label={t("ssfStreamStatusReason")}
                  fieldId="ssfStreamStatusReason"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfStreamStatusReasonHelp")}
                      fieldLabelId="ssfStreamStatusReason"
                    />
                  }
                >
                  <TextInput
                    id="ssfStreamStatusReason"
                    data-testid="ssfStreamStatusReason"
                    readOnlyVariant="default"
                    value={clientStream.statusReason ?? ""}
                  />
                </FormGroup>
                <FormGroup
                  label={t("ssfEventsRequested")}
                  fieldId="ssfEventsRequested"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfEventsRequestedHelp")}
                      fieldLabelId="ssfEventsRequested"
                    />
                  }
                >
                  {clientStream.eventsRequested &&
                  clientStream.eventsRequested.length > 0 ? (
                    <KeycloakSelect
                      toggleId="ssfEventsRequested"
                      data-testid="ssfEventsRequested"
                      variant={SelectVariant.typeaheadMulti}
                      isDisabled
                      chipGroupProps={{
                        numChips: 5,
                        expandedText: t("hide"),
                        collapsedText: t("showRemaining"),
                      }}
                      typeAheadAriaLabel={t("ssfEventsRequested")}
                      onToggle={noop}
                      isOpen={false}
                      selections={clientStream.eventsRequested}
                      onSelect={noop}
                    >
                      {clientStream.eventsRequested.map((event) => (
                        <SelectOption key={event} value={event}>
                          {event}
                        </SelectOption>
                      ))}
                    </KeycloakSelect>
                  ) : (
                    <TextInput
                      id="ssfEventsRequested"
                      data-testid="ssfEventsRequested"
                      readOnlyVariant="default"
                      value={t("ssfEventsRequestedEmpty")}
                    />
                  )}
                </FormGroup>
                <FormGroup
                  label={t("ssfEventsDelivered")}
                  fieldId="ssfEventsDelivered"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfEventsDeliveredHelp")}
                      fieldLabelId="ssfEventsDelivered"
                    />
                  }
                >
                  {clientStream.eventsDelivered &&
                  clientStream.eventsDelivered.length > 0 ? (
                    <KeycloakSelect
                      toggleId="ssfEventsDelivered"
                      data-testid="ssfEventsDelivered"
                      variant={SelectVariant.typeaheadMulti}
                      isDisabled
                      chipGroupProps={{
                        numChips: 5,
                        expandedText: t("hide"),
                        collapsedText: t("showRemaining"),
                      }}
                      typeAheadAriaLabel={t("ssfEventsDelivered")}
                      onToggle={noop}
                      isOpen={false}
                      selections={clientStream.eventsDelivered}
                      onSelect={noop}
                    >
                      {clientStream.eventsDelivered.map((event) => (
                        <SelectOption key={event} value={event}>
                          {event}
                        </SelectOption>
                      ))}
                    </KeycloakSelect>
                  ) : (
                    <TextInput
                      id="ssfEventsDelivered"
                      data-testid="ssfEventsDelivered"
                      readOnlyVariant="default"
                      value={t("ssfEventsDeliveredEmpty")}
                    />
                  )}
                </FormGroup>
                <FormGroup
                  label={t("ssfDelivery")}
                  fieldId="ssfDelivery"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfDeliveryHelp")}
                      fieldLabelId="ssfDelivery"
                    />
                  }
                >
                  <TextInput
                    id="ssfDelivery"
                    data-testid="ssfDelivery"
                    readOnlyVariant="default"
                    value={
                      isPollDeliveryMethod(clientStream.delivery?.method)
                        ? t("ssfDelivery.POLL")
                        : t("ssfDelivery.PUSH")
                    }
                  />
                </FormGroup>
                {/* Push timeouts only apply when Keycloak makes outbound
                  HTTP push requests. POLL is inbound (receivers call
                  the transmitter), so the timeout knobs have no effect
                  and we hide them to avoid suggesting otherwise. */}
                {(ssfDelivery === "PUSH" || !ssfDelivery) &&
                  !isPollDeliveryMethod(clientStream.delivery?.method) && (
                    <>
                      <NumberControl
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.pushEndpointConnectTimeoutMillis",
                        )}
                        label={t("ssfPushEndpointConnectTimeout")}
                        labelIcon={t("ssfPushEndpointConnectTimeoutHelp")}
                        controller={{
                          defaultValue: defaultPushConnectTimeoutMillis,
                          rules: {
                            min: 0,
                          },
                        }}
                      />
                      <NumberControl
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.pushEndpointSocketTimeoutMillis",
                        )}
                        label={t("ssfPushEndpointSocketTimeout")}
                        labelIcon={t("ssfPushEndpointSocketTimeoutHelp")}
                        controller={{
                          defaultValue: defaultPushSocketTimeoutMillis,
                          rules: {
                            min: 0,
                          },
                        }}
                      />
                    </>
                  )}
                <ActionGroup>
                  <Button
                    variant="secondary"
                    onClick={() => save()}
                    data-testid="ssfStreamSave"
                  >
                    {t("save")}
                  </Button>
                  <Button
                    variant="link"
                    onClick={reset}
                    data-testid="ssfStreamRevert"
                  >
                    {t("revert")}
                  </Button>
                  <Button
                    variant="tertiary"
                    onClick={triggerVerifyStream}
                    data-testid="ssfStreamVerify"
                  >
                    {t("ssfVerifyStream")}
                  </Button>
                  <Button
                    variant="danger"
                    onClick={toggleDeleteStreamDialog}
                    data-testid="ssfStreamDelete"
                  >
                    {t("ssfDeleteStream")}
                  </Button>
                </ActionGroup>
              </FormAccess>
            )}
          </CardBody>
        </Card>
      )}
      {activeTab === "subjects" && <SubjectsTab client={client} />}
      {activeTab === "pending-events" && (
        <EventsTab
          client={client}
          availableSupportedEvents={availableSupportedEvents}
          nativelyEmittedEvents={nativelyEmittedEvents}
        />
      )}
    </PageSection>
  );
};
