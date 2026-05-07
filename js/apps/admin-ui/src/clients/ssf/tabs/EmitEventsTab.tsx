import { fetchWithError } from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Alert,
  Button,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  FormGroup,
  Label,
  SelectOption,
  Text,
  TextContent,
  TextInput,
} from "@patternfly/react-core";
import debouncePromise from "p-debounce";
import { useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { useAdminClient } from "../../../admin-client";
import CodeEditor from "../../../components/form/CodeEditor";
import { FormAccess } from "../../../components/form/FormAccess";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { addTrailingSlash } from "../../../util";
import { getAuthorizationHeaders } from "../../../utils/getAuthorizationHeaders";
import { toSsfClientTab } from "../../routes/ClientSsfTab";
import type { SsfClientStream } from "./StreamTab";

type SsfEmitResult = {
  status: string;
  jti?: string;
};

type EmitSubjectType = "user-email" | "user-id" | "user-username" | "org-alias";

type EmitEventsFormValues = {
  emitEventType: string;
  emitSubjectType: EmitSubjectType;
  emitSubjectValue: string;
  emitPayload: string;
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

export type EmitEventsTabProps = {
  client: ClientRepresentation;
  clientStream: SsfClientStream | null;
  nativelyEmittedEvents: string[];
};

export const EmitEventsTab = ({
  client,
  clientStream,
  nativelyEmittedEvents,
}: EmitEventsTabProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();

  // Constrain the emit dropdown to events_delivered — the
  // authoritative set the dispatcher will actually push to this
  // receiver. It's already the intersection of receiver-side
  // events_requested and what the realm registry supports, so
  // listing anything outside it would just produce filter-dropped
  // emissions.
  const emittableEvents = clientStream?.eventsDelivered ?? [];

  const {
    control,
    handleSubmit,
    watch,
    formState: { isSubmitting },
  } = useForm<EmitEventsFormValues>({
    defaultValues: {
      emitEventType: "",
      emitSubjectType: "user-email",
      emitSubjectValue: "",
      // Default to an empty JSON object with a newline between the
      // braces so the caret lands on an indented line ready for typing.
      emitPayload: "{\n  \n}",
    },
    mode: "onSubmit",
  });

  // UI-only state for the typeahead/single selects and live feedback
  // panels — these aren't form values, so they stay in useState.
  const [emitEventTypeOpen, setEmitEventTypeOpen] = useState(false);
  const [emitEventTypeFilter, setEmitEventTypeFilter] = useState("");
  const [emitSubjectTypeOpen, setEmitSubjectTypeOpen] = useState(false);
  // Live JSON parse-error message — updated on every change so the
  // operator sees the problem as they type rather than only after
  // hitting Emit. The submit handler also uses this to short-circuit
  // before the HTTP call.
  const [emitPayloadParseError, setEmitPayloadParseError] = useState<
    string | null
  >(null);
  const [emitResult, setEmitResult] = useState<SsfEmitResult | null>(null);
  const [emitError, setEmitError] = useState<string | null>(null);

  // Watch the subject-type so the TextInput placeholder switches
  // from "user@example.com" to "user-uuid" / "username" / "org-alias"
  // without re-rendering the rest of the form.
  const emitSubjectType = watch("emitSubjectType");

  // Debounce the live JSON parse so a fast typist doesn't pay
  // JSON.parse on every keystroke against a large payload. 250ms
  // matches the cadence at which the validation error feels
  // "instant" without flickering as the user types mid-token.
  const debouncedValidatePayload = useMemo(
    () =>
      debouncePromise((value: string) => {
        try {
          JSON.parse(substitutePayloadPlaceholders(value));
          setEmitPayloadParseError(null);
        } catch (error) {
          setEmitPayloadParseError(
            t("ssfEmitPayloadInvalidJson", { error: String(error) }),
          );
        }
      }, 250),
    [t],
  );

  /**
   * Emits a synthetic SSF event for this receiver via the admin-emit
   * endpoint. Builds an iss_sub subject from the realm issuer and the
   * provided userId so the admin only has to supply the user; the event
   * payload is pasted verbatim as JSON. On success, the result panel
   * shows a "Look up this event" link that navigates to the Event
   * Search tab with the returned jti pre-filled.
   */
  const onSubmit = async (values: EmitEventsFormValues) => {
    if (!client.id) {
      return;
    }
    let parsedPayload: unknown;
    try {
      parsedPayload =
        values.emitPayload.trim() === ""
          ? {}
          : JSON.parse(substitutePayloadPlaceholders(values.emitPayload));
    } catch (error) {
      const message = t("ssfEmitPayloadInvalidJson", { error: String(error) });
      setEmitError(message);
      setEmitPayloadParseError(message);
      return;
    }
    setEmitError(null);
    try {
      // Send the (subjectType, subjectValue) shorthand — the admin
      // emit backend resolves it through the same path the
      // /subjects:add endpoint uses, then for user subjects builds
      // the sub_id via the receiver-format-aware mapper so the
      // resulting SET matches the shape native events for this
      // receiver would have. Org subjects produce a complex tenant-
      // only subject for org-scoped routing.
      const response = await fetchWithError(
        `${addTrailingSlash(adminClient.baseUrl)}admin/realms/${realm}/ssf/clients/${client.clientId}/events/emit`,
        {
          method: "POST",
          headers: {
            ...getAuthorizationHeaders(await adminClient.getAccessToken()),
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            eventType: values.emitEventType,
            subjectType: values.emitSubjectType,
            subjectValue: values.emitSubjectValue.trim(),
            event: parsedPayload,
          }),
        },
      );
      const result = (await response.json()) as SsfEmitResult;
      setEmitResult(result);
    } catch (error) {
      setEmitError(error instanceof Error ? error.message : String(error));
      setEmitResult(null);
    }
  };

  const eventSearchPath = (jti: string) => {
    // The route's :clientId path param is the client's internal UUID
    // (the parent client-details routes use it that way), not the OAuth
    // client_id. Use client.id; client.clientId would 404.
    const target = toSsfClientTab({
      realm,
      clientId: client.id!,
      tab: "event-search",
    });
    return {
      ...target,
      search: `?jti=${encodeURIComponent(jti)}`,
    };
  };

  return (
    <Card isFlat className="pf-v5-u-mt-md">
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
          onSubmit={handleSubmit(onSubmit)}
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
            <Controller
              name="emitEventType"
              control={control}
              rules={{ required: t("ssfEmitEventTypeRequired") }}
              render={({ field }) => (
                <KeycloakSelect
                  toggleId="ssfEmitEventType"
                  data-testid="ssfEmitEventType"
                  variant={SelectVariant.typeahead}
                  typeAheadAriaLabel={t("ssfEmitEventType")}
                  placeholderText={t("ssfEmitEventTypeSelectPrompt")}
                  onToggle={setEmitEventTypeOpen}
                  isOpen={emitEventTypeOpen}
                  selections={field.value || undefined}
                  onSelect={(value) => {
                    field.onChange(value.toString());
                    setEmitEventTypeOpen(false);
                    setEmitEventTypeFilter("");
                  }}
                  onClear={() => {
                    field.onChange("");
                    setEmitEventTypeFilter("");
                  }}
                  onFilter={setEmitEventTypeFilter}
                >
                  {emittableEvents
                    .filter((eventType) =>
                      eventType
                        .toLowerCase()
                        .includes(emitEventTypeFilter.toLowerCase()),
                    )
                    .map((eventType) => (
                      <SelectOption key={eventType} value={eventType}>
                        {eventType}
                        {nativelyEmittedEvents.includes(eventType) && (
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
              )}
            />
          </FormGroup>
          <FormGroup label={t("ssfSubjectType")} fieldId="ssfEmitSubjectType">
            <Controller
              name="emitSubjectType"
              control={control}
              render={({ field }) => (
                <KeycloakSelect
                  toggleId="ssfEmitSubjectType"
                  data-testid="ssfEmitSubjectType"
                  variant={SelectVariant.single}
                  onToggle={setEmitSubjectTypeOpen}
                  isOpen={emitSubjectTypeOpen}
                  selections={field.value}
                  onSelect={(value) => {
                    field.onChange(value as EmitSubjectType);
                    setEmitSubjectTypeOpen(false);
                  }}
                >
                  <SelectOption value="user-email">
                    {t("ssfSubjectType.userEmail")}
                  </SelectOption>
                  <SelectOption value="user-id">
                    {t("ssfSubjectType.userId")}
                  </SelectOption>
                  <SelectOption value="user-username">
                    {t("ssfSubjectType.userUsername")}
                  </SelectOption>
                  <SelectOption value="org-alias">
                    {t("ssfSubjectType.orgAlias")}
                  </SelectOption>
                </KeycloakSelect>
              )}
            />
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
            <Controller
              name="emitSubjectValue"
              control={control}
              rules={{
                validate: (value) =>
                  value.trim() !== "" || t("ssfEmitSubjectValueRequired"),
              }}
              render={({ field }) => (
                <TextInput
                  id="ssfEmitSubjectValue"
                  data-testid="ssfEmitSubjectValue"
                  value={field.value}
                  onChange={(_e, value) => field.onChange(value)}
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
              )}
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
            <Controller
              name="emitPayload"
              control={control}
              render={({ field }) => (
                <CodeEditor
                  data-testid="ssfEmitPayload"
                  aria-label={t("ssfEmitPayload")}
                  language="json"
                  height={220}
                  value={field.value}
                  onChange={(value) => {
                    field.onChange(value);
                    // Live validation: substitute placeholders first
                    // (so unquoted __now__ becomes a valid numeric
                    // literal) then JSON.parse. Blank payload resolves
                    // to {} at submit time and is therefore valid —
                    // clear the error synchronously so the UI feedback
                    // is immediate when the user empties the field.
                    if (value.trim() === "") {
                      setEmitPayloadParseError(null);
                      return;
                    }
                    void debouncedValidatePayload(value);
                  }}
                />
              )}
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
              type="submit"
              variant="primary"
              isLoading={isSubmitting}
              isDisabled={isSubmitting || emitPayloadParseError !== null}
              data-testid="ssfEmitEvent"
            >
              {t("ssfEmitEvent")}
            </Button>
          </ActionGroup>
          {emitError && (
            <Alert
              variant="danger"
              isInline
              className="pf-v5-u-mt-md"
              data-testid="ssfEmitError"
              title={emitError}
            />
          )}
          {emitResult && (
            <TextContent className="pf-v5-u-mt-md" data-testid="ssfEmitResult">
              <Text className="pf-v5-u-color-status-success--100">
                {t("ssfEmitResult", {
                  status: emitResult.status,
                  jti: emitResult.jti,
                })}
              </Text>
              {emitResult.jti && (
                <Text>
                  <Link
                    to={eventSearchPath(emitResult.jti)}
                    data-testid="ssfEmitResultLookup"
                  >
                    {t("ssfEmitResultLookupLink")}
                  </Link>
                </Text>
              )}
            </TextContent>
          )}
        </FormAccess>
      </CardBody>
    </Card>
  );
};
