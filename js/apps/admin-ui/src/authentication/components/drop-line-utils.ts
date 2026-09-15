import type { DropInfo } from "../execution-model";

export const shouldShowDropLineAfter = (
  dropInfo: DropInfo | undefined,
  visualIndex: number | undefined,
): boolean => {
  if (!dropInfo || dropInfo.mode !== "reorder-after") {
    return false;
  }

  if (visualIndex === undefined) {
    return false;
  }

  return dropInfo.insertIndex === visualIndex + 1;
};
