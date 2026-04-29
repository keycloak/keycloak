import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  FormGroup,
  InputGroup,
  InputGroupItem,
  Text,
  TextContent,
  TextInput,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../../admin-client";
import CodeEditor from "../../../components/form/CodeEditor";
import { FormAccess } from "../../../components/form/FormAccess";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { addTrailingSlash } from "../../../util";
import { getAuthorizationHeaders } from "../../../utils/getAuthorizationHeaders";
import useFormatDate from "../../../utils/useFormatDate";

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

export type EventsTabProps = {
  client: ClientRepresentation;
  availableSupportedEvents: string[];
  nativelyEmittedEvents: string[];
};

export const EventsTab = ({
  client,
  availableSupportedEvents,
  nativelyEmittedEvents,
}: EventsTabProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const formatDate = useFormatDate();

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

  return (
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
                isDisabled={pendingActionLoading || !pendingLookupJti?.trim()}
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
                            new Date(pendingLookupResult.nextAttemptAt * 1000),
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
                <option value="">{t("ssfEmitEventTypeSelectPrompt")}</option>
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
            <FormGroup label={t("ssfSubjectType")} fieldId="ssfEmitSubjectType">
              <select
                id="ssfEmitSubjectType"
                data-testid="ssfEmitSubjectType"
                value={emitSubjectType}
                onChange={(e) =>
                  setEmitSubjectType(e.target.value as typeof emitSubjectType)
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
  );
};
