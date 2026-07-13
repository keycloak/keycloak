export default interface EventHookLogRepresentation {
  id?: string;
  executionId?: string;
  status?: string;
  messageStatus?: string;
  attemptNumber?: number;
  statusCode?: string;
  durationMs?: number;
  details?: string;
  createdAt?: number;
}
