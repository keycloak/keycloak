import { describe, expect, it } from "vitest";
import type { DropInfo } from "../execution-model";
import { shouldShowDropLineAfter } from "../components/drop-line-utils";

describe("drop line helpers", () => {
  const baseDropInfo: DropInfo = {
    targetId: "2",
    mode: "reorder-after",
    targetLevel: 0,
    targetParentId: null,
    insertIndex: 6,
  };

  it("shows the after line on the visual row before insertIndex", () => {
    expect(shouldShowDropLineAfter(baseDropInfo, 5)).toBe(true);
    expect(shouldShowDropLineAfter(baseDropInfo, 3)).toBe(false);
  });

  it("shows the after line for adjacent reorder-after drops", () => {
    const adjacentDrop: DropInfo = {
      ...baseDropInfo,
      insertIndex: 4,
    };

    expect(shouldShowDropLineAfter(adjacentDrop, 3)).toBe(true);
  });

  it("does not show an after line for other drop modes", () => {
    const reorderBeforeDrop: DropInfo = {
      ...baseDropInfo,
      mode: "reorder-before",
      insertIndex: 3,
    };

    expect(shouldShowDropLineAfter(reorderBeforeDrop, 2)).toBe(false);
    expect(shouldShowDropLineAfter(undefined, 2)).toBe(false);
    expect(shouldShowDropLineAfter(baseDropInfo, undefined)).toBe(false);
  });
});
