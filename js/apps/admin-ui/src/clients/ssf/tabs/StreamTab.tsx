import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  ListEmptyState,
  PasswordInput,
  SelectVariant,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
  InputGroup,
  InputGroupItem,
  Label,
  SelectOption,
  Text,
  TextContent,
  TextInput,
} from "@patternfly/react-core";
import {
  CheckCircleIcon,
  InfoCircleIcon,
  PauseCircleIcon,
  SyncAltIcon,
  TimesCircleIcon,
} from "@patternfly/react-icons";
import { useEffect, useRef, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../../admin-client";
import { useConfirmDialog } from "../../../components/confirm-dialog/ConfirmDialog";
import { CopyToClipboardButton } from "../../../components/copy-to-clipboard-button/CopyToClipboardButton";
import { FormAccess } from "../../../components/form/FormAccess";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { addTrailingSlash, convertAttributeNameToForm } from "../../../util";
import { getAuthorizationHeaders } from "../../../utils/getAuthorizationHeaders";
import useFormatDate from "../../../utils/useFormatDate";
import type { FormFields } from "../../ClientDetails";

const DELIVERY_METHOD_PUSH_URI = "urn:ietf:rfc:8935";
const DELIVERY_METHOD_POLL_URI = "urn:ietf:rfc:8936";

const isPollDeliveryMethod = (method: string | undefined): boolean =>
  method === DELIVERY_METHOD_POLL_URI ||
  method === "https://schemas.openid.net/secevent/risc/delivery-method/poll";

/**
 * Order-insensitive equality for the events_requested /
 * events_delivered string lists. Used to drive the "dirty" indicator
 * on the admin-side stream edit form so toggling a multi-select item
 * back to its original state doesn't keep the Save button enabled.
 */
const arraysEqualUnordered = (a: string[], b: string[]): boolean => {
  if (a === b) return true;
  if (a.length !== b.length) return false;
  const aSorted = [...a].sort();
  const bSorted = [...b].sort();
  for (let i = 0; i < aSorted.length; i++) {
    if (aSorted[i] !== bSorted[i]) return false;
  }
  return true;
};

/**
 * Best-effort URL validation for the receiver's push endpoint. Uses
 * the URL constructor (no regex juggling) and additionally requires
 * an http/https protocol — the SSF dispatcher only knows how to push
 * over HTTP, so accepting e.g. ftp:// or javascript: would just
 * dead-letter the queued events on first push attempt. An empty
 * string is treated as "not yet typed" and not validated here so the
 * field's existing isRequired check owns that case.
 */
const isValidPushEndpointUrl = (value: string): boolean => {
  const trimmed = value.trim();
  if (trimmed === "") return true;
  try {
    const parsed = new URL(trimmed);
    return parsed.protocol === "http:" || parsed.protocol === "https:";
  } catch {
    return false;
  }
};

/**
 * Builds a human-readable message from a failed SSF endpoint response.
 * The transmitter returns OAuth2-style error bodies
 * ({ error, error_description }), so surface error_description
 * rather than the raw JSON. Falls back to the raw body, then to the HTTP status line.
 */
const ssfErrorMessage = async (response: Response): Promise<string> => {
  const text = await response.text();
  if (text) {
    try {
      const body = JSON.parse(text) as {
        error_description?: string;
        error?: string;
      };
      const message = body.error_description || body.error;
      if (message) return message;
    } catch {
      // Body wasn't JSON — fall through to the raw text.
    }
    return text;
  }
  return `${response.status} ${response.statusText || "Request failed"}`;
};

type SsfClientStreamDelivery = {
  method?: string;
  endpoint_url?: string;
  authorization_header?: string;
};

export type SsfStreamManagedBy = "RECEIVER" | "KEYCLOAK";

export type SsfClientStream = {
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
  managedBy?: SsfStreamManagedBy;
};

export type StreamTabProps = {
  client: ClientRepresentation;
  clientStream: SsfClientStream | null;
  setClientStream: (stream: SsfClientStream | null) => void;
  defaultSupportedEvents: string;
  nativelyEmittedEvents: string[];
  refresh: () => void;
};

export const StreamTab = ({
  client,
  clientStream,
  setClientStream,
  defaultSupportedEvents,
  nativelyEmittedEvents,
  refresh,
}: StreamTabProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { control, setValue } = useFormContext<FormFields>();
  const { addAlert, addError } = useAlerts();
  const formatDate = useFormatDate();

  // The Events Requested dropdown on the create-stream form should
  // surface only events the receiver has marked as Supported, not the
  // full transmitter-wide registry. Watch the form value so the list
  // reflects unsaved Receiver-tab edits without a round-trip; fall
  // back to the realm default when the receiver hasn't customised it.
  const ssfSupportedEvents = useWatch({
    control,
    name: convertAttributeNameToForm<FormFields>(
      "attributes.ssf.supportedEvents",
    ) as never,
  }) as string | undefined;
  const receiverSupportedEvents = (ssfSupportedEvents ?? defaultSupportedEvents)
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
    // Storage order is whatever the operator picked when configuring the
    // receiver — sort here so the create-stream dropdown is alphabetical
    // and matches the deterministic order the backend now returns for
    // availableSupportedEvents.
    .sort((a, b) => a.localeCompare(b));

  // Admin-side create-stream form state — used by the empty-state form
  // when no stream is registered yet. Kept as plain useState because the
  // surrounding react-hook-form context is bound to the client
  // representation's attributes, not to stream-create parameters.
  const [createStreamMethod, setCreateStreamMethod] = useState<"PUSH" | "POLL">(
    "PUSH",
  );
  const [createStreamEndpointUrl, setCreateStreamEndpointUrl] = useState("");
  const [createStreamAuthHeader, setCreateStreamAuthHeader] = useState("");
  const [createStreamEvents, setCreateStreamEvents] = useState<string[]>([]);
  const [createStreamEventsFilter, setCreateStreamEventsFilter] = useState("");
  const [createStreamProfile, setCreateStreamProfile] = useState<
    "SSF_1_0" | "SSE_CAEP"
  >(client.attributes?.["ssf.profile"] as "SSF_1_0" | "SSE_CAEP");
  const [createStreamDescription, setCreateStreamDescription] = useState("");
  const [createStreamEventsOpen, setCreateStreamEventsOpen] = useState(false);
  const [createStreamSubmitting, setCreateStreamSubmitting] = useState(false);
  const [createStreamFormOpen, setCreateStreamFormOpen] = useState(false);

  const [statusActionLoading, setStatusActionLoading] = useState(false);

  // ---- admin-side stream edit state ---------------------------------
  // Tracks pending edits on the registered stream's admin-editable
  // fields. Initialised from the loaded stream and reset when the
  // stream refreshes so a refresh-without-save discards local edits.
  const [editStreamDescription, setEditStreamDescription] = useState("");
  const [editStreamEventsRequested, setEditStreamEventsRequested] = useState<
    string[]
  >([]);
  const [editStreamEventsDelivered, setEditStreamEventsDelivered] = useState<
    string[]
  >([]);
  const [editEventsRequestedOpen, setEditEventsRequestedOpen] = useState(false);
  const [editEventsDeliveredOpen, setEditEventsDeliveredOpen] = useState(false);
  const [streamEditSubmitting, setStreamEditSubmitting] = useState(false);

  // Description / events_requested / events_delivered are receiver-
  // owned for streams the receiver registered itself (via the
  // standard SSF /streams endpoint). We surface them as read-only on
  // the admin Stream tab in that mode so an admin save can't silently
  // overwrite the receiver's contract; the server PATCH endpoint
  // stays open for operators who need to override out-of-band, but
  // the UI keeps that out of the happy path.
  const canEditStream = clientStream?.managedBy === "KEYCLOAK";

  const streamEditDirty =
    canEditStream &&
    ((clientStream.description ?? "") !== editStreamDescription ||
      !arraysEqualUnordered(
        clientStream.eventsRequested ?? [],
        editStreamEventsRequested,
      ) ||
      !arraysEqualUnordered(
        clientStream.eventsDelivered ?? [],
        editStreamEventsDelivered,
      ));

  // Tracks the streamId the edit fields were last seeded from, so we can
  // tell "a different stream just loaded" (always re-seed) apart from "the
  // admin is editing the stream that's already loaded" (don't clobber).
  const seededStreamIdRef = useRef<string | undefined>(undefined);

  // Re-seed edit state when the loaded stream changes — but never clobber
  // unsaved admin edits to the stream that's already loaded.
  //
  // The dirty guard alone deadlocks on first load of a KEYCLOAK-managed
  // stream: the edit fields start empty, so the moment the stream loads with
  // a non-empty description / events the state looks "dirty" against that
  // empty seed and the guard refuses to seed it — leaving the fields blank
  // forever. Gating the guard on "same stream as last seeded" breaks the
  // cycle: a freshly created / newly loaded stream has no local edits to
  // protect, so it always seeds.
  useEffect(() => {
    const loadedStreamId = clientStream?.streamId;
    const sameStream = seededStreamIdRef.current === loadedStreamId;
    if (sameStream && streamEditDirty) {
      return;
    }
    seededStreamIdRef.current = loadedStreamId;
    setEditStreamDescription(clientStream?.description ?? "");
    setEditStreamEventsRequested(clientStream?.eventsRequested ?? []);
    setEditStreamEventsDelivered(clientStream?.eventsDelivered ?? []);
  }, [
    clientStream?.streamId,
    clientStream?.description,
    clientStream?.eventsRequested,
    clientStream?.eventsDelivered,
    streamEditDirty,
  ]);

  const submitStreamEdit = async () => {
    if (!client.clientId || !clientStream || !streamEditDirty) {
      return;
    }
    setStreamEditSubmitting(true);
    try {
      const body: Record<string, unknown> = {};
      if ((clientStream.description ?? "") !== editStreamDescription) {
        body.description = editStreamDescription;
      }
      if (
        !arraysEqualUnordered(
          clientStream.eventsRequested ?? [],
          editStreamEventsRequested,
        )
      ) {
        body.events_requested = editStreamEventsRequested;
      }
      if (
        !arraysEqualUnordered(
          clientStream.eventsDelivered ?? [],
          editStreamEventsDelivered,
        )
      ) {
        body.events_delivered = editStreamEventsDelivered;
      }

      const response = await fetch(
        `${addTrailingSlash(
          adminClient.baseUrl,
        )}admin/realms/${realm}/ssf/clients/${client.clientId}/stream`,
        {
          method: "PATCH",
          headers: {
            ...getAuthorizationHeaders(await adminClient.getAccessToken()),
            "Content-Type": "application/json",
          },
          body: JSON.stringify(body),
        },
      );
      if (!response.ok) {
        throw new Error(await ssfErrorMessage(response));
      }
      addAlert(t("ssfStreamUpdateSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("ssfStreamUpdateError", error);
    } finally {
      setStreamEditSubmitting(false);
    }
  };

  const discardStreamEdit = () => {
    setEditStreamDescription(clientStream?.description ?? "");
    setEditStreamEventsRequested(clientStream?.eventsRequested ?? []);
    setEditStreamEventsDelivered(clientStream?.eventsDelivered ?? []);
  };

  /**
   * Drives the admin stream-status endpoint
   * (POST /admin/realms/{realm}/ssf/clients/{clientId}/stream/status).
   * The backend funnels through StreamService.updateStreamStatus, so
   * this triggers the spec-mandated stream-updated SET dispatch and
   * the outbox HELD ↔ PENDING alignment that a generic client-save
   * doesn't.
   */
  const triggerStreamStatusUpdate = async (
    targetStatus: "enabled" | "paused" | "disabled",
  ) => {
    if (!client.id || !clientStream?.streamId) {
      return;
    }
    setStatusActionLoading(true);
    try {
      const response = await fetch(
        `${addTrailingSlash(
          adminClient.baseUrl,
        )}admin/realms/${realm}/ssf/clients/${client.clientId}/stream/status`,
        {
          method: "POST",
          headers: {
            ...getAuthorizationHeaders(await adminClient.getAccessToken()),
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            stream_id: clientStream.streamId,
            status: targetStatus,
          }),
        },
      );
      if (!response.ok) {
        throw new Error(await ssfErrorMessage(response));
      }
      // Keep the form-bound status field in sync immediately so a
      // subsequent generic Save doesn't clobber the just-applied
      // backend status with a stale form value (the parent's
      // useFetch refresh below is async and races the click).
      setValue(
        convertAttributeNameToForm<FormFields>("attributes.ssf.stream.status"),
        targetStatus,
        { shouldDirty: false },
      );
      addAlert(t("ssfStreamStatusUpdateSuccess"), AlertVariant.success);
      // Refresh so the read-only status indicator + lastVerifiedAt /
      // updatedAt in the UI reflect the new state.
      refresh();
    } catch (error) {
      addError("ssfStreamStatusUpdateError", error);
    } finally {
      setStatusActionLoading(false);
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
    setCreateStreamEndpointUrl("");
    setCreateStreamAuthHeader("");
    setCreateStreamEvents([]);
    setCreateStreamDescription("");
    setCreateStreamMethod("PUSH");
  };

  // Prefill events_requested with the receiver's Supported Events on
  // open. The vast majority of streams want every event the receiver
  // declared support for; the operator can still narrow the selection
  // before submitting. Seeded fresh each open so unsaved Receiver-tab
  // edits flow through.
  const openCreateStreamForm = () => {
    setCreateStreamEvents([...receiverSupportedEvents]);
    setCreateStreamFormOpen(true);
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
    if (
      createStreamMethod === "PUSH" &&
      !isValidPushEndpointUrl(createStreamEndpointUrl)
    ) {
      addError(
        "ssfCreateStreamError",
        new Error(t("ssfCreateStreamEndpointUrlInvalid")),
      );
      return;
    }
    setCreateStreamSubmitting(true);
    try {
      // SSF profile is a per-receiver attribute; the create-stream
      // backend reads it from the client (resolveReceiverProfile).
      // If the operator picked a different profile in the create
      // form, persist the receiver attribute first so the subsequent
      // create-stream call sees the new value. Two API calls instead
      // of one, but the side effect is OK — profile is a one-time
      // decision per receiver, and the create form is precisely
      // where you'd be making it the first time.
      const savedProfile = client.attributes?.["ssf.profile"] || "SSF_1_0";
      if (createStreamProfile !== savedProfile) {
        await adminClient.clients.update(
          { id: client.id! },
          {
            ...client,
            attributes: {
              ...(client.attributes ?? {}),
              "ssf.profile": createStreamProfile,
            },
          },
        );
      }

      // Event aliases stored in local state are resolved back to their
      // canonical URIs by the transmitter at create time — the admin UI
      // shows/stores aliases because that's what the admin picks from
      // the receiver's supported-events list, and the backend accepts
      // either form.
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
        throw new Error(await ssfErrorMessage(response));
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

  return (
    <>
      <DeleteStreamConfirm />
      <Card isFlat className="pf-v5-u-mt-md">
        {clientStream && (
          <>
            <CardHeader>
              <CardTitle>{t("ssfStream")}</CardTitle>
            </CardHeader>

            <CardBody>
              <TextContent>
                <Text>{t("ssfStreamHelp")}</Text>
              </TextContent>
            </CardBody>
          </>
        )}
        <CardBody>
          {clientStream && (
            <ActionGroup className="pf-v5-u-pb-md">
              <Button variant="link" onClick={refresh} data-testid="ssfRefresh">
                <SyncAltIcon /> {t("refresh")}
              </Button>
            </ActionGroup>
          )}
          {!clientStream ? (
            <>
              {!createStreamFormOpen && (
                <ListEmptyState
                  message={t("ssfStreamNotRegistered")}
                  instructions={t("ssfStreamNotRegisteredHelp")}
                  primaryActionText={t("ssfCreateStream")}
                  onPrimaryAction={openCreateStreamForm}
                />
              )}
              {createStreamFormOpen && (
                <FormAccess
                  role="manage-clients"
                  fineGrainedAccess={client.access?.configure}
                  isHorizontal
                >
                  <FormGroup
                    label={t("ssfProfile")}
                    fieldId="ssfCreateStreamProfile"
                    labelIcon={
                      <HelpItem
                        helpText={t("ssfCreateStreamProfileHelp")}
                        fieldLabelId="ssfCreateStreamProfile"
                      />
                    }
                  >
                    <select
                      id="ssfCreateStreamProfile"
                      data-testid="ssfCreateStreamProfile"
                      className="pf-v5-c-form-control"
                      value={createStreamProfile}
                      onChange={(e) =>
                        setCreateStreamProfile(
                          e.target.value === "SSE_CAEP"
                            ? "SSE_CAEP"
                            : "SSF_1_0",
                        )
                      }
                    >
                      <option value="SSF_1_0">{t("ssfProfile.SSF_1_0")}</option>
                      <option value="SSE_CAEP">
                        {t("ssfProfile.SSE_CAEP")}
                      </option>
                    </select>
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
                          validated={
                            isValidPushEndpointUrl(createStreamEndpointUrl)
                              ? "default"
                              : "error"
                          }
                          onChange={(_e, value) =>
                            setCreateStreamEndpointUrl(value)
                          }
                        />
                        {!isValidPushEndpointUrl(createStreamEndpointUrl) && (
                          <FormHelperText>
                            <HelperText>
                              <HelperTextItem
                                variant="error"
                                data-testid="ssfCreateStreamEndpointUrlError"
                              >
                                {t("ssfCreateStreamEndpointUrlInvalid")}
                              </HelperTextItem>
                            </HelperText>
                          </FormHelperText>
                        )}
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
                        setCreateStreamEventsFilter("");
                      }}
                      onClear={() => {
                        setCreateStreamEvents([]);
                        setCreateStreamEventsFilter("");
                      }}
                      onFilter={setCreateStreamEventsFilter}
                    >
                      {receiverSupportedEvents
                        .filter((event) =>
                          event
                            .toLowerCase()
                            .includes(createStreamEventsFilter.toLowerCase()),
                        )
                        .map((event) => (
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
                        (createStreamMethod === "PUSH" &&
                          (!createStreamEndpointUrl.trim() ||
                            !isValidPushEndpointUrl(createStreamEndpointUrl)))
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
              <FormGroup
                label={t("ssfStreamManagedBy")}
                fieldId="ssfStreamManagedBy"
                labelIcon={
                  <HelpItem
                    helpText={t("ssfStreamManagedByHelp")}
                    fieldLabelId="ssfStreamManagedBy"
                  />
                }
              >
                <Label
                  data-testid="ssfStreamManagedBy"
                  color={
                    clientStream.managedBy === "KEYCLOAK" ? "blue" : "grey"
                  }
                >
                  {clientStream.managedBy === "KEYCLOAK"
                    ? t("ssfStreamManagedByKeycloak")
                    : t("ssfStreamManagedByReceiver")}
                </Label>
              </FormGroup>
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
                  readOnlyVariant={canEditStream ? undefined : "default"}
                  value={editStreamDescription}
                  onChange={
                    canEditStream
                      ? (_event, value) => setEditStreamDescription(value)
                      : undefined
                  }
                />
              </FormGroup>

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
                    value={formatDate(new Date(clientStream.createdAt * 1000))}
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
                    value={formatDate(new Date(clientStream.updatedAt * 1000))}
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
                      ? formatDate(new Date(clientStream.lastVerifiedAt * 1000))
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
              <FormGroup
                label={t("ssfStreamStatus")}
                fieldId="ssfStreamStatusIndicator"
                labelIcon={
                  <HelpItem
                    helpText={t("ssfStreamStatusHelp")}
                    fieldLabelId="ssfStreamStatus"
                  />
                }
              >
                {clientStream.status === "enabled" && (
                  <Label
                    color="green"
                    icon={<CheckCircleIcon />}
                    data-testid="ssfStreamStatusIndicator.enabled"
                  >
                    {t("ssfStreamStatus.enabled")}
                  </Label>
                )}
                {clientStream.status === "paused" && (
                  <Label
                    color="orange"
                    icon={<PauseCircleIcon />}
                    data-testid="ssfStreamStatusIndicator.paused"
                  >
                    {t("ssfStreamStatus.paused")}
                  </Label>
                )}
                {clientStream.status === "disabled" && (
                  <Label
                    color="red"
                    icon={<TimesCircleIcon />}
                    data-testid="ssfStreamStatusIndicator.disabled"
                  >
                    {t("ssfStreamStatus.disabled")}
                  </Label>
                )}
                {!clientStream.status && (
                  <Label
                    color="blue"
                    icon={<InfoCircleIcon />}
                    data-testid="ssfStreamStatusIndicator.unknown"
                  >
                    {t("ssfStreamStatus.enabled")}
                  </Label>
                )}
              </FormGroup>
              <ActionGroup>
                {clientStream.status !== "enabled" && (
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => triggerStreamStatusUpdate("enabled")}
                    isDisabled={statusActionLoading}
                    data-testid="ssfStreamStatusEnable"
                  >
                    {t("ssfStreamStatusEnable")}
                  </Button>
                )}
                {clientStream.status !== "paused" && (
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => triggerStreamStatusUpdate("paused")}
                    isDisabled={statusActionLoading}
                    data-testid="ssfStreamStatusPause"
                  >
                    {t("ssfStreamStatusPause")}
                  </Button>
                )}
                {clientStream.status !== "disabled" && (
                  <Button
                    type="button"
                    variant="danger"
                    onClick={() => triggerStreamStatusUpdate("disabled")}
                    isDisabled={statusActionLoading}
                    data-testid="ssfStreamStatusDisable"
                  >
                    {t("ssfStreamStatusDisable")}
                  </Button>
                )}
              </ActionGroup>
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
                <KeycloakSelect
                  toggleId="ssfEventsRequested"
                  data-testid="ssfEventsRequested"
                  variant={SelectVariant.typeaheadMulti}
                  isDisabled={!canEditStream}
                  chipGroupProps={{
                    numChips: 5,
                    expandedText: t("hide"),
                    collapsedText: t("showRemaining"),
                  }}
                  typeAheadAriaLabel={t("ssfEventsRequested")}
                  onToggle={(open) => setEditEventsRequestedOpen(open)}
                  isOpen={editEventsRequestedOpen}
                  selections={editStreamEventsRequested}
                  onSelect={(value) => {
                    const event = value as string;
                    setEditStreamEventsRequested((prev) =>
                      prev.includes(event)
                        ? prev.filter((e) => e !== event)
                        : [...prev, event],
                    );
                  }}
                  onClear={
                    canEditStream
                      ? () => setEditStreamEventsRequested([])
                      : undefined
                  }
                >
                  {receiverSupportedEvents.map((event) => (
                    <SelectOption key={event} value={event}>
                      {event}
                    </SelectOption>
                  ))}
                </KeycloakSelect>
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
                <KeycloakSelect
                  toggleId="ssfEventsDelivered"
                  data-testid="ssfEventsDelivered"
                  variant={SelectVariant.typeaheadMulti}
                  isDisabled={!canEditStream}
                  chipGroupProps={{
                    numChips: 5,
                    expandedText: t("hide"),
                    collapsedText: t("showRemaining"),
                  }}
                  typeAheadAriaLabel={t("ssfEventsDelivered")}
                  onToggle={(open) => setEditEventsDeliveredOpen(open)}
                  isOpen={editEventsDeliveredOpen}
                  selections={editStreamEventsDelivered}
                  onSelect={(value) => {
                    const event = value as string;
                    setEditStreamEventsDelivered((prev) =>
                      prev.includes(event)
                        ? prev.filter((e) => e !== event)
                        : [...prev, event],
                    );
                  }}
                  onClear={
                    canEditStream
                      ? () => setEditStreamEventsDelivered([])
                      : undefined
                  }
                >
                  {receiverSupportedEvents.map((event) => (
                    <SelectOption key={event} value={event}>
                      {event}
                    </SelectOption>
                  ))}
                </KeycloakSelect>
              </FormGroup>
              <ActionGroup>
                {canEditStream && (
                  <>
                    <Button
                      type="button"
                      variant="primary"
                      onClick={() => submitStreamEdit()}
                      isDisabled={!streamEditDirty || streamEditSubmitting}
                      isLoading={streamEditSubmitting}
                      data-testid="ssfStreamSave"
                    >
                      {t("save")}
                    </Button>
                    <Button
                      type="button"
                      variant="link"
                      onClick={() => discardStreamEdit()}
                      isDisabled={!streamEditDirty || streamEditSubmitting}
                      data-testid="ssfStreamRevert"
                    >
                      {t("revert")}
                    </Button>
                  </>
                )}
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
    </>
  );
};
