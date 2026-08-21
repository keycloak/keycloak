export const shouldPruneExpandedFlows = (
  previousHoverLevel: number | null,
  hoveredLevel?: number,
): boolean =>
  previousHoverLevel !== null &&
  hoveredLevel !== undefined &&
  hoveredLevel < previousHoverLevel;

export const nextHoverLevel = (
  previousHoverLevel: number | null,
  hoveredLevel?: number,
): number | null =>
  hoveredLevel === undefined ? previousHoverLevel : hoveredLevel;
