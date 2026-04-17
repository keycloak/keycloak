export default interface EventHookMessageRepresentation {
    id?: string;
    targetId?: string;
    sourceType?: string;
    sourceEventId?: string;
    sourceEventName?: string;
    status?: string;
    attemptCount?: number;
    nextAttemptAt?: number;
    createdAt?: number;
    updatedAt?: number;
    claimOwner?: string;
    claimedAt?: number;
    lastError?: string;
    payload?: unknown;
}