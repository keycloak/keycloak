export function flattenTree(tree) {
    let result = [];
    tree.forEach(item => {
        if (item.children) {
            result = result.concat(flattenTree(item.children));
        }
        else {
            result.push(item.id);
        }
    });
    return result;
}
export function flattenTreeWithFolders(tree) {
    let result = [];
    tree.forEach(item => {
        result.push(item.id);
        if (item.children) {
            result = result.concat(flattenTreeWithFolders(item.children));
        }
    });
    return result;
}
export function filterFolders(tree, inputList) {
    let result = [];
    tree.forEach(item => {
        if (item.children) {
            result = result.concat(filterFolders(item.children, inputList));
        }
        else {
            if (inputList.includes(item.id)) {
                result.push(item.id);
            }
        }
    });
    return result;
}
export function filterTreeItems(item, inputList) {
    if (inputList.includes(item.id)) {
        return true;
    }
    if (item.children) {
        return ((item.children = item.children
            .map(opt => Object.assign({}, opt))
            .filter(child => filterTreeItems(child, inputList))).length > 0);
    }
}
export function filterTreeItemsWithoutFolders(item, inputList) {
    if (item.children) {
        return ((item.children = item.children
            .map(opt => Object.assign({}, opt))
            .filter(child => child.children ? filterTreeItemsWithoutFolders(child, inputList) : filterTreeItems(child, inputList))).length > 0);
    }
    if (inputList.includes(item.id)) {
        return true;
    }
}
export function filterRestTreeItems(item, inputList) {
    if (item.children) {
        const child = (item.children = item.children
            .map(opt => Object.assign({}, opt))
            .filter(child => filterRestTreeItems(child, inputList))).length > 0;
        return child;
    }
    if (!inputList.includes(item.id)) {
        return true;
    }
}
//# sourceMappingURL=treeUtils.js.map