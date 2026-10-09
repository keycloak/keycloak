import { describe, expect, it } from "vitest";
import {
  hasMovedToDeeperLevel,
  isWithinAutoExpandedContext,
  keepExpandedIdsForHoveredRow,
  keepExpandedIdsForPrune,
  nextHoverLevel,
  shouldPruneExpandedFlows,
  shouldPruneOnHoverMove,
} from "../drag-hover";

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

  describe("hasMovedToDeeperLevel", () => {
    it("returns true when hovering deeper", () => {
      expect(hasMovedToDeeperLevel(0, 1)).toBe(true);
    });

    it("returns false when staying at the same level", () => {
      expect(hasMovedToDeeperLevel(1, 1)).toBe(false);
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

  describe("isWithinAutoExpandedContext", () => {
    const ancestorPathIds = (id: string) => {
      if (id === "child") {
        return new Set(["parent"]);
      }
      return new Set<string>();
    };

    it("returns false when hovering outside an auto-expanded subflow", () => {
      expect(
        isWithinAutoExpandedContext(
          "sibling",
          null,
          new Set(["parent"]),
          ancestorPathIds,
        ),
      ).toBe(false);
    });

    it("returns true when hovering a pending expand target", () => {
      expect(
        isWithinAutoExpandedContext(
          "parent",
          "parent",
          new Set(),
          ancestorPathIds,
          {
            pendingExpandId: "parent",
          },
        ),
      ).toBe(true);
    });

    it("returns true when hovering the drop-into zone of a subflow", () => {
      expect(
        isWithinAutoExpandedContext(
          "parent",
          "parent",
          new Set(),
          ancestorPathIds,
          {
            isDropIntoTarget: true,
          },
        ),
      ).toBe(true);
    });

    it("returns true when hovering a subflow edge zone that is auto-expanded", () => {
      expect(
        isWithinAutoExpandedContext(
          "parent",
          "parent",
          new Set(["parent"]),
          ancestorPathIds,
        ),
      ).toBe(true);
    });

    it("returns true when hovering inside an auto-expanded subflow", () => {
      expect(
        isWithinAutoExpandedContext(
          "child",
          "parent",
          new Set(["parent"]),
          ancestorPathIds,
        ),
      ).toBe(true);
    });

    it("returns true when entering a nested subflow under an auto-expanded parent", () => {
      expect(
        isWithinAutoExpandedContext(
          "child",
          "child",
          new Set(["parent"]),
          ancestorPathIds,
        ),
      ).toBe(true);
    });
  });

  describe("shouldPruneOnHoverMove", () => {
    it("returns true when moving up", () => {
      expect(
        shouldPruneOnHoverMove(true, false, false, new Set(["parent"]), true),
      ).toBe(true);
    });

    it("returns true when leaving an auto-expanded subflow at the same level", () => {
      expect(
        shouldPruneOnHoverMove(false, false, false, new Set(["parent"]), false),
      ).toBe(true);
    });

    it("returns false while moving deeper into nested subflows", () => {
      expect(
        shouldPruneOnHoverMove(false, true, false, new Set(["parent"]), false),
      ).toBe(false);
    });

    it("returns false while still inside an auto-expanded subflow", () => {
      expect(
        shouldPruneOnHoverMove(false, false, false, new Set(["parent"]), true),
      ).toBe(false);
    });

    it("returns false when no subflows were auto-expanded", () => {
      expect(
        shouldPruneOnHoverMove(false, false, false, new Set(), false),
      ).toBe(false);
    });
  });

  describe("keepExpandedIdsForHoveredRow", () => {
    const ancestorPathIds = (id: string) =>
      id === "child" ? new Set(["parent"]) : new Set<string>();

    it("keeps only ancestors when reordering around a subflow", () => {
      expect(
        keepExpandedIdsForHoveredRow("parent", true, false, ancestorPathIds),
      ).toEqual(new Set());
    });

    it("keeps the hovered subflow when dropping into it", () => {
      expect(
        keepExpandedIdsForHoveredRow("parent", true, true, ancestorPathIds),
      ).toEqual(new Set(["parent"]));
    });

    it("keeps ancestor subflows for nested rows", () => {
      expect(
        keepExpandedIdsForHoveredRow("child", false, false, ancestorPathIds),
      ).toEqual(new Set(["parent"]));
    });
  });

  describe("keepExpandedIdsForPrune", () => {
    const ancestorPathIds = (id: string) => {
      if (id === "nested-parent") {
        return new Set(["forms"]);
      }
      return new Set<string>();
    };

    it("keeps dragged flow ancestors when leaving the flow context", () => {
      expect(
        keepExpandedIdsForPrune(
          new Set(),
          true,
          "nested-parent",
          ancestorPathIds,
        ),
      ).toEqual(new Set(["forms"]));
    });

    it("does not keep the dragged flow context itself expanded", () => {
      const keepExpanded = keepExpandedIdsForPrune(
        new Set(),
        true,
        "nested-parent",
        ancestorPathIds,
      );

      expect(keepExpanded.has("nested-parent")).toBe(false);
    });

    it("returns hovered keep-expanded ids when not leaving the flow context", () => {
      expect(
        keepExpandedIdsForPrune(
          new Set(["target"]),
          false,
          "nested-parent",
          ancestorPathIds,
        ),
      ).toEqual(new Set(["target"]));
    });
  });
});
