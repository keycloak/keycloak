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
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  FormGroup,
  InputGroup,
  InputGroupItem,
  PageSection,
  SelectOption,
  Text,
  TextInput,
} from "@patternfly/react-core";
import { CopyToClipboardButton } from "../components/copy-to-clipboard-button/CopyToClipboardButton";
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

const FALLBACK_DEFAULT_SUPPORTED_EVENTS =
  "CaepCredentialChange,CaepSessionRevoked";

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
};

export const SsfTab = ({ save, client }: SsfTabProps) => {
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
  const [streamFetchKey, setStreamFetchKey] = useState(0);
  const [configFetchKey, setConfigFetchKey] = useState(0);

  // Admin-side create-stream form state — used by the empty-state form
  // when no stream is registered yet. Kept as plain useState because the
  // surrounding react-hook-form context is bound to the client
  // representation's attributes, not to stream-create parameters.
  const [createStreamAudience, setCreateStreamAudience] = useState("");
  const [createStreamEndpointUrl, setCreateStreamEndpointUrl] = useState("");
  const [createStreamAuthHeader, setCreateStreamAuthHeader] = useState("");
  const [createStreamEvents, setCreateStreamEvents] = useState<string[]>([]);
  const [createStreamDescription, setCreateStreamDescription] = useState("");
  const [createStreamEventsOpen, setCreateStreamEventsOpen] = useState(false);
  const [createStreamSubmitting, setCreateStreamSubmitting] = useState(false);
  const [createStreamFormOpen, setCreateStreamFormOpen] = useState(false);

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
        )}admin/realms/${realm}/ssf/clients/${client.id}/stream/verify`,
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
  };

  const submitCreateStream = async () => {
    if (!client.id) {
      return;
    }
    if (!createStreamEndpointUrl.trim()) {
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
      const body: Record<string, unknown> = {
        delivery: {
          method: "urn:ietf:rfc:8935",
          endpoint_url: createStreamEndpointUrl.trim(),
        },
      };
      if (createStreamAuthHeader.trim()) {
        (body.delivery as Record<string, unknown>).authorization_header =
          createStreamAuthHeader.trim();
      }
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
        )}admin/realms/${realm}/ssf/clients/${client.id}/stream`,
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
          )}admin/realms/${realm}/ssf/clients/${client.id}/stream`,
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
        )}admin/realms/${realm}/ssf/clients/${client.id}/stream`,
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
      "ssf.streamAudience",
      "ssf.supportedEvents",
      "ssf.profile",
      "ssf.userSubjectFormat",
      "ssf.verificationTrigger",
      "ssf.verificationDelayMillis",
      "ssf.status",
      "ssf.delivery",
      "ssf.pushEndpointConnectTimeoutMillis",
      "ssf.pushEndpointSocketTimeoutMillis",
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
      <ScrollForm
        label={t("jumpToSection")}
        sections={[
          {
            title: t("ssfReceiver"),
            panel: (
              <>
                <Text className="pf-v5-u-pb-lg">{t("ssfReceiverHelp")}</Text>
                <FormAccess
                  role="manage-clients"
                  fineGrainedAccess={client.access?.configure}
                  isHorizontal
                >
                  <TextControl
                    name={convertAttributeNameToForm<FormFields>(
                      "attributes.ssf.streamAudience",
                    )}
                    label={t("ssfStreamAudience")}
                    labelIcon={t("ssfStreamAudienceHelp")}
                  />
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
                              </SelectOption>
                            ))}
                          </KeycloakSelect>
                        );
                      }}
                    />
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
                  {saveActionGroup("ssfReceiver")}
                </FormAccess>
              </>
            ),
          },
          {
            title: t("ssfStream"),
            panel: (
              <>
                <Text className="pf-v5-u-pb-lg">{t("ssfStreamHelp")}</Text>
                <ActionGroup className="pf-v5-u-pb-md">
                  <Button
                    variant="secondary"
                    onClick={refresh}
                    data-testid="ssfRefresh"
                  >
                    {t("refresh")}
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
                            onChange={(_e, value) =>
                              setCreateStreamAudience(value)
                            }
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
                          <TextInput
                            id="ssfCreateStreamDeliveryMethod"
                            data-testid="ssfCreateStreamDeliveryMethod"
                            readOnlyVariant="default"
                            value={t("ssfDelivery.PUSH")}
                          />
                        </FormGroup>
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
                            typeAheadAriaLabel={t(
                              "ssfCreateStreamEventsRequested",
                            )}
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
                          clientStream.audience &&
                          clientStream.audience.length > 0
                            ? clientStream.audience.join(", ")
                            : ""
                        }
                      />
                    </FormGroup>
                    {clientStream.delivery?.endpoint_url && (
                      <FormGroup
                        label={t("ssfStreamPushEndpointUrl")}
                        fieldId="ssfStreamPushEndpointUrl"
                        labelIcon={
                          <HelpItem
                            helpText={t("ssfStreamPushEndpointUrlHelp")}
                            fieldLabelId="ssfStreamPushEndpointUrl"
                          />
                        }
                      >
                        <TextInput
                          id="ssfStreamPushEndpointUrl"
                          data-testid="ssfStreamPushEndpointUrl"
                          readOnlyVariant="default"
                          value={clientStream.delivery.endpoint_url}
                        />
                      </FormGroup>
                    )}
                    {clientStream.delivery?.endpoint_url && (
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
                        // PULL delivery is not yet supported
                        // { key: "PULL", value: t("ssfDelivery.PULL") },
                      ]}
                    />
                    {(ssfDelivery === "PUSH" || !ssfDelivery) && (
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
              </>
            ),
          },
        ]}
        borders
      />
    </PageSection>
  );
};
