export const shouldPruneExpandedFlows = (
  previousHoverLevel: number | null,
  hoveredLevel?: number,
): boolean =>
  previousHoverLevel !== null &&
  hoveredLevel !== undefined &&
  hoveredLevel < previousHoverLevel;

export const hasMovedToDeeperLevel = (
  previousHoverLevel: number | null,
  hoveredLevel?: number,
): boolean =>
  previousHoverLevel !== null &&
  hoveredLevel !== undefined &&
  hoveredLevel > previousHoverLevel;

export const nextHoverLevel = (
  previousHoverLevel: number | null,
  hoveredLevel?: number,
): number | null =>
  hoveredLevel === undefined ? previousHoverLevel : hoveredLevel;

type AutoExpandedContextOptions = {
  pendingExpandId?: string | null;
  isDropIntoTarget?: boolean;
};

export const isWithinAutoExpandedContext = (
  hoveredExecutionId: string | null,
  flowContextId: string | null,
  autoExpandedIds: ReadonlySet<string>,
  ancestorPathIds: (id: string) => Set<string>,
  options: AutoExpandedContextOptions = {},
): boolean => {
  if (options.pendingExpandId === hoveredExecutionId) {
    return true;
  }

  if (options.isDropIntoTarget) {
    return true;
  }

  if (!flowContextId) {
    return autoExpandedIds.size === 0;
  }

  const ancestors = ancestorPathIds(flowContextId);
  for (const id of autoExpandedIds) {
    if (id === flowContextId || ancestors.has(id)) {
      return true;
    }
  }

  return false;
};

export const shouldPruneOnHoverMove = (
  movedUp: boolean,
  movedDeeper: boolean,
  pruneForDifferentFlow: boolean,
  autoExpandedIds: ReadonlySet<string>,
  isWithinAutoExpandedContext: boolean,
): boolean => {
  if (movedUp || pruneForDifferentFlow) {
    return true;
  }

  if (movedDeeper || autoExpandedIds.size === 0) {
    return false;
  }

  return !isWithinAutoExpandedContext;
};

export const keepExpandedIdsForHoveredRow = (
  hoveredExecutionId: string,
  isSubflow: boolean,
  isDropIntoTarget: boolean,
  ancestorPathIds: (id: string) => Set<string>,
): Set<string> => {
  const keepExpanded = ancestorPathIds(hoveredExecutionId);
  if (isSubflow && isDropIntoTarget) {
    keepExpanded.add(hoveredExecutionId);
  }
  return keepExpanded;
};

/** Keeps ancestor subflows visible when leaving the dragged flow context. */
export const keepExpandedIdsForPrune = (
  hoveredKeepExpanded: ReadonlySet<string>,
  pruneForDifferentFlow: boolean,
  draggedFlowContextId: string | null,
  ancestorPathIds: (id: string) => Set<string>,
): Set<string> => {
  const keepExpanded = new Set(hoveredKeepExpanded);

  if (pruneForDifferentFlow && draggedFlowContextId) {
    for (const id of ancestorPathIds(draggedFlowContextId)) {
      keepExpanded.add(id);
    }
  }

  return keepExpanded;
};
