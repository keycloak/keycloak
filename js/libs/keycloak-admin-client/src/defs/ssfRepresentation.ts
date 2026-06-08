export interface SsfStreamDeliveryConfig {
  method?: string;
  endpoint_url?: string;
  authorization_header?: string;
  additional_parameters?: Record<string, unknown>;
}

export interface SsfClientStreamRepresentation {
  streamId?: string;
  description?: string;
  status?: string;
  statusReason?: string;
  audience?: string[];
  delivery?: SsfStreamDeliveryConfig;
  eventsSupported?: string[];
  eventsRequested?: string[];
  eventsDelivered?: string[];
  createdAt?: number;
  updatedAt?: number;
  lastVerifiedAt?: number;
}

export interface SsfStreamConfigInputRepresentation {
  description?: string;
  events_requested?: string[];
  delivery?: SsfStreamDeliveryConfig;
  aud?: string[];
  iss?: string;
  format?: string;
}

export interface SsfStreamStatusRepresentation {
  stream_id?: string;
  status?: string;
  reason?: string;
}

export interface SsfConfigRepresentation {
  defaultSupportedEvents?: string[];
  availableSupportedEvents?: string[];
  nativelyEmittedEvents?: string[];
  defaultPushEndpointConnectTimeoutMillis?: number;
  defaultPushEndpointSocketTimeoutMillis?: number;
  defaultUserSubjectFormat?: string;
}

export interface SsfAdminSubjectRequest {
  type?: string;
  value?: string;
}

export interface SsfAdminSubjectResponse {
  status?: string;
  entity_type?: string;
  entity_id?: string;
  source_org_alias?: string;
}

export interface SsfEmitEventRequest {
  eventType?: string;
  sub_id?: Record<string, unknown>;
  subjectType?: string;
  subjectValue?: string;
  event?: Record<string, unknown>;
}

export interface SsfEmitEventResponse {
  status?: string;
  jti?: string;
  message?: string;
}

export interface SsfEventRepresentation {
  jti?: string;
  eventType?: string;
  deliveryMethod?: string;
  status?: string;
  attempts?: number;
  createdAt?: number;
  nextAttemptAt?: number;
  deliveredAt?: number;
  lastError?: string;
  streamId?: string;
  decodedSet?: Record<string, unknown>;
  userId?: string;
}

export interface SsfDeleteEventsResponse {
  deleted?: number;
}

export interface SsfEventStatsStatusEntry {
  count?: number;
  oldestCreatedAt?: string;
}

export interface SsfEventStatsRepresentation {
  statuses?: Record<string, SsfEventStatsStatusEntry>;
}
