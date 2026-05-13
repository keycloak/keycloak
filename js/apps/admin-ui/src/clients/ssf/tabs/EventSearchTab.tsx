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
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";

import { useAdminClient } from "../../../admin-client";
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

export type EventSearchTabProps = {
  client: ClientRepresentation;
};

export const EventSearchTab = ({ client }: EventSearchTabProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const formatDate = useFormatDate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [pendingLookupJti, setPendingLookupJti] = useState(
    () => searchParams.get("jti") ?? "",
  );
  const [pendingLookupResult, setPendingLookupResult] =
    useState<SsfPendingEvent | null>(null);
  const [pendingLookupError, setPendingLookupError] = useState<string | null>(
    null,
  );
  const [pendingActionLoading, setPendingActionLoading] = useState(false);

  /**
   * Looks up a single outbox row by jti scoped to this receiver — calls
   * GET /admin/realms/{realm}/ssf/clients/{uuid}/pending-events/{jti}.
   * 404 is rendered as a "not found" message rather than an alert; any
   * other failure surfaces both inline and via addError.
   */
  const handlePendingLookup = async (jti?: string) => {
    const lookupJti = (jti ?? pendingLookupJti).trim();
    if (!client.id || !lookupJti) {
      return;
    }
    // Don't wipe the previous result/error here — that causes the
    // result block to unmount during the request, which collapses the
    // container height and makes the layout jump on repeated clicks.
    // We update them in place once the new fetch resolves.
    setPendingActionLoading(true);
    try {
      const response = await fetch(
        `${addTrailingSlash(adminClient.baseUrl)}admin/realms/${realm}/ssf/clients/${client.clientId}/pending-events/${encodeURIComponent(lookupJti)}`,
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

  // Auto-run the lookup once on mount when the URL carries ?jti=... —
  // the emit success panel uses this as a one-click handoff into the
  // search tab. After consuming the param, drop it from the URL so a
  // page refresh doesn't re-trigger the lookup with a now-stale value.
  useEffect(() => {
    const jti = searchParams.get("jti");
    if (!jti) return;
    void handlePendingLookup(jti);
    const next = new URLSearchParams(searchParams);
    next.delete("jti");
    setSearchParams(next, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
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
          onSubmit={(e) => {
            e.preventDefault();
            void handlePendingLookup();
          }}
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
              type="button"
              variant="primary"
              onClick={() => handlePendingLookup()}
              isDisabled={pendingActionLoading || !pendingLookupJti.trim()}
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
                    ? formatDate(new Date(pendingLookupResult.createdAt * 1000))
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
                      {JSON.stringify(pendingLookupResult.decodedSet, null, 2)}
                    </pre>
                  </>
                )}
              </TextContent>
            </FormGroup>
          )}
        </FormAccess>
      </CardBody>
    </Card>
  );
};
