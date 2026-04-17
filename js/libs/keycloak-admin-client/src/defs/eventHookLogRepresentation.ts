export default interface EventHookLogRepresentation {
    id?: string;
    executionId?: string;
    batchExecution?: boolean;
    messageId?: string;
    targetId?: string;
    sourceType?: string;
    sourceEventId?: string;
    sourceEventName?: string;
    status?: string;
    messageStatus?: string;
    attemptNumber?: number;
    statusCode?: string;
    durationMs?: number;
    details?: string;
    createdAt?: number;
}