export type EventHookTargetStatus = "NOT_USED" | "OK" | "HAS_PROBLEMS";

export default interface EventHookTargetRepresentation {
  id?: string;
  name?: string;
  type?: string;
  enabled?: boolean;
  createdAt?: number;
  updatedAt?: number;
  autoDisabled?: boolean;
  autoDisabledUntil?: number;
  autoDisabledReason?: string;
  settings?: Record<string, unknown>;
  displayInfo?: string;
  status?: EventHookTargetStatus;
}
