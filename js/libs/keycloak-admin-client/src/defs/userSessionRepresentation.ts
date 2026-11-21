export default interface UserSessionRepresentation {
  id?: string;
  clients?: Record<string, string>;
  ipAddress?: string;
  lastAccess?: number;
  start?: number;
  userId?: string;
  username?: string;
  transientUser?: boolean;
  os?: string;
  osVersion?: string;
  browser?: string;
  device?: string;
  mobile?: boolean;
}
