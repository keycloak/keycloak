import { describe, expect, it } from "vitest";
import { nextHoverLevel, shouldPruneExpandedFlows } from "../drag-hover";

describe("drag-hover helpers", () => {
  describe("shouldPruneExpandedFlows", () => {
    it("returns false when previous hover level is unknown", () => {
      expect(shouldPruneExpandedFlows(null, 1)).toBe(false);
    });

    it("returns false when hovering deeper", () => {
      expect(shouldPruneExpandedFlows(1, 2)).toBe(false);
    });

    it("returns false when staying at same level", () => {
      expect(shouldPruneExpandedFlows(1, 1)).toBe(false);
    });

    it("returns true when moving up", () => {
      expect(shouldPruneExpandedFlows(2, 1)).toBe(true);
    });

    it("returns false when no row is hovered", () => {
      expect(shouldPruneExpandedFlows(1, undefined)).toBe(false);
    });
  });

  describe("nextHoverLevel", () => {
    it("keeps last level when no row is hovered", () => {
      expect(nextHoverLevel(2, undefined)).toBe(2);
    });

    it("updates tracked level when a row is hovered", () => {
      expect(nextHoverLevel(2, 1)).toBe(1);
    });
  });
});
