import { DualListSelectorTreeItemData } from './DualListSelectorTree';

export function flattenTree(tree: DualListSelectorTreeItemData[]): string[] {
  let result = [] as string[];
  tree.forEach(item => {
    if (item.children) {
      result = result.concat(flattenTree(item.children));
    } else {
      result.push(item.id);
    }
  });
  return result;
}

export function flattenTreeWithFolders(tree: DualListSelectorTreeItemData[]): string[] {
  let result = [] as string[];
  tree.forEach(item => {
    result.push(item.id);
    if (item.children) {
      result = result.concat(flattenTreeWithFolders(item.children));
    }
  });
  return result;
}

export function filterFolders(tree: DualListSelectorTreeItemData[], inputList: string[]): string[] {
  let result = [] as string[];
  tree.forEach(item => {
    if (item.children) {
      result = result.concat(filterFolders(item.children, inputList));
    } else {
      if (inputList.includes(item.id)) {
        result.push(item.id);
      }
    }
  });
  return result;
}

export function filterTreeItems(item: DualListSelectorTreeItemData, inputList: string[]): boolean {
  if (inputList.includes(item.id)) {
    return true;
  }
  if (item.children) {
    return (
      (item.children = item.children
        .map(opt => Object.assign({}, opt))
        .filter(child => filterTreeItems(child, inputList))).length > 0
    );
  }
}

export function filterTreeItemsWithoutFolders(item: DualListSelectorTreeItemData, inputList: string[]): boolean {
  if (item.children) {
    return (
      (item.children = item.children
        .map(opt => Object.assign({}, opt))
        .filter(child =>
          child.children ? filterTreeItemsWithoutFolders(child, inputList) : filterTreeItems(child, inputList)
        )).length > 0
    );
  }

  if (inputList.includes(item.id)) {
    return true;
  }
}

export function filterRestTreeItems(item: DualListSelectorTreeItemData, inputList: string[]): boolean {
  if (item.children) {
    const child =
      (item.children = item.children
        .map(opt => Object.assign({}, opt))
        .filter(child => filterRestTreeItems(child, inputList))).length > 0;
    return child;
  }

  if (!inputList.includes(item.id)) {
    return true;
  }
}
