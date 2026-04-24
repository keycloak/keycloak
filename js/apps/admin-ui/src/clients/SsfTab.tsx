import { fetchWithError } from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  ListEmptyState,
  NumberControl,
  PasswordInput,
  SelectControl,
  SelectVariant,
  TextAreaControl,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import type { SsfClientTab } from "./routes/ClientSsfTab";
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
import { CopyToClipboardButton } from "../components/copy-to-clipboard-button/CopyToClipboardButton";
import { DefaultSwitchControl } from "../components/SwitchControl";
import {
  AddRoleButton,
  AddRoleMappingModal,
  FilterType,
} from "../components/role-mapping/AddRoleMappingModal";
import { ServiceRole } from "../components/role-mapping/RoleMapping";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../components/form/FormAccess";
import { useRealm } from "../context/realm-context/RealmContext";
import { addTrailingSlash, convertAttributeNameToForm } from "../util";
import { getAuthorizationHeaders } from "../utils/getAuthorizationHeaders";
import useFormatDate from "../utils/useFormatDate";
import type { FormFields, SaveOptions } from "./ClientDetails";
import { TimeSelector } from "../components/time-selector/TimeSelector";
import CodeEditor from "../components/form/CodeEditor";

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

/**
 * Replaces supported text-level placeholders in a raw JSON payload
 * before it's handed to {@code JSON.parse}. Today only {@code __now__}
 * is recognized; it expands to the current Unix time in seconds as a
 * bare integer literal, so it works for both unquoted numeric fields
 * ({@code "event_timestamp": __now__}) and quoted string fields
 * ({@code "some_field": "__now__"}). The expansion runs in plain text
 * so it sidesteps the JSON grammar — no need for a custom parser.
 */
const substitutePayloadPlaceholders = (raw: string): string =>
  raw.replace(/__now__/g, String(Math.floor(Date.now() / 1000)));

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

type SsfPendingEvent = {
  jti: string;
  eventType?: string;
  deliveryMethod?: string;
  status?: string;
  attempts?: number;
  createdAt?: number;
  nextAttemptAt?: number;
  deliveredAt?: number;
  lastError?: string;
  streamId?: string;
  // Decoded JWS payload of the Security Event Token — the full
  // claim set the receiver will process (iss/iat/jti/aud/txn plus
  // the subject and event body). Rendered as formatted JSON in the
  // lookup result so the operator sees exactly what goes on the
  // wire.
  decodedSet?: Record<string, unknown>;
  // Resolved Keycloak user UUID — server-side maps the SET's
  // subject through SubjectResolver. Null for org-only /
  // unresolvable subjects; the UI then omits the user
  // click-through.
  userId?: string;
};

type SsfEmitResult = {
  status: string;
  jti?: string;
};

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
  const [manualOnlyEventsOpen, setManualOnlyEventsOpen] = useState(false);
  const [streamFetchKey, setStreamFetchKey] = useState(0);
  const [configFetchKey, setConfigFetchKey] = useState(0);

  // --- Pending Events tab state ---
  const [pendingLookupJti, setPendingLookupJti] = useState("");
  const [pendingLookupResult, setPendingLookupResult] =
    useState<SsfPendingEvent | null>(null);
  const [pendingLookupError, setPendingLookupError] = useState<string | null>(
    null,
  );
  const [pendingActionLoading, setPendingActionLoading] = useState(false);
  const [emitEventType, setEmitEventType] = useState("");
  // Same shorthand the Subjects tab takes — backend resolves via the
  // SubjectManagementService.resolveByAdminType path.
  const [emitSubjectType, setEmitSubjectType] = useState<
    "user-email" | "user-id" | "user-username" | "org-alias"
  >("user-email");
  const [emitSubjectValue, setEmitSubjectValue] = useState("");
  // Default to an empty JSON object with a newline between the
  // braces so the caret lands on an indented line ready for typing.
  const [emitPayload, setEmitPayload] = useState("{\n  \n}");
  // Live JSON parse-error message — updated on every change so the
  // operator sees the problem as they type rather than only after
  // hitting Emit. The submit handler also uses this to short-circuit
  // before the HTTP call.
  const [emitPayloadParseError, setEmitPayloadParseError] = useState<
    string | null
  >(null);
  const [emitResult, setEmitResult] = useState<SsfEmitResult | null>(null);
  const [emitError, setEmitError] = useState<string | null>(null);

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

  // --- SSF Subjects section state ---
  type SubjectType = "user-id" | "user-email" | "user-username" | "org-alias";
  const [subjectType, setSubjectType] = useState<SubjectType>("user-email");
  const [subjectValue, setSubjectValue] = useState("");
  const [subjectStatus, setSubjectStatus] = useState<{
    variant: "success" | "danger" | "info";
    message: string;
  } | null>(null);
  const [subjectLoading, setSubjectLoading] = useState(false);

  const ssfNotifyKey = `ssf.notify.${client.clientId}`;

  const callSubjectAdminEndpoint = async (
    action: "subjects/add" | "subjects/remove" | "subjects/ignore",
  ) => {
    const baseUrl = addTrailingSlash(adminClient.baseUrl);
    const res = await fetch(
      `${baseUrl}admin/realms/${realm}/ssf/clients/${client.clientId}/${action}`,
      {
        method: "POST",
        headers: {
          ...getAuthorizationHeaders(await adminClient.getAccessToken()),
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ type: subjectType, value: subjectValue }),
      },
    );
    return res;
  };

  const checkSubjectViaAdminApi = async () => {
    const baseUrl = addTrailingSlash(adminClient.baseUrl);
    const headers = getAuthorizationHeaders(await adminClient.getAccessToken());

    let attrs: Record<string, string[]> = {};

    if (subjectType === "user-id") {
      const res = await fetch(
        `${baseUrl}admin/realms/${realm}/users/${encodeURIComponent(subjectValue)}`,
        { headers },
      );
      if (!res.ok) return null;
      const user = await res.json();
      attrs = user.attributes ?? {};
    } else if (subjectType === "user-email") {
      const res = await fetch(
        `${baseUrl}admin/realms/${realm}/users?email=${encodeURIComponent(subjectValue)}&exact=true&max=1&briefRepresentation=false`,
        { headers },
      );
      if (!res.ok) return null;
      const users = await res.json();
      if (!Array.isArray(users) || users.length === 0) return null;
      attrs = users[0].attributes ?? {};
    } else if (subjectType === "user-username") {
      const res = await fetch(
        `${baseUrl}admin/realms/${realm}/users?username=${encodeURIComponent(subjectValue)}&exact=true&max=1&briefRepresentation=false`,
        { headers },
      );
      if (!res.ok) return null;
      const users = await res.json();
      if (!Array.isArray(users) || users.length === 0) return null;
      attrs = users[0].attributes ?? {};
    } else if (subjectType === "org-alias") {
      const res = await fetch(
        `${baseUrl}admin/realms/${realm}/organizations?search=${encodeURIComponent(subjectValue)}&exact=true&first=0&max=1`,
        { headers },
      );
      if (!res.ok) return null;
      const orgs = await res.json();
      if (!Array.isArray(orgs) || orgs.length === 0) return null;
      attrs = orgs[0].attributes ?? {};
    }

    const values = attrs[ssfNotifyKey];
    if (!values) return "absent";
    if (values.includes("true")) return "notified";
    if (values.includes("false")) return "ignored";
    return "absent";
  };

  const handleSubjectAction = async (
    action: "add" | "remove" | "ignore" | "check",
  ) => {
    if (!subjectValue.trim()) {
      setSubjectStatus({
        variant: "danger",
        message: t("ssfSubjectValueRequired"),
      });
      return;
    }
    setSubjectLoading(true);
    setSubjectStatus(null);
    try {
      if (action === "check") {
        const status = await checkSubjectViaAdminApi();
        if (status === null) {
          setSubjectStatus({
            variant: "danger",
            message: t("ssfSubjectNotFound"),
          });
        } else if (status === "notified") {
          setSubjectStatus({
            variant: "success",
            message: t("ssfSubjectIsNotified"),
          });
        } else if (status === "ignored") {
          setSubjectStatus({
            variant: "danger",
            message: t("ssfSubjectIsIgnored"),
          });
        } else {
          setSubjectStatus({
            variant: "info",
            message: t("ssfSubjectIsNotNotified"),
          });
        }
        return;
      }

      const endpoint =
        action === "add"
          ? "subjects/add"
          : action === "ignore"
            ? "subjects/ignore"
            : "subjects/remove";
      const res = await callSubjectAdminEndpoint(endpoint);

      if (res.status === 404) {
        setSubjectStatus({
          variant: "danger",
          message: t("ssfSubjectNotFound"),
        });
        return;
      }
      if (!res.ok) {
        const body = await res.text();
        throw new Error(`${res.status} ${body || res.statusText}`);
      }

      setSubjectStatus({
        variant: "success",
        message:
          action === "add"
            ? t("ssfSubjectAdded")
            : action === "ignore"
              ? t("ssfSubjectIgnored")
              : t("ssfSubjectRemoved"),
      });
    } catch (error) {
      setSubjectStatus({
        variant: "danger",
        message: String(error),
      });
    } finally {
      setSubjectLoading(false);
    }
  };

  const refresh = () => {
    setStreamFetchKey((k) => k + 1);
    setConfigFetchKey((k) => k + 1);
  };

  /**
   * Looks up a single outbox row by jti scoped to this receiver — calls
   * GET /admin/realms/{realm}/ssf/clients/{uuid}/pending-events/{jti}.
   * 404 is rendered as a "not found" message rather than an alert; any
   * other failure surfaces both inline and via addError.
   */
  const handlePendingLookup = async () => {
    if (!client.id || !pendingLookupJti?.trim()) {
      return;
    }
    // Don't wipe the previous result/error here — that causes the
    // result block to unmount during the request, which collapses the
    // container height and makes the layout jump on repeated clicks.
    // We update them in place once the new fetch resolves.
    setPendingActionLoading(true);
    try {
      const response = await fetch(
        `${addTrailingSlash(adminClient.baseUrl)}admin/realms/${realm}/ssf/clients/${client.clientId}/pending-events/${encodeURIComponent(pendingLookupJti.trim())}`,
        {
          headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
        },
      );
      if (response.status === 404) {
        setPendingLookupError(t("ssfPendingLookupNotFound"));
        setPendingLookupResult(null);
        return;
      }
      if (!response.ok) {
        const text = await response.text();
        setPendingLookupError(text || `HTTP ${response.status}`);
        setPendingLookupResult(null);
        return;
      }
      setPendingLookupResult((await response.json()) as SsfPendingEvent);
      setPendingLookupError(null);
    } catch (error) {
      setPendingLookupError(String(error));
      setPendingLookupResult(null);
    } finally {
      setPendingActionLoading(false);
    }
  };

  /**
   * Emits a synthetic SSF event for this receiver via the admin-emit
   * endpoint. Builds an iss_sub subject from the realm issuer and the
   * provided userId so the admin only has to supply the user; the event
   * payload is pasted verbatim as JSON. On success, pre-fills the
   * lookup form with the returned jti so the operator can click
   * "Lookup" to immediately inspect the freshly-enqueued row.
   */
  const handleEmitEvent = async () => {
    if (!client.id) {
      return;
    }
    if (!emitEventType) {
      setEmitError(t("ssfEmitEventTypeRequired"));
      return;
    }
    if (!emitSubjectValue.trim()) {
      setEmitError(t("ssfEmitSubjectValueRequired"));
      return;
    }
    let parsedPayload: unknown;
    try {
      parsedPayload =
        emitPayload.trim() === ""
          ? {}
          : JSON.parse(substitutePayloadPlaceholders(emitPayload));
    } catch (error) {
      const message = t("ssfEmitPayloadInvalidJson", { error: String(error) });
      setEmitError(message);
      setEmitPayloadParseError(message);
      return;
    }
    // Don't wipe the previous result/error here — see the matching
    // comment on handlePendingLookup. Old state stays visible while
    // the new request is in flight; updated atomically below.
    setPendingActionLoading(true);
    try {
      // Send the (subjectType, subjectValue) shorthand — the admin
      // emit backend resolves it through the same path the
      // /subjects:add endpoint uses, then for user subjects builds
      // the sub_id via the receiver-format-aware mapper so the
      // resulting SET matches the shape native events for this
      // receiver would have. Org subjects produce a complex tenant-
      // only subject for org-scoped routing.
      const body = {
        eventType: emitEventType,
        subjectType: emitSubjectType,
        subjectValue: emitSubjectValue.trim(),
        event: parsedPayload,
      };
      const response = await fetch(
        `${addTrailingSlash(adminClient.baseUrl)}admin/realms/${realm}/ssf/clients/${client.clientId}/events/emit`,
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
        setEmitError(text || `HTTP ${response.status}`);
        setEmitResult(null);
        return;
      }
      const result = (await response.json()) as SsfEmitResult;
      setEmitResult(result);
      setEmitError(null);
      // Pre-fill the lookup field so the operator can immediately
      // click Lookup to inspect the freshly-enqueued outbox row.
      // The server may omit jti for filter-dropped emissions that
      // never minted a SET — don't clobber the existing value with
      // undefined in that case.
      if (result.jti) {
        setPendingLookupJti(result.jti);
      }
    } catch (error) {
      setEmitError(String(error));
      setEmitResult(null);
    } finally {
      setPendingActionLoading(false);
    }
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

  const ssfVerificationTrigger = watch(
    convertAttributeNameToForm<FormFields>(
      "attributes.ssf.verificationTrigger",
    ),
  );
  const ssfDelivery = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.delivery"),
  );
  // DefaultSwitchControl with stringify persists "true" / "false" —
  // compare as string so the role picker toggles in sync with the
  // switch rather than interpreting the raw boolean.
  const ssfAllowEmitEvents = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.allowEmitEvents"),
  );

  // Drive the manual-only multi-select options off the live value of
  // supportedEvents so adding / removing a supported event immediately
  // adjusts what the operator can mark manual-only. No standalone
  // registry list — the manual-only set is a strict subset.
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
      "ssf.manualOnlyEvents",
      "ssf.profile",
      "ssf.userSubjectFormat",
      "ssf.defaultSubjects",
      "ssf.autoNotifyOnLogin",
      "ssf.requireServiceAccount",
      "ssf.requiredRole",
      "ssf.allowEmitEvents",
      "ssf.emitEventsRole",
      "ssf.minVerificationInterval",
      "ssf.verificationTrigger",
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
                  { key: "SSE_CAEP", value: t("ssfProfile.SSE_CAEP") },
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
                    value: t("ssfUserSubjectFormat.complex.iss_sub+tenant"),
                  },
                  {
                    key: "complex.email+tenant",
                    value: t("ssfUserSubjectFormat.complex.email+tenant"),
                  },
                ]}
              />
              <SelectControl
                name={convertAttributeNameToForm<FormFields>(
                  "attributes.ssf.verificationTrigger",
                )}
                label={t("ssfVerification")}
                labelIcon={t("ssfVerificationHelp")}
                controller={{
                  defaultValue: "RECEIVER_INITIATED",
                }}
                options={[
                  {
                    key: "RECEIVER_INITIATED",
                    value: t("ssfVerification.RECEIVER_INITIATED"),
                  },
                  {
                    key: "TRANSMITTER_INITIATED",
                    value: t("ssfVerification.TRANSMITTER_INITIATED"),
                  },
                ]}
              />
              {ssfVerificationTrigger === "TRANSMITTER_INITIATED" && (
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
                  "attributes.ssf.autoNotifyOnLogin",
                )}
                label={t("ssfAutoNotifyOnLogin")}
                labelIcon={t("ssfAutoNotifyOnLoginHelp")}
                stringify
              />
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
                                clientId: parseRoleValue(field.value)[0],
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
                    const selected = splitSupportedEvents(field.value);
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
                    );
                  }}
                />
              </FormGroup>
              <FormGroup
                label={t("ssfManualOnlyEvents")}
                fieldId="ssfManualOnlyEvents"
                labelIcon={
                  <HelpItem
                    helpText={t("ssfManualOnlyEventsHelp")}
                    fieldLabelId="ssfManualOnlyEvents"
                  />
                }
              >
                <Controller
                  name={convertAttributeNameToForm<FormFields>(
                    "attributes.ssf.manualOnlyEvents",
                  )}
                  control={control}
                  defaultValue=""
                  render={({ field }) => {
                    const supportedEvents =
                      splitSupportedEvents(ssfSupportedEvents);
                    const selected = splitSupportedEvents(field.value).filter(
                      (e) => supportedEvents.includes(e),
                    );
                    return (
                      <KeycloakSelect
                        toggleId="ssfManualOnlyEvents"
                        data-testid="ssfManualOnlyEvents"
                        variant={SelectVariant.typeaheadMulti}
                        chipGroupProps={{
                          numChips: 5,
                          expandedText: t("hide"),
                          collapsedText: t("showRemaining"),
                        }}
                        typeAheadAriaLabel={t("ssfManualOnlyEvents")}
                        onToggle={setManualOnlyEventsOpen}
                        isOpen={manualOnlyEventsOpen}
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
              <DefaultSwitchControl
                name={convertAttributeNameToForm<FormFields>(
                  "attributes.ssf.allowEmitEvents",
                )}
                label={t("ssfAllowEmitEvents")}
                labelIcon={t("ssfAllowEmitEventsHelp")}
                stringify
              />
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
                            onClose={() => setEmitRolePickerOpen(false)}
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
                                  clientId: parseRoleValue(field.value)[0],
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
                <SelectControl
                  name={convertAttributeNameToForm<FormFields>(
                    "attributes.ssf.delivery",
                  )}
                  label={t("ssfDelivery")}
                  labelIcon={t("ssfDeliveryHelp")}
                  controller={{
                    defaultValue: "PUSH",
                  }}
                  options={[
                    { key: "PUSH", value: t("ssfDelivery.PUSH") },
                    { key: "POLL", value: t("ssfDelivery.POLL") },
                  ]}
                />
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
      {activeTab === "subjects" && (
        <Card isFlat className="pf-v5-u-mt-md">
          <CardHeader>
            <CardTitle>{t("ssfTabSubjects")}</CardTitle>
          </CardHeader>
          <CardBody>
            <TextContent>
              <Text>{t("ssfSubjectsHelp")}</Text>
            </TextContent>
          </CardBody>
          <CardBody>
            <FormAccess
              role="manage-clients"
              fineGrainedAccess={client.access?.configure}
              isHorizontal
            >
              <FormGroup label={t("ssfSubjectType")} fieldId="ssfSubjectType">
                <select
                  id="ssfSubjectType"
                  data-testid="ssfSubjectType"
                  value={subjectType}
                  onChange={(e) =>
                    setSubjectType(e.target.value as SubjectType)
                  }
                  className="pf-v5-c-form-control"
                >
                  <option value="user-email">
                    {t("ssfSubjectType.userEmail")}
                  </option>
                  <option value="user-id">{t("ssfSubjectType.userId")}</option>
                  <option value="user-username">
                    {t("ssfSubjectType.userUsername")}
                  </option>
                  <option value="org-alias">
                    {t("ssfSubjectType.orgAlias")}
                  </option>
                </select>
              </FormGroup>
              <FormGroup
                label={t("ssfSubjectValue")}
                fieldId="ssfSubjectValue"
                isRequired
              >
                <InputGroup>
                  <InputGroupItem isFill>
                    <TextInput
                      id="ssfSubjectValue"
                      data-testid="ssfSubjectValue"
                      value={subjectValue}
                      onChange={(_e, value) => setSubjectValue(value)}
                      placeholder={
                        subjectType === "user-email"
                          ? "user@example.com"
                          : subjectType === "user-id"
                            ? "user-uuid"
                            : subjectType === "user-username"
                              ? "username"
                              : "org-alias"
                      }
                    />
                  </InputGroupItem>
                </InputGroup>
              </FormGroup>
              <ActionGroup>
                <Button
                  variant="primary"
                  onClick={() => handleSubjectAction("add")}
                  isDisabled={subjectLoading}
                  data-testid="ssfSubjectAdd"
                >
                  {t("ssfSubjectAdd")}
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => handleSubjectAction("ignore")}
                  isDisabled={subjectLoading}
                  data-testid="ssfSubjectIgnore"
                >
                  {t("ssfSubjectIgnore")}
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => handleSubjectAction("remove")}
                  isDisabled={subjectLoading}
                  data-testid="ssfSubjectRemove"
                >
                  {t("ssfSubjectRemove")}
                </Button>
                <Button
                  variant="tertiary"
                  onClick={() => handleSubjectAction("check")}
                  isDisabled={subjectLoading}
                  data-testid="ssfSubjectCheck"
                >
                  {t("ssfSubjectCheck")}
                </Button>
              </ActionGroup>
              {subjectStatus && (
                <Text
                  className={`pf-v5-u-mt-md ${
                    subjectStatus.variant === "success"
                      ? "pf-v5-u-color-status-success--100"
                      : subjectStatus.variant === "danger"
                        ? "pf-v5-u-color-status-danger--100"
                        : "pf-v5-u-color-status-info--100"
                  }`}
                  data-testid="ssfSubjectStatus"
                >
                  {subjectStatus.message}
                </Text>
              )}
            </FormAccess>
          </CardBody>
        </Card>
      )}
      {activeTab === "pending-events" && (
        <>
          {/* Lookup section ------------------------------------------ */}
          <Card isFlat className="pf-v5-u-mt-md">
            <CardHeader>
              <CardTitle>{t("ssfLookupTitle")}</CardTitle>
            </CardHeader>
            <CardBody>
              <TextContent>
                <Text>{t("ssfLookupTitleHelp")}</Text>
              </TextContent>
            </CardBody>
            <CardBody>
              <FormAccess
                role="manage-clients"
                fineGrainedAccess={client.access?.configure}
                isHorizontal
              >
                <FormGroup
                  label={t("ssfPendingLookupJti")}
                  fieldId="ssfPendingLookupJti"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfPendingLookupJtiHelp")}
                      fieldLabelId="ssfPendingLookupJti"
                    />
                  }
                  isRequired
                >
                  <InputGroup>
                    <InputGroupItem isFill>
                      <TextInput
                        id="ssfPendingLookupJti"
                        data-testid="ssfPendingLookupJti"
                        value={pendingLookupJti}
                        onChange={(_e, value) => setPendingLookupJti(value)}
                        placeholder={t("ssfPendingLookupJtiPlaceholder")}
                      />
                    </InputGroupItem>
                  </InputGroup>
                </FormGroup>
                <ActionGroup>
                  <Button
                    variant="primary"
                    onClick={handlePendingLookup}
                    isDisabled={
                      pendingActionLoading || !pendingLookupJti?.trim()
                    }
                    data-testid="ssfPendingLookup"
                  >
                    {t("ssfPendingLookup")}
                  </Button>
                </ActionGroup>
                {pendingLookupError && (
                  <Text
                    className="pf-v5-u-mt-md pf-v5-u-color-status-danger--100"
                    data-testid="ssfPendingLookupError"
                  >
                    {pendingLookupError}
                  </Text>
                )}
                {pendingLookupResult && (
                  <FormGroup
                    label={t("ssfPendingLookupResult")}
                    fieldId="ssfPendingLookupResult"
                  >
                    <TextContent data-testid="ssfPendingLookupResult">
                      <Text>
                        <strong>{t("ssfPendingFieldStatus")}:</strong>{" "}
                        {pendingLookupResult.status ?? "-"}
                      </Text>
                      <Text>
                        <strong>{t("ssfPendingFieldEventType")}:</strong>{" "}
                        {pendingLookupResult.eventType ?? "-"}
                      </Text>
                      <Text>
                        <strong>{t("ssfPendingFieldDeliveryMethod")}:</strong>{" "}
                        {pendingLookupResult.deliveryMethod ?? "-"}
                      </Text>
                      {/* Attempts + Next attempt at are PUSH drainer
                      retry state — for POLL the receiver pulls on its
                      own cadence and these fields carry no useful
                      information. Hide them for POLL rows to avoid
                      operator confusion. */}
                      {pendingLookupResult.deliveryMethod !== "POLL" && (
                        <Text>
                          <strong>{t("ssfPendingFieldAttempts")}:</strong>{" "}
                          {pendingLookupResult.attempts ?? 0}
                        </Text>
                      )}
                      <Text>
                        <strong>{t("ssfPendingFieldCreatedAt")}:</strong>{" "}
                        {pendingLookupResult.createdAt
                          ? formatDate(
                              new Date(pendingLookupResult.createdAt * 1000),
                            )
                          : "-"}
                      </Text>
                      {pendingLookupResult.deliveryMethod !== "POLL" && (
                        <Text>
                          <strong>{t("ssfPendingFieldNextAttemptAt")}:</strong>{" "}
                          {pendingLookupResult.nextAttemptAt
                            ? formatDate(
                                new Date(
                                  pendingLookupResult.nextAttemptAt * 1000,
                                ),
                              )
                            : "-"}
                        </Text>
                      )}
                      <Text>
                        <strong>{t("ssfPendingFieldDeliveredAt")}:</strong>{" "}
                        {pendingLookupResult.deliveredAt
                          ? formatDate(
                              new Date(pendingLookupResult.deliveredAt * 1000),
                            )
                          : "-"}
                      </Text>
                      {pendingLookupResult.lastError && (
                        <Text>
                          <strong>{t("ssfPendingFieldLastError")}:</strong>{" "}
                          {pendingLookupResult.lastError}
                        </Text>
                      )}
                      {pendingLookupResult.userId && (
                        <Text>
                          <strong>{t("ssfPendingFieldUserId")}:</strong>{" "}
                          {pendingLookupResult.userId}
                        </Text>
                      )}
                      {pendingLookupResult.decodedSet && (
                        <>
                          <Text>
                            <strong>{t("ssfPendingFieldDecodedSet")}:</strong>
                          </Text>
                          <pre
                            data-testid="ssfPendingFieldDecodedSetJson"
                            className="pf-v5-u-font-family-monospace"
                          >
                            {JSON.stringify(
                              pendingLookupResult.decodedSet,
                              null,
                              2,
                            )}
                          </pre>
                        </>
                      )}
                    </TextContent>
                  </FormGroup>
                )}
              </FormAccess>
            </CardBody>
          </Card>

          {/* Emit section -------------------------------------------- */}
          <Card isFlat className="pf-v5-u-mt-xl">
            <CardHeader>
              <CardTitle>{t("ssfEmitTitle")}</CardTitle>
            </CardHeader>
            <CardBody>
              <TextContent>
                <Text>{t("ssfEmitTitleHelp")}</Text>
              </TextContent>
            </CardBody>
            <CardBody>
              <FormAccess
                role="manage-clients"
                fineGrainedAccess={client.access?.configure}
                isHorizontal
              >
                <FormGroup
                  label={t("ssfEmitEventType")}
                  fieldId="ssfEmitEventType"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfEmitEventTypeHelp")}
                      fieldLabelId="ssfEmitEventType"
                    />
                  }
                  isRequired
                >
                  <select
                    id="ssfEmitEventType"
                    data-testid="ssfEmitEventType"
                    value={emitEventType}
                    onChange={(e) => setEmitEventType(e.target.value)}
                    className="pf-v5-c-form-control"
                  >
                    <option value="">
                      {t("ssfEmitEventTypeSelectPrompt")}
                    </option>
                    {availableSupportedEvents.map((eventType) => (
                      <option key={eventType} value={eventType}>
                        {eventType}
                        {nativelyEmittedEvents.includes(eventType)
                          ? ` (${t("ssfNativelyEmittedBadge")})`
                          : ""}
                      </option>
                    ))}
                  </select>
                </FormGroup>
                <FormGroup
                  label={t("ssfSubjectType")}
                  fieldId="ssfEmitSubjectType"
                >
                  <select
                    id="ssfEmitSubjectType"
                    data-testid="ssfEmitSubjectType"
                    value={emitSubjectType}
                    onChange={(e) =>
                      setEmitSubjectType(
                        e.target.value as typeof emitSubjectType,
                      )
                    }
                    className="pf-v5-c-form-control"
                  >
                    <option value="user-email">
                      {t("ssfSubjectType.userEmail")}
                    </option>
                    <option value="user-id">
                      {t("ssfSubjectType.userId")}
                    </option>
                    <option value="user-username">
                      {t("ssfSubjectType.userUsername")}
                    </option>
                    <option value="org-alias">
                      {t("ssfSubjectType.orgAlias")}
                    </option>
                  </select>
                </FormGroup>
                <FormGroup
                  label={t("ssfSubjectValue")}
                  fieldId="ssfEmitSubjectValue"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfEmitSubjectValueHelp")}
                      fieldLabelId="ssfEmitSubjectValue"
                    />
                  }
                  isRequired
                >
                  <TextInput
                    id="ssfEmitSubjectValue"
                    data-testid="ssfEmitSubjectValue"
                    value={emitSubjectValue}
                    onChange={(_e, value) => setEmitSubjectValue(value)}
                    placeholder={
                      emitSubjectType === "user-email"
                        ? "user@example.com"
                        : emitSubjectType === "user-id"
                          ? "user-uuid"
                          : emitSubjectType === "user-username"
                            ? "username"
                            : "org-alias"
                    }
                  />
                </FormGroup>
                <FormGroup
                  label={t("ssfEmitPayload")}
                  fieldId="ssfEmitPayload"
                  labelIcon={
                    <HelpItem
                      helpText={t("ssfEmitPayloadHelp")}
                      fieldLabelId="ssfEmitPayload"
                    />
                  }
                >
                  <CodeEditor
                    data-testid="ssfEmitPayload"
                    aria-label={t("ssfEmitPayload")}
                    language="json"
                    height={220}
                    value={emitPayload}
                    onChange={(value) => {
                      setEmitPayload(value);
                      // Live validation: substitute placeholders first
                      // (so unquoted __now__ becomes a valid numeric
                      // literal) then JSON.parse. Blank payload resolves
                      // to {} at submit time and is therefore valid.
                      const trimmed = value.trim();
                      if (trimmed === "") {
                        setEmitPayloadParseError(null);
                        return;
                      }
                      try {
                        JSON.parse(substitutePayloadPlaceholders(value));
                        setEmitPayloadParseError(null);
                      } catch (error) {
                        setEmitPayloadParseError(
                          t("ssfEmitPayloadInvalidJson", {
                            error: String(error),
                          }),
                        );
                      }
                    }}
                  />
                  {emitPayloadParseError && (
                    <Text
                      className="pf-v5-u-mt-sm pf-v5-u-color-status-danger--100"
                      data-testid="ssfEmitPayloadParseError"
                    >
                      {emitPayloadParseError}
                    </Text>
                  )}
                </FormGroup>
                <ActionGroup>
                  <Button
                    variant="primary"
                    onClick={handleEmitEvent}
                    isDisabled={
                      pendingActionLoading || emitPayloadParseError !== null
                    }
                    data-testid="ssfEmitEvent"
                  >
                    {t("ssfEmitEvent")}
                  </Button>
                </ActionGroup>
                {emitError && (
                  <Text
                    className="pf-v5-u-mt-md pf-v5-u-color-status-danger--100"
                    data-testid="ssfEmitError"
                  >
                    {emitError}
                  </Text>
                )}
                {emitResult && (
                  <Text
                    className="pf-v5-u-mt-md pf-v5-u-color-status-success--100"
                    data-testid="ssfEmitResult"
                  >
                    {t("ssfEmitResult", {
                      status: emitResult.status,
                      jti: emitResult.jti,
                    })}
                  </Text>
                )}
              </FormAccess>
            </CardBody>
          </Card>
        </>
      )}
    </PageSection>
  );
};
