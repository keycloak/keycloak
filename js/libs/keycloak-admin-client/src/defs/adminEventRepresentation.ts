import type AuthDetailsRepresentation from "./authDetailsRepresentation.js";

export default interface AdminEventRepresentation {
  authDetails?: AuthDetailsRepresentation;
  error?: string;
  operationType?: string;
  realmId?: string;
  representation?: string;
  resourcePath?: string;
  resourceType?: string;
  time?: number;
  details?: Record<string, any>;
}
