export default interface EventHookMessageRepresentation {
  id?: string;
  targetId?: string;
  executionId?: string;
  sourceType?: string;
  sourceEventId?: string;
  sourceEventName?: string;
  userId?: string;
  resourcePath?: string;
  executionBatch?: boolean;
  status?: string;
  attemptCount?: number;
  nextAttemptAt?: number;
  createdAt?: number;
  updatedAt?: number;
  lastError?: string;
  test?: boolean;
  payload?: unknown;
}
