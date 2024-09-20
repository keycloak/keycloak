import { useEffect, useState } from "react";

import { TreeView, TreeViewDataItem } from "@patternfly/react-core";

type CheckableTreeViewProps = {
  data: TreeViewDataItem[];
  onSelect: (items: TreeViewDataItem[]) => void;
};

export const CheckableTreeView = ({
  data,
  onSelect,
}: CheckableTreeViewProps) => {
  const [state, setState] = useState<{
    options: TreeViewDataItem[];
    checkedItems: TreeViewDataItem[];
  }>({ options: [], checkedItems: [] });

  useEffect(() => {
    onSelect(state.checkedItems.filter((i) => i.checkProps?.checked === true));
  }, [state]);

  useEffect(() => {
    setState({ options: data, checkedItems: [] });
  }, [data]);

  const flattenTree = (tree: TreeViewDataItem[]) => {
    let result: TreeViewDataItem[] = [];
    tree.forEach((item) => {
      result.push(item);
      if (item.children) {
        result = result.concat(flattenTree(item.children));
      }
    });
    return result;
  };

  const onCheck = (evt: React.ChangeEvent, treeViewItem: TreeViewDataItem) => {
    const checked = (evt.target as HTMLInputElement).checked;
    const flatCheckedItems = flattenTree([treeViewItem]);

    setState((prevState) => {
      return {
        options: prevState.options,
        checkedItems: checked
          ? prevState.checkedItems.concat(
              flatCheckedItems.filter(
                (item) => !prevState.checkedItems.some((i) => i.id === item.id),
              ),
            )
          : prevState.checkedItems.filter(
              (item) => !flatCheckedItems.some((i) => i.id === item.id),
            ),
      };
    });
  };

  const isChecked = (item: TreeViewDataItem) =>
    state.checkedItems.some((i) => i.id === item.id);

  const areSomeDescendantsChecked = (dataItem: TreeViewDataItem): boolean =>
    dataItem.children
      ? dataItem.children.some((child) => areSomeDescendantsChecked(child))
      : isChecked(dataItem);

  const mapTree = (item: TreeViewDataItem): TreeViewDataItem => {
    const hasCheck = isChecked(item);
    // Reset checked properties to be updated
    item.checkProps!.checked = false;

    if (hasCheck) {
      item.checkProps!.checked = true;
    } else {
      const hasPartialCheck = areSomeDescendantsChecked(item);
      if (hasPartialCheck) {
        item.checkProps!.checked = null;
      }
    }

    if (item.children) {
      return {
        ...item,
        children: item.children.map((child) => mapTree(child)),
      };
    }
    return item;
  };

  const mapped = state.options.map((item) => mapTree(item));
  return <TreeView data={mapped} onCheck={onCheck} hasCheckboxes />;
};
