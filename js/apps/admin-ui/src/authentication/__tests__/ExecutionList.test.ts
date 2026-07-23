import { describe, expect, it } from "vitest";
import { ExecutionList, IndexChange, LevelChange } from "../execution-model";

describe("ExecutionList", () => {
  const list2 = new ExecutionList([
    { id: "1", index: 0, level: 0 },
    { id: "2", index: 1, level: 0 },
    { id: "3", index: 0, level: 1 },
    { id: "4", index: 1, level: 1 },
    { id: "5", index: 0, level: 2 },
    { id: "6", index: 1, level: 2 },
    { id: "7", index: 2, level: 0 },
  ]);

  it("Move 1 down to the end", () => {
    const diff = list2.getChange({ id: "1" }, [
      "2",
      "3",
      "4",
      "5",
      "1",
      "6",
      "7",
    ]);

    expect(diff).toBeInstanceOf(LevelChange);
    expect((diff as LevelChange).parent?.id).toBe("4");
  });

  it("Index change", () => {
    const diff = list2.getChange({ id: "5" }, [
      "1",
      "2",
      "3",
      "4",
      "6",
      "5",
      "7",
    ]);

    expect(diff).toBeInstanceOf(IndexChange);
    expect((diff as IndexChange).newIndex).toBe(1);
    expect((diff as IndexChange).oldIndex).toBe(0);
  });

  it("Move 7 down to the top", () => {
    const diff = list2.getChange({ id: "7" }, [
      "7",
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
    ]);

    expect(diff).toBeInstanceOf(IndexChange);
    expect((diff as IndexChange).newIndex).toBe(0);
    expect((diff as IndexChange).oldIndex).toBe(2);
  });

  it("Move 5 to the top level", () => {
    const diff = list2.getChange({ id: "5" }, [
      "1",
      "5",
      "2",
      "3",
      "4",
      "6",
      "7",
    ]);

    expect(diff).toBeInstanceOf(LevelChange);
    expect((diff as LevelChange).parent).toBeUndefined();
  });

  it("Move 5 to the top level, begin of the list", () => {
    const diff = list2.getChange({ id: "5" }, [
      "5",
      "1",
      "2",
      "3",
      "4",
      "6",
      "7",
    ]);

    expect(diff).toBeInstanceOf(LevelChange);
    expect((diff as LevelChange).parent).toBeUndefined();
  });

  it("Move 6 one level up", () => {
    const diff = list2.getChange({ id: "6" }, [
      "1",
      "2",
      "6",
      "3",
      "4",
      "5",
      "7",
    ]);

    expect(diff).toBeInstanceOf(LevelChange);
    expect((diff as LevelChange).parent?.id).toBe("2");
  });

  it("Move a parent to the top", () => {
    const diff = list2.getChange({ id: "4" }, [
      "4",
      "5",
      "6",
      "1",
      "2",
      "3",
      "7",
    ]);

    expect(diff).toBeInstanceOf(LevelChange);
    expect((diff as LevelChange).parent?.id).toBeUndefined();
  });

  it("Move a parent same level", () => {
    const diff = list2.getChange({ id: "4" }, [
      "1",
      "2",
      "4",
      "5",
      "6",
      "3",
      "7",
    ]);

    expect(diff).toBeInstanceOf(IndexChange);
    expect((diff as IndexChange).newIndex).toBe(0);
  });

  it("Move 5 to the bottom", () => {
    const diff = list2.getChange({ id: "5" }, [
      "1",
      "2",
      "3",
      "4",
      "6",
      "7",
      "5",
    ]);

    expect(diff).toBeInstanceOf(LevelChange);
    expect((diff as LevelChange).parent).toBeUndefined();
  });

  it("Construct list", () => {
    //given
    const list = [
      { id: "0", level: 0, index: 0 },
      { id: "1", level: 1, index: 0 },
      { id: "2", level: 0, index: 1 },
      { id: "3", level: 1, index: 0 },
      { id: "4", level: 0, index: 2 },
      { id: "5", level: 1, index: 0 },
    ];

    //when
    const result = new ExecutionList(list);

    //then
    expect(result.expandableList).toEqual([
      {
        executionList: [{ id: "1", index: 0, level: 1 }],
        id: "0",
        index: 0,
        isCollapsed: false,
        level: 0,
      },
      {
        executionList: [{ id: "3", index: 0, level: 1 }],
        id: "2",
        index: 1,
        isCollapsed: false,
        level: 0,
      },
      {
        executionList: [{ id: "5", index: 0, level: 1 }],
        id: "4",
        index: 2,
        isCollapsed: false,
        level: 0,
      },
    ]);
  });

  it("When a sub-list has a sub-list all root nodes should not become part of first list", () => {
    //given
    const list = [
      { id: "0", level: 0, index: 0 },
      { id: "1", level: 1, index: 0 },
      { id: "2", level: 2, index: 0 },
      { id: "3", level: 0, index: 1 },
    ];

    //when
    const result = new ExecutionList(list);

    //then
    expect(result.expandableList).toEqual([
      {
        executionList: [
          {
            id: "1",
            index: 0,
            isCollapsed: false,
            level: 1,
            executionList: [{ id: "2", level: 2, index: 0 }],
          },
        ],
        id: "0",
        index: 0,
        isCollapsed: false,
        level: 0,
      },
      {
        id: "3",
        level: 0,
        index: 1,
      },
    ]);
  });

  describe("getChange", () => {
    const list2Data = [
      { id: "1", index: 0, level: 0 },
      { id: "2", index: 1, level: 0 },
      { id: "3", index: 0, level: 1 },
      { id: "4", index: 1, level: 1 },
      { id: "5", index: 0, level: 2 },
      { id: "6", index: 1, level: 2 },
      { id: "7", index: 2, level: 0 },
    ];

    const findInTree = (
      nodes: ExecutionList["expandableList"],
      id: string,
    ): (typeof nodes)[number] | undefined => {
      for (const node of nodes) {
        if (node.id === id) {
          return node;
        }
        if (node.executionList) {
          const found = findInTree(node.executionList, id);
          if (found) {
            return found;
          }
        }
      }
      return undefined;
    };

    it("LevelChange oldIndex should be target parent child count, not 0", () => {
      const list = new ExecutionList(list2Data);
      const parent4 = findInTree(list.expandableList, "4")!;

      const diff = list.getChange({ id: "1" }, [
        "2",
        "3",
        "4",
        "5",
        "1",
        "6",
        "7",
      ]);

      expect(diff).toBeInstanceOf(LevelChange);
      const levelChange = diff as LevelChange;
      expect(levelChange.parent?.id).toBe("4");
      expect(levelChange.oldIndex).toBe(parent4.executionList!.length);
      expect(levelChange.newIndex).toBe(1);
    });

    it("LevelChange oldIndex after moving up a level uses sibling count", () => {
      const list = new ExecutionList(list2Data);
      const parent2 = findInTree(list.expandableList, "2")!;

      const diff = list.getChange({ id: "6" }, [
        "1",
        "2",
        "6",
        "3",
        "4",
        "5",
        "7",
      ]);

      expect(diff).toBeInstanceOf(LevelChange);
      const levelChange = diff as LevelChange;
      expect(levelChange.parent?.id).toBe("2");
      expect(levelChange.oldIndex).toBe(parent2.executionList!.length);
      expect(levelChange.newIndex).toBe(0);
    });

    it("LevelChange parent should come from the tree, not the flat list", () => {
      const list = new ExecutionList(list2Data);

      const diff = list.getChange({ id: "1" }, [
        "2",
        "3",
        "4",
        "5",
        "1",
        "6",
        "7",
      ]);

      const levelChange = diff as LevelChange;
      const treeParent = findInTree(list.expandableList, "4")!;

      expect(levelChange.parent?.executionList?.map((c) => c.id)).toEqual(
        treeParent.executionList?.map((c) => c.id),
      );
    });

    it("does not throw when the moved execution is hidden by collapse", () => {
      const list = new ExecutionList(list2Data);
      findInTree(list.expandableList, "2")!.isCollapsed = true;

      const diff = list.getChange({ id: "3" }, ["1", "3", "2", "7"]);

      expect(diff).toBeDefined();
      expect(diff).toBeInstanceOf(IndexChange);
    });

    it("uses visual order for parent lookup when a subflow is collapsed", () => {
      const list = new ExecutionList(list2Data);
      findInTree(list.expandableList, "4")!.isCollapsed = true;

      const diff = list.getChange({ id: "3" }, ["1", "2", "4", "7", "3"]);

      expect(diff).toBeInstanceOf(IndexChange);
      expect((diff as IndexChange).oldIndex).toBe(0);
      expect((diff as IndexChange).newIndex).toBe(1);
    });
  });

  describe("resolveDropTarget", () => {
    const list2Data = [
      { id: "1", index: 0, level: 0 },
      {
        id: "2",
        index: 1,
        level: 0,
        authenticationFlow: true,
        displayName: "Subflow 2",
      },
      { id: "3", index: 0, level: 1 },
      {
        id: "4",
        index: 1,
        level: 1,
        authenticationFlow: true,
        displayName: "Subflow 4",
      },
      { id: "5", index: 0, level: 2 },
      { id: "6", index: 1, level: 2 },
      { id: "7", index: 2, level: 0 },
    ];

    it("nests into subflow via into vertical", () => {
      const list = new ExecutionList(list2Data);
      const resolved = list.resolveDropTarget("1", "2", "into");

      expect(resolved).not.toBeNull();
      expect(resolved!.kind).toBe("level-change");
      expect(resolved!.change).toBeInstanceOf(LevelChange);
      const levelChange = resolved!.change as LevelChange;
      expect(levelChange.parent?.id).toBe("2");
      expect(levelChange.oldIndex).toBe(2);
      expect(levelChange.newIndex).toBe(2);
      expect(resolved!.preview.mode).toBe("drop-into");
      expect(resolved!.preview.targetLevel).toBe(1);
      expect(resolved!.preview.targetParentId).toBe("2");
    });

    it("drop-into current parent uses post-append index excluding dragged child", () => {
      const list = new ExecutionList(list2Data);
      const subflow2 = list.expandableList.find((ex) => ex.id === "2")!;
      subflow2.isCollapsed = false;

      const resolved = list.resolveDropTarget("3", "2", "into");

      expect(resolved).not.toBeNull();
      expect(resolved!.kind).toBe("level-change");
      const levelChange = resolved!.change as LevelChange;
      expect(levelChange.parent?.id).toBe("2");
      expect(levelChange.oldIndex).toBe(1);
      expect(levelChange.newIndex).toBe(1);
    });

    it("drop-into current parent from last child position is a no-op index", () => {
      const list = new ExecutionList(list2Data);
      const subflow4 = list.expandableList
        .find((ex) => ex.id === "2")!
        .executionList!.find((ex) => ex.id === "4")!;
      subflow4.isCollapsed = false;

      const resolved = list.resolveDropTarget("6", "4", "into");

      expect(resolved).not.toBeNull();
      const levelChange = resolved!.change as LevelChange;
      expect(levelChange.parent?.id).toBe("4");
      expect(levelChange.oldIndex).toBe(1);
      expect(levelChange.newIndex).toBe(1);
    });

    it("reorders at same level when dropping after a row", () => {
      const list = new ExecutionList(list2Data);
      const resolved = list.resolveDropTarget("3", "4", "after");

      expect(resolved).not.toBeNull();
      expect(resolved!.preview.mode).toBe("reorder-after");
      expect(resolved!.preview.targetLevel).toBe(1);
      expect(resolved!.preview.targetParentId).toBe("2");
    });

    it("inserts between children in expanded subflow at hover position", () => {
      const list = new ExecutionList(list2Data);
      const subflow2 = list.expandableList.find((ex) => ex.id === "2")!;
      subflow2.isCollapsed = false;

      const resolved = list.resolveDropTarget("1", "3", "after");

      expect(resolved).not.toBeNull();
      expect(resolved!.preview.mode).toBe("reorder-after");
      expect(resolved!.kind).toBe("level-change");
      const levelChange = resolved!.change as LevelChange;
      expect(levelChange.parent?.id).toBe("2");
      expect(levelChange.newIndex).toBe(1);
    });

    it("inserts before first child in expanded subflow", () => {
      const list = new ExecutionList(list2Data);
      const subflow2 = list.expandableList.find((ex) => ex.id === "2")!;
      subflow2.isCollapsed = false;

      const resolved = list.resolveDropTarget("1", "3", "before");

      expect(resolved).not.toBeNull();
      expect(resolved!.preview.mode).toBe("reorder-before");
      const levelChange = resolved!.change as LevelChange;
      expect(levelChange.parent?.id).toBe("2");
      expect(levelChange.newIndex).toBe(0);
    });

    it("reorders within expanded subflow without moving to end", () => {
      const list = new ExecutionList(list2Data);
      const subflow2 = list.expandableList.find((ex) => ex.id === "2")!;
      subflow2.isCollapsed = false;

      const resolved = list.resolveDropTarget("4", "3", "before");

      expect(resolved).not.toBeNull();
      expect(resolved!.kind).toBe("reorder");
      expect(resolved!.change).toBeInstanceOf(IndexChange);
      expect((resolved!.change as IndexChange).newIndex).toBe(0);
    });

    it("reorders at same level without level change", () => {
      const list = new ExecutionList(list2Data);
      const resolved = list.resolveDropTarget("7", "1", "after");

      expect(resolved).not.toBeNull();
      expect(resolved!.kind).toBe("reorder");
      expect(resolved!.change).toBeInstanceOf(IndexChange);
      expect(resolved!.preview.targetLevel).toBe(0);
      expect(resolved!.preview.targetParentId).toBeNull();
    });

    it("reorder-after expanded subflow places item after subtree", () => {
      const list = new ExecutionList(list2Data);
      const subflow2 = list.expandableList.find((ex) => ex.id === "2")!;
      subflow2.isCollapsed = false;

      const resolved = list.resolveDropTarget("1", "2", "after");

      expect(resolved).not.toBeNull();
      expect(resolved!.preview.mode).toBe("reorder-after");
      expect(resolved!.preview.insertIndex).toBe(6);
      expect(resolved!.order).toEqual(["2", "3", "4", "5", "6", "1", "7"]);
    });

    it("reorder preview matches kind when no subflow parent applies", () => {
      const list = new ExecutionList(list2Data);
      const subflow2 = list.expandableList.find((ex) => ex.id === "2")!;
      subflow2.isCollapsed = false;

      const resolved = list.resolveDropTarget("4", "3", "before");

      expect(resolved!.kind).toBe("reorder");
      expect(resolved!.preview.targetParentId).toBe("2");
      expect(resolved!.preview.targetLevel).toBe(1);
    });

    it("preview insertIndex is before hover row for reorder-before", () => {
      const list = new ExecutionList(list2Data);
      const visualOrder = list.order();
      const hoverIndex = visualOrder.findIndex((ex) => ex.id === "7");
      const resolved = list.resolveDropTarget("1", "7", "before");

      expect(resolved!.preview.insertIndex).toBe(hoverIndex);
      expect(resolved!.preview.mode).toBe("reorder-before");
    });

    it("returns null for into on non-subflow", () => {
      const list = new ExecutionList(list2Data);
      expect(list.resolveDropTarget("1", "1", "into")).toBeNull();
    });
  });

  describe("collapse helpers", () => {
    const list2Data = [
      { id: "1", index: 0, level: 0 },
      {
        id: "2",
        index: 1,
        level: 0,
        authenticationFlow: true,
        displayName: "Subflow 2",
      },
      { id: "3", index: 0, level: 1 },
      { id: "7", index: 2, level: 0 },
    ];

    it("collapses all subflows", () => {
      const list = new ExecutionList(list2Data);
      const subflow2 = list.expandableList.find((ex) => ex.id === "2")!;
      subflow2.isCollapsed = false;

      list.collapseAllSubflows();

      expect(subflow2.isCollapsed).toBe(true);
    });

    it("restores collapse state from snapshot", () => {
      const list = new ExecutionList(list2Data);
      const subflow2 = list.expandableList.find((ex) => ex.id === "2")!;
      subflow2.isCollapsed = false;
      const snapshot = list.snapshotCollapseState();

      list.collapseAllSubflows();
      expect(subflow2.isCollapsed).toBe(true);

      list.restoreCollapseState(snapshot);
      expect(subflow2.isCollapsed).toBe(false);
    });
  });

  it("find item by index", () => {
    //given
    const list = [
      { id: "0", level: 0, index: 0 },
      { id: "1", level: 0, index: 1 },
      { id: "2", level: 0, index: 2 },
      { id: "3", level: 0, index: 3 },
      { id: "4", level: 1, index: 0 },
      { id: "5", level: 1, index: 1 },
      { id: "6", level: 2, index: 0 },
      { id: "7", level: 2, index: 1 },
      { id: "8", level: 0, index: 4 },
      { id: "9", level: 0, index: 5 },
      { id: "10", level: 0, index: 6 },
    ];

    //when
    const result = new ExecutionList(list);
    const item = result.findExecution(10);

    //then
    expect(item).toEqual({ id: "10", level: 0, index: 6 });
  });
});
