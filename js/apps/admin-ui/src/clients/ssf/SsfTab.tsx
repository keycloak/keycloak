import { fetchWithError } from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { useFormContext } from "react-hook-form";

import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { addTrailingSlash, convertAttributeNameToForm } from "../../util";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import type { FormFields, SaveOptions } from "../ClientDetails";
import type { SsfClientTab } from "../routes/ClientSsfTab";
import { EmitEventsTab } from "./tabs/EmitEventsTab";
import { EventSearchTab } from "./tabs/EventSearchTab";
import { ReceiverTab } from "./tabs/ReceiverTab";
import { StreamTab, type SsfClientStream } from "./tabs/StreamTab";
import { SubjectsTab } from "./tabs/SubjectsTab";

const FALLBACK_DEFAULT_SUPPORTED_EVENTS =
  "CaepCredentialChange,CaepSessionRevoked";

const isPollDeliveryMethod = (method: string | undefined): boolean =>
  method === "urn:ietf:rfc:8936" ||
  method === "https://schemas.openid.net/secevent/risc/delivery-method/poll";

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
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();

  const { watch, setValue } = useFormContext<FormFields>();

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
  const [streamFetchKey, setStreamFetchKey] = useState(0);
  const [configFetchKey, setConfigFetchKey] = useState(0);

  const refresh = () => {
    setStreamFetchKey((k) => k + 1);
    setConfigFetchKey((k) => k + 1);
  };

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
      "ssf.delivery",
      "ssf.pushEndpointConnectTimeoutMillis",
      "ssf.pushEndpointSocketTimeoutMillis",
      "ssf.maxEventAgeSeconds",
      "ssf.inactivityTimeoutSeconds",
      "ssf.subjectRemovalGraceSeconds",
    ]);

  return (
    <PageSection variant="light" className="pf-v5-u-py-0">
      {activeTab === "receiver" && (
        <ReceiverTab
          client={client}
          clientStream={clientStream}
          defaultSupportedEvents={defaultSupportedEvents}
          availableSupportedEvents={availableSupportedEvents}
          nativelyEmittedEvents={nativelyEmittedEvents}
          defaultUserSubjectFormat={defaultUserSubjectFormat}
          save={save}
          reset={reset}
        />
      )}
      {activeTab === "stream" && (
        <StreamTab
          client={client}
          clientStream={clientStream}
          setClientStream={setClientStream}
          defaultSupportedEvents={defaultSupportedEvents}
          nativelyEmittedEvents={nativelyEmittedEvents}
          defaultPushConnectTimeoutMillis={defaultPushConnectTimeoutMillis}
          defaultPushSocketTimeoutMillis={defaultPushSocketTimeoutMillis}
          save={save}
          reset={reset}
          refresh={refresh}
        />
      )}
      {activeTab === "subjects" && <SubjectsTab client={client} />}
      {activeTab === "event-search" && <EventSearchTab client={client} />}
      {activeTab === "emit-events" && (
        <EmitEventsTab
          client={client}
          clientStream={clientStream}
          nativelyEmittedEvents={nativelyEmittedEvents}
        />
      )}
    </PageSection>
  );
};
