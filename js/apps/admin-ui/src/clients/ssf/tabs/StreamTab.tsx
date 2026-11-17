import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { SsfStreamConfigInputRepresentation } from "@keycloak/keycloak-admin-client";
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
import { convertAttributeNameToForm } from "../../../util";
import useFormatDate from "../../../utils/useFormatDate";
import type { FormFields } from "../../ClientDetails";
import { isPollDeliveryMethod } from "../utils";
import { CreateStreamForm } from "./CreateStreamForm";

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
      const body: Partial<SsfStreamConfigInputRepresentation> = {};
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

      await adminClient.ssf.updateClientStream(
        { clientId: client.clientId! },
        body,
      );
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
      await adminClient.ssf.updateClientStreamStatus(
        { clientId: client.clientId! },
        {
          stream_id: clientStream.streamId,
          status: targetStatus,
        },
      );
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
      await adminClient.ssf.verifyClientStream({
        clientId: client.clientId!,
      });
      addAlert(t("ssfVerifyStreamSuccess"), AlertVariant.success);
      // Refetch the stream endpoint so the Last verified field in the UI
      // picks up the new timestamp the backend just stamped.
      refresh();
    } catch (error) {
      addError("ssfVerifyStreamError", error);
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
        await adminClient.ssf.deleteClientStream({
          clientId: client.clientId!,
        });
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
                  onPrimaryAction={() => setCreateStreamFormOpen(true)}
                />
              )}
              {createStreamFormOpen && (
                <CreateStreamForm
                  client={client}
                  receiverSupportedEvents={receiverSupportedEvents}
                  nativelyEmittedEvents={nativelyEmittedEvents}
                  onCancel={() => setCreateStreamFormOpen(false)}
                  onSuccess={() => {
                    setCreateStreamFormOpen(false);
                    refresh();
                  }}
                />
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
