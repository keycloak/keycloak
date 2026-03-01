export default interface UserSessionRepresentation {
  id?: string;
  clients?: Record<string, string>;
  ipAddress?: string;
  lastAccess?: number;
  start?: number;
  userId?: string;
  username?: string;
  transientUser?: boolean;
}
