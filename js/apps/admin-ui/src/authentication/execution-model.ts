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
    parent?: ExpandableExecution
  ) {
    super(oldIndex, newIndex);
    this.parent = parent;
  }
}

export class ExecutionList {
  private list: ExpandableExecution[];
  expandableList: ExpandableExecution[];

  constructor(list: AuthenticationExecutionInfoRepresentation[]) {
    this.list = list as ExpandableExecution[];

    const exList = {
      executionList: [],
      isCollapsed: false,
    };
    this.transformToExpandableList(0, -1, exList);
    this.expandableList = exList.executionList;
  }

  private transformToExpandableList(
    currentIndex: number,
    currentLevel: number,
    execution: ExpandableExecution
  ) {
    for (let index = currentIndex; index < this.list.length; index++) {
      const ex = this.list[index];
      const level = ex.level || 0;
      if (level <= currentLevel) {
        return index - 1;
      }

      const nextRowLevel = this.list[index + 1]?.level || 0;
      const hasChild = level < nextRowLevel;

      if (hasChild) {
        const subLevel = { ...ex, executionList: [], isCollapsed: false };
        index = this.transformToExpandableList(index + 1, level, subLevel);
        execution.executionList?.push(subLevel);
      } else {
        execution.executionList?.push(ex);
      }
    }
    return this.list.length;
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
    id: string,
    list?: ExpandableExecution[]
  ): ExpandableExecution | undefined {
    let found = (list || this.expandableList).find((ex) => ex.id === id);
    if (!found) {
      for (const ex of list || this.expandableList) {
        if (ex.executionList) {
          found = this.findExecution(id, ex.executionList);
          if (found) {
            return found;
          }
        }
      }
    }
    return found;
  }

  private getParentNodes(level?: number) {
    for (let index = 0; index < this.list.length; index++) {
      const ex = this.list[index];
      if (
        index + 1 < this.list.length &&
        this.list[index + 1].level! > ex.level! &&
        ex.level! + 1 === level
      ) {
        return ex;
      }
    }
  }

  getChange(
    changed: AuthenticationExecutionInfoRepresentation,
    order: string[]
  ) {
    const currentOrder = this.order();
    const newLocIndex = order.findIndex((id) => id === changed.id);
    const oldLocation =
      currentOrder[currentOrder.findIndex((ex) => ex.id === changed.id)];
    const newLocation = currentOrder[newLocIndex];

    if (newLocation.level !== oldLocation.level) {
      if (newLocation.level! > 0) {
        const parent = this.getParentNodes(newLocation.level);
        return new LevelChange(
          parent?.executionList?.length || 0,
          newLocation.index!,
          parent
        );
      }
      return new LevelChange(this.expandableList.length, newLocation.index!);
    }

    return new IndexChange(oldLocation.index!, newLocation.index!);
  }

  clone() {
    const newList = new ExecutionList([]);
    newList.list = this.list;
    newList.expandableList = this.expandableList;
    return newList;
  }
}
