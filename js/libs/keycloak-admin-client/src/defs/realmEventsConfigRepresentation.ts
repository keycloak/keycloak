/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/#_realmeventsconfigrepresentation
 */

export interface RealmEventsConfigRepresentation {
  eventsEnabled?: boolean;
  eventsExpiration?: number;
  eventsListeners?: string[];
  enabledEventTypes?: string[];
  adminEventsEnabled?: boolean;
  adminEventsDetailsEnabled?: boolean;
}
