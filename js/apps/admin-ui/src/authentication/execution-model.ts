import type AuthenticationExecutionInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationExecutionInfoRepresentation";

export type ExpandableExecution = AuthenticationExecutionInfoRepresentation & {
  executionList?: ExpandableExecution[];
  isCollapsed: boolean;
};

export class IndexChange {
  oldIndex: number;
  newIndex: number;

  constructor(oldIndex: number, newIndex: number) {
    this.oldIndex = oldIndex;
    this.newIndex = newIndex;
  }
}

export class LevelChange extends IndexChange {
  parent?: ExpandableExecution;

  constructor(
    oldIndex: number,
    newIndex: number,
    parent?: ExpandableExecution,
  ) {
    super(oldIndex, newIndex);
    this.parent = parent;
  }
}

export type DropMode =
  | "reorder"
  | "reorder-before"
  | "reorder-after"
  | "drop-into";

export type DropVertical = "before" | "after" | "into";

export type DropInfo = {
  targetId: string | null;
  mode: DropMode;
  targetLevel: number;
  targetParentId: string | null;
  insertIndex: number;
};

export type ResolvedDrop = {
  kind: "reorder" | "level-change";
  change: IndexChange | LevelChange;
  preview: DropInfo;
  order: string[];
};

export class ExecutionList {
  #list: ExpandableExecution[];
  expandableList: ExpandableExecution[];

  constructor(list: AuthenticationExecutionInfoRepresentation[]) {
    this.#list = list as ExpandableExecution[];

    const exList = {
      executionList: [],
      isCollapsed: false,
    };
    this.#transformToExpandableList(0, -1, exList);
    this.expandableList = exList.executionList;
  }

  #transformToExpandableList(
    currentIndex: number,
    currentLevel: number,
    execution: ExpandableExecution,
  ) {
    for (let index = currentIndex; index < this.#list.length; index++) {
      const ex = this.#list[index];
      const level = ex.level || 0;
      if (level <= currentLevel) {
        return index - 1;
      }

      const nextRowLevel = this.#list[index + 1]?.level || 0;
      const hasChild = level < nextRowLevel;

      if (hasChild) {
        const subLevel = { ...ex, executionList: [], isCollapsed: false };
        index = this.#transformToExpandableList(index + 1, level, subLevel);
        execution.executionList?.push(subLevel);
      } else {
        execution.executionList?.push(ex);
      }
    }
    return this.#list.length;
  }

  order(list?: ExpandableExecution[]) {
    let result: ExpandableExecution[] = [];
    for (const row of list || this.expandableList) {
      result.push(row);
      if (row.executionList && !row.isCollapsed) {
        result = result.concat(this.order(row.executionList));
      }
    }
    return result;
  }

  findExecution(
    index: number,
    current: { index: number } = { index: 0 },
    list?: ExpandableExecution[],
  ): ExpandableExecution | undefined {
    const l = list || this.expandableList;
    for (let i = 0; i < l.length; i++) {
      const ex = l[i];

      if (current.index === index) {
        return ex;
      }
      current.index++;
      if (ex.executionList && !ex.isCollapsed) {
        const found = this.findExecution(index, current, ex.executionList);
        if (found) {
          return found;
        }
      }
    }
    return undefined;
  }

  #findInTree(
    id: string,
    list: ExpandableExecution[] = this.expandableList,
  ): ExpandableExecution | undefined {
    for (const ex of list) {
      if (ex.id === id) {
        return ex;
      }
      if (ex.executionList) {
        const found = this.#findInTree(id, ex.executionList);
        if (found) {
          return found;
        }
      }
    }
    return undefined;
  }

  #findParentOf(
    id: string,
    list: ExpandableExecution[] = this.expandableList,
  ): ExpandableExecution | undefined {
    for (const ex of list) {
      if (ex.executionList?.some((child) => child.id === id)) {
        return ex;
      }
      if (ex.executionList) {
        const found = this.#findParentOf(id, ex.executionList);
        if (found) {
          return found;
        }
      }
    }
    return undefined;
  }

  #getParentFromVisualOrder(
    level: number,
    visualIndex: number,
    visualOrder: ExpandableExecution[],
  ): ExpandableExecution | undefined {
    let parent: ExpandableExecution | undefined;
    for (let i = 0; i < visualIndex && i < visualOrder.length; i++) {
      const ex = visualOrder[i];
      if (level - 1 === (ex.level || 0)) {
        parent = ex;
      }
    }
    return parent;
  }

  #buildOrderAfterDrop(
    draggedId: string,
    insertIndex: number,
    visualOrder: ExpandableExecution[],
  ): string[] {
    const order = visualOrder.map((ex) => ex.id!);
    const oldIndex = order.indexOf(draggedId);
    if (oldIndex === -1) {
      return order;
    }
    order.splice(oldIndex, 1);
    let adjustedInsert = insertIndex;
    if (oldIndex < insertIndex) {
      adjustedInsert--;
    }
    order.splice(adjustedInsert, 0, draggedId);
    return order;
  }

  #insertIndexAfterSubflow(
    hoverIndex: number,
    hoverLevel: number,
    visualOrder: ExpandableExecution[],
  ): number {
    let insertIndex = hoverIndex + 1;
    for (let i = hoverIndex + 1; i < visualOrder.length; i++) {
      if ((visualOrder[i].level || 0) <= hoverLevel) {
        break;
      }
      insertIndex = i + 1;
    }
    return insertIndex;
  }

  #siblingIndexAtEndOfSubflow(
    draggedId: string,
    parent: ExpandableExecution,
  ): number {
    const children =
      parent.executionList?.filter((child) => child.id !== draggedId) ?? [];
    return children.length;
  }

  #siblingIndexRelativeToHover(
    draggedId: string,
    hoverId: string,
    vertical: DropVertical,
  ): number {
    const parent = this.#findParentOf(hoverId);
    const siblings = parent?.executionList ?? this.expandableList;
    const hoverIdx = siblings.findIndex((s) => s.id === hoverId);
    if (hoverIdx === -1) {
      return 0;
    }

    let targetIdx = vertical === "before" ? hoverIdx : hoverIdx + 1;
    const draggedIdx = siblings.findIndex((s) => s.id === draggedId);
    if (draggedIdx !== -1 && draggedIdx < targetIdx) {
      targetIdx--;
    }
    return targetIdx;
  }

  resolveDropTarget(
    draggedId: string,
    hoverId: string,
    vertical: DropVertical,
  ): ResolvedDrop | null {
    const visualOrder = this.order();
    const hoverIndex = visualOrder.findIndex((ex) => ex.id === hoverId);
    if (hoverIndex === -1) {
      return null;
    }

    const dragged = this.#findInTree(draggedId);
    const hover = visualOrder[hoverIndex];
    if (!dragged || draggedId === hoverId) {
      return null;
    }

    const hoverLevel = hover.level || 0;
    let insertIndex: number;
    let clampedLevel: number;
    let dropMode: DropMode;

    if (vertical === "into") {
      if (!hover.authenticationFlow) {
        return null;
      }
      dropMode = "drop-into";
      clampedLevel = hoverLevel + 1;
      insertIndex = this.#insertIndexAfterSubflow(
        hoverIndex,
        hoverLevel,
        visualOrder,
      );
    } else {
      dropMode = vertical === "before" ? "reorder-before" : "reorder-after";
      insertIndex = vertical === "before" ? hoverIndex : hoverIndex + 1;
      clampedLevel = hoverLevel;
    }

    const targetParent =
      vertical === "into"
        ? hover
        : clampedLevel > 0
          ? this.#getParentFromVisualOrder(
              clampedLevel,
              insertIndex,
              visualOrder,
            )
          : undefined;

    const targetParentId =
      clampedLevel > 0 && targetParent?.authenticationFlow
        ? (targetParent.id ?? null)
        : null;

    const preview: DropInfo = {
      targetId: hoverId,
      mode: dropMode,
      targetLevel: clampedLevel,
      targetParentId,
      insertIndex,
    };

    const order = this.#buildOrderAfterDrop(
      draggedId,
      insertIndex,
      visualOrder,
    );
    const draggedLevel = dragged.level || 0;
    const currentParent = this.#findParentOf(draggedId);

    const needsLevelChange =
      vertical === "into" ||
      clampedLevel !== draggedLevel ||
      currentParent?.id !== targetParent?.id;

    if (needsLevelChange) {
      const parent =
        vertical === "into"
          ? hover
          : clampedLevel > 0
            ? targetParent
            : undefined;

      if (clampedLevel > 0 && !parent?.authenticationFlow) {
        return {
          kind: "reorder",
          change: this.getChange(dragged, order),
          preview,
          order,
        };
      }

      const change =
        vertical === "into"
          ? new LevelChange(
              hover.executionList?.length || 0,
              this.#siblingIndexAtEndOfSubflow(draggedId, hover),
              hover,
            )
          : clampedLevel > 0
            ? new LevelChange(
                parent!.executionList?.length || 0,
                this.#siblingIndexRelativeToHover(draggedId, hoverId, vertical),
                parent,
              )
            : new LevelChange(
                this.expandableList.length,
                this.#siblingIndexRelativeToHover(draggedId, hoverId, vertical),
              );

      return {
        kind: "level-change",
        change,
        preview,
        order,
      };
    }

    return {
      kind: "reorder",
      change: this.getChange(dragged, order),
      preview,
      order,
    };
  }

  #siblingIndexInOrder(
    changedId: string,
    level: number,
    parentId: string | undefined,
    orderIds: string[],
  ): number {
    let siblingIndex = 0;
    for (const id of orderIds) {
      if (id === changedId) {
        return siblingIndex;
      }
      const node = this.#findInTree(id);
      if (!node) {
        continue;
      }
      if (
        (node.level || 0) === level &&
        this.#findParentOf(id)?.id === parentId
      ) {
        siblingIndex++;
      }
    }
    return siblingIndex;
  }

  getChange(
    changed: AuthenticationExecutionInfoRepresentation,
    order: string[],
  ) {
    const currentOrder = this.order();
    const changedId = changed.id!;
    const newLocIndex = order.findIndex((id) => id === changedId);
    const oldLocIndex = currentOrder.findIndex((ex) => ex.id === changedId);

    const oldLocation =
      oldLocIndex >= 0
        ? currentOrder[oldLocIndex]
        : this.#findInTree(changedId);
    if (!oldLocation) {
      return new IndexChange(0, 0);
    }

    const newLocation =
      newLocIndex >= 0 && newLocIndex < currentOrder.length
        ? currentOrder[newLocIndex]
        : undefined;
    if (!newLocation) {
      return new IndexChange(oldLocation.index!, oldLocation.index!);
    }

    const oldLevel = oldLocation.level || 0;
    const newLevel = newLocation.level || 0;

    const currentParent =
      oldLocIndex >= 0
        ? this.#getParentFromVisualOrder(oldLevel, oldLocIndex, currentOrder)
        : this.#findParentOf(changedId);

    const dropParent = this.#getParentFromVisualOrder(
      oldLevel,
      newLocIndex,
      currentOrder,
    );
    const targetParent = this.#getParentFromVisualOrder(
      newLevel,
      newLocIndex,
      currentOrder,
    );

    const parentChanged = currentParent?.id !== dropParent?.id;
    const levelChanged = oldLevel !== newLevel;

    const nestedSiblingIndex = this.#siblingIndexInOrder(
      changedId,
      oldLevel,
      currentParent?.id,
      order,
    );
    const collapsedSameParentReorder =
      dropParent?.id === currentParent?.id &&
      newLevel < oldLevel &&
      nestedSiblingIndex >= 0 &&
      !(oldLevel > 1 && newLevel === 0);

    if (collapsedSameParentReorder) {
      return new IndexChange(oldLocation.index!, nestedSiblingIndex);
    }

    if (parentChanged || levelChanged) {
      const parent = levelChanged ? targetParent : dropParent;
      if (newLevel > 0) {
        return new LevelChange(
          parent?.executionList?.length || 0,
          this.#siblingIndexInOrder(changedId, newLevel, parent?.id, order),
          parent,
        );
      }
      return new LevelChange(
        this.expandableList.length,
        this.#siblingIndexInOrder(changedId, 0, undefined, order),
      );
    }

    return new IndexChange(
      oldLocation.index!,
      this.#siblingIndexInOrder(changedId, oldLevel, currentParent?.id, order),
    );
  }

  findParentOf(id: string): ExpandableExecution | undefined {
    return this.#findParentOf(id);
  }

  ancestorPathIds(id: string): Set<string> {
    const ancestors = new Set<string>();
    let current = this.#findParentOf(id);
    while (current?.id) {
      ancestors.add(current.id);
      current = this.#findParentOf(current.id);
    }
    return ancestors;
  }

  collapseAllSubflows() {
    this.#applyCollapseState(this.expandableList, new Set());
  }

  collapseAllExceptAncestorPath(draggedId: string) {
    const keepExpanded = this.ancestorPathIds(draggedId);
    this.#applyCollapseState(this.expandableList, keepExpanded);
  }

  #applyCollapseState(list: ExpandableExecution[], keepExpanded: Set<string>) {
    for (const ex of list) {
      if (ex.executionList?.length) {
        ex.isCollapsed = !keepExpanded.has(ex.id!);
        this.#applyCollapseState(ex.executionList, keepExpanded);
      }
    }
  }

  snapshotCollapseState(): Map<string, boolean> {
    const map = new Map<string, boolean>();
    this.#collectCollapseState(this.expandableList, map);
    return map;
  }

  #collectCollapseState(
    list: ExpandableExecution[],
    map: Map<string, boolean>,
  ) {
    for (const ex of list) {
      if (ex.executionList?.length && ex.id) {
        map.set(ex.id, ex.isCollapsed);
        this.#collectCollapseState(ex.executionList, map);
      }
    }
  }

  restoreCollapseState(snapshot: Map<string, boolean>) {
    this.#restoreCollapseState(this.expandableList, snapshot);
  }

  #restoreCollapseState(
    list: ExpandableExecution[],
    snapshot: Map<string, boolean>,
  ) {
    for (const ex of list) {
      if (ex.executionList?.length && ex.id && snapshot.has(ex.id)) {
        ex.isCollapsed = snapshot.get(ex.id)!;
        this.#restoreCollapseState(ex.executionList, snapshot);
      }
    }
  }

  clone() {
    const newList = new ExecutionList([]);
    newList.#list = this.#list;
    newList.expandableList = this.expandableList;
    return newList;
  }
}
