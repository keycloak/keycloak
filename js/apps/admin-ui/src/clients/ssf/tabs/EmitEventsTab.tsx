import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
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
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { useAdminClient } from "../../../admin-client";
import CodeEditor from "../../../components/form/CodeEditor";
import { FormAccess } from "../../../components/form/FormAccess";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { addTrailingSlash } from "../../../util";
import { getAuthorizationHeaders } from "../../../utils/getAuthorizationHeaders";
import { toSsfClientTab } from "../../routes/ClientSsfTab";

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

export type EmitEventsTabProps = {
  client: ClientRepresentation;
  availableSupportedEvents: string[];
  nativelyEmittedEvents: string[];
};

export const EmitEventsTab = ({
  client,
  availableSupportedEvents,
  nativelyEmittedEvents,
}: EmitEventsTabProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();

  const [emitEventType, setEmitEventType] = useState("");
  const [emitEventTypeOpen, setEmitEventTypeOpen] = useState(false);
  const [emitEventTypeFilter, setEmitEventTypeFilter] = useState("");
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
  const [emitLoading, setEmitLoading] = useState(false);

  /**
   * Emits a synthetic SSF event for this receiver via the admin-emit
   * endpoint. Builds an iss_sub subject from the realm issuer and the
   * provided userId so the admin only has to supply the user; the event
   * payload is pasted verbatim as JSON. On success, the result panel
   * shows a "Look up this event" link that navigates to the Event
   * Search tab with the returned jti pre-filled.
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
    setEmitLoading(true);
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
    } catch (error) {
      setEmitError(String(error));
      setEmitResult(null);
    } finally {
      setEmitLoading(false);
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
            <KeycloakSelect
              toggleId="ssfEmitEventType"
              data-testid="ssfEmitEventType"
              variant={SelectVariant.typeahead}
              typeAheadAriaLabel={t("ssfEmitEventType")}
              placeholderText={t("ssfEmitEventTypeSelectPrompt")}
              onToggle={setEmitEventTypeOpen}
              isOpen={emitEventTypeOpen}
              selections={emitEventType || undefined}
              onSelect={(value) => {
                setEmitEventType(value.toString());
                setEmitEventTypeOpen(false);
                setEmitEventTypeFilter("");
              }}
              onClear={() => {
                setEmitEventType("");
                setEmitEventTypeFilter("");
              }}
              onFilter={setEmitEventTypeFilter}
            >
              {availableSupportedEvents
                .filter((eventType) =>
                  eventType
                    .toLowerCase()
                    .includes(emitEventTypeFilter.toLowerCase()),
                )
                .map((eventType) => (
                  <SelectOption key={eventType} value={eventType}>
                    {eventType}
                    {nativelyEmittedEvents.includes(eventType) && (
                      <Label color="blue" isCompact className="pf-v5-u-ml-sm">
                        {t("ssfNativelyEmittedBadge")}
                      </Label>
                    )}
                  </SelectOption>
                ))}
            </KeycloakSelect>
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
              <option value="org-alias">{t("ssfSubjectType.orgAlias")}</option>
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
              isDisabled={emitLoading || emitPayloadParseError !== null}
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
