export default interface EventHookTestResultRepresentation {
  success: boolean;
  retryable?: boolean;
  statusCode?: string;
  details?: string;
  durationMs?: number;
}
