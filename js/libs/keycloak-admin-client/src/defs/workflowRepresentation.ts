export default interface WorkflowRepresentation {
  id?: string;
  name?: string;
  enabled?: boolean;
  steps?: Step[];
}

export interface Step {
  name?: string;
  priority?: number;
  uses?: string;
  "scheduled-at"?: number;
  status?: "COMPLETED" | "PENDING";
}
