import { describe, expect, it } from "vitest";
import { containsOrphan, ExpandableExecution } from "./execution-model";

const execution = (
  overrides: Partial<ExpandableExecution> = {},
): ExpandableExecution => ({ isCollapsed: false, ...overrides });

describe("containsOrphan", () => {
  it("returns false when no provider is unavailable", () => {
    expect(containsOrphan(execution({ executionList: [execution()] }))).toBe(
      false,
    );
  });

  it("detects an orphan on the dragged execution itself", () => {
    expect(containsOrphan(execution({ providerUnavailable: true }))).toBe(true);
  });

  it("detects an orphan nested in a sub flow", () => {
    const subFlow = execution({
      executionList: [execution({ providerUnavailable: true })],
    });
    expect(containsOrphan(execution({ executionList: [subFlow] }))).toBe(true);
  });
});
