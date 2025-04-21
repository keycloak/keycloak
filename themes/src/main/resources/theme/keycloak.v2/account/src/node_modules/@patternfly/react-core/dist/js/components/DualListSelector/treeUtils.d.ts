import { DualListSelectorTreeItemData } from './DualListSelectorTree';
export declare function flattenTree(tree: DualListSelectorTreeItemData[]): string[];
export declare function flattenTreeWithFolders(tree: DualListSelectorTreeItemData[]): string[];
export declare function filterFolders(tree: DualListSelectorTreeItemData[], inputList: string[]): string[];
export declare function filterTreeItems(item: DualListSelectorTreeItemData, inputList: string[]): boolean;
export declare function filterTreeItemsWithoutFolders(item: DualListSelectorTreeItemData, inputList: string[]): boolean;
export declare function filterRestTreeItems(item: DualListSelectorTreeItemData, inputList: string[]): boolean;
//# sourceMappingURL=treeUtils.d.ts.map