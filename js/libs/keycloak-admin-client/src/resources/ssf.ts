import type {
  SsfAdminSubjectRequest,
  SsfAdminSubjectResponse,
  SsfClientStreamRepresentation,
  SsfConfigRepresentation,
  SsfDeleteEventsResponse,
  SsfEmitEventRequest,
  SsfEmitEventResponse,
  SsfEventRepresentation,
  SsfEventStatsRepresentation,
  SsfStreamConfigInputRepresentation,
  SsfStreamStatusRepresentation,
} from "../defs/ssfRepresentation.js";
import type { KeycloakAdminClient } from "../client.js";
import Resource from "./resource.js";

export interface SsfClientQuery {
  clientId: string;
}

export interface SsfPendingEventQuery extends SsfClientQuery {
  jti: string;
}

export interface SsfDeleteEventsQuery {
  status: string;
  olderThan?: string;
}

export class Ssf extends Resource<{ realm?: string }> {
  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/ssf",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }

  public getConfig = this.makeRequest<void, SsfConfigRepresentation>({
    method: "GET",
    path: "/config",
  });

  public getEventStats = this.makeRequest<void, SsfEventStatsRepresentation>({
    method: "GET",
    path: "/events/stats",
  });

  public deleteEvents = this.makeRequest<
    SsfDeleteEventsQuery,
    SsfDeleteEventsResponse
  >({
    method: "DELETE",
    path: "/events",
    queryParamKeys: ["status", "olderThan"],
  });

  public deleteQueuedEvents = this.makeRequest<void, SsfDeleteEventsResponse>({
    method: "DELETE",
    path: "/events/queued",
  });

  public findClientStream = this.makeRequest<
    SsfClientQuery,
    SsfClientStreamRepresentation | null
  >({
    method: "GET",
    path: "/clients/{clientId}/stream",
    urlParamKeys: ["clientId"],
    catchNotFound: true,
  });

  public createClientStream = this.makeUpdateRequest<
    SsfClientQuery,
    SsfStreamConfigInputRepresentation,
    SsfClientStreamRepresentation
  >({
    method: "POST",
    path: "/clients/{clientId}/stream",
    urlParamKeys: ["clientId"],
  });

  public deleteClientStream = this.makeRequest<SsfClientQuery, void>({
    method: "DELETE",
    path: "/clients/{clientId}/stream",
    urlParamKeys: ["clientId"],
  });

  public verifyClientStream = this.makeRequest<SsfClientQuery, void>({
    method: "POST",
    path: "/clients/{clientId}/stream/verify",
    urlParamKeys: ["clientId"],
  });

  public updateClientStreamStatus = this.makeUpdateRequest<
    SsfClientQuery,
    SsfStreamStatusRepresentation,
    SsfStreamStatusRepresentation
  >({
    method: "POST",
    path: "/clients/{clientId}/stream/status",
    urlParamKeys: ["clientId"],
  });

  public addSubject = this.makeUpdateRequest<
    SsfClientQuery,
    SsfAdminSubjectRequest,
    SsfAdminSubjectResponse
  >({
    method: "POST",
    path: "/clients/{clientId}/subjects/add",
    urlParamKeys: ["clientId"],
  });

  public removeSubject = this.makeUpdateRequest<
    SsfClientQuery,
    SsfAdminSubjectRequest,
    SsfAdminSubjectResponse
  >({
    method: "POST",
    path: "/clients/{clientId}/subjects/remove",
    urlParamKeys: ["clientId"],
  });

  public ignoreSubject = this.makeUpdateRequest<
    SsfClientQuery,
    SsfAdminSubjectRequest,
    SsfAdminSubjectResponse
  >({
    method: "POST",
    path: "/clients/{clientId}/subjects/ignore",
    urlParamKeys: ["clientId"],
  });

  public checkSubject = this.makeUpdateRequest<
    SsfClientQuery,
    SsfAdminSubjectRequest,
    SsfAdminSubjectResponse | null
  >({
    method: "POST",
    path: "/clients/{clientId}/subjects/check",
    urlParamKeys: ["clientId"],
    catchNotFound: true,
  });

  public emitEvent = this.makeUpdateRequest<
    SsfClientQuery,
    SsfEmitEventRequest,
    SsfEmitEventResponse
  >({
    method: "POST",
    path: "/clients/{clientId}/events/emit",
    urlParamKeys: ["clientId"],
  });

  public findPendingEvent = this.makeRequest<
    SsfPendingEventQuery,
    SsfEventRepresentation | null
  >({
    method: "GET",
    path: "/clients/{clientId}/pending-events/{jti}",
    urlParamKeys: ["clientId", "jti"],
    catchNotFound: true,
  });

  public getClientEventStats = this.makeRequest<
    SsfClientQuery,
    SsfEventStatsRepresentation
  >({
    method: "GET",
    path: "/clients/{clientId}/events/stats",
    urlParamKeys: ["clientId"],
  });

  public deleteClientEvents = this.makeRequest<
    SsfClientQuery & SsfDeleteEventsQuery,
    SsfDeleteEventsResponse
  >({
    method: "DELETE",
    path: "/clients/{clientId}/events",
    urlParamKeys: ["clientId"],
    queryParamKeys: ["status", "olderThan"],
  });

  public deleteClientQueuedEvents = this.makeRequest<
    SsfClientQuery,
    SsfDeleteEventsResponse
  >({
    method: "DELETE",
    path: "/clients/{clientId}/events/queued",
    urlParamKeys: ["clientId"],
  });
}
