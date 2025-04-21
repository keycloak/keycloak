import React from 'react';
import {
  DualListSelector,
  DualListSelectorPane,
  DualListSelectorList,
  DualListSelectorControlsWrapper,
  DualListSelectorControl,
  DualListSelectorTree,
  DualListSelectorTreeItemData,
  SearchInput
} from '@patternfly/react-core';
import AngleDoubleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-left-icon';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleDoubleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-right-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';

interface FoodNode {
  id: string;
  text: string;
  children?: FoodNode[];
}

interface ExampleProps {
  data: FoodNode[];
}

export const DualListSelectorComposableTree: React.FunctionComponent<ExampleProps> = ({ data }: ExampleProps) => {
  const [checkedLeafIds, setCheckedLeafIds] = React.useState<string[]>([]);
  const [chosenLeafIds, setChosenLeafIds] = React.useState<string[]>(['beans', 'beef', 'chicken', 'tofu']);
  const [chosenFilter, setChosenFilter] = React.useState<string>('');
  const [availableFilter, setAvailableFilter] = React.useState<string>('');
  let hiddenChosen: string[] = [];
  let hiddenAvailable: string[] = [];

  // helper function to build memoized lists
  const buildTextById = (node: FoodNode): { [key: string]: string } => {
    let textById = {};
    if (!node) {
      return textById;
    }
    textById[node.id] = node.text;
    if (node.children) {
      node.children.forEach(child => {
        textById = { ...textById, ...buildTextById(child) };
      });
    }
    return textById;
  };

  // helper function to build memoized lists
  const getDescendantLeafIds = (node: FoodNode): string[] => {
    if (!node.children || !node.children.length) {
      return [node.id];
    } else {
      let childrenIds = [];
      node.children.forEach(child => {
        childrenIds = [...childrenIds, ...getDescendantLeafIds(child)];
      });
      return childrenIds;
    }
  };

  // helper function to build memoized lists
  const getLeavesById = (node: FoodNode): { [key: string]: string[] } => {
    let leavesById = {};
    if (!node.children || !node.children.length) {
      leavesById[node.id] = [node.id];
    } else {
      node.children.forEach(child => {
        leavesById[node.id] = getDescendantLeafIds(node);
        leavesById = { ...leavesById, ...getLeavesById(child) };
      });
    }
    return leavesById;
  };

  // Builds a map of child leaf nodes by node id - memoized so that it only rebuilds the list if the data changes.
  const { memoizedLeavesById, memoizedAllLeaves, memoizedNodeText } = React.useMemo(() => {
    let leavesById = {};
    let allLeaves = [];
    let nodeTexts = {};
    data.forEach(foodNode => {
      nodeTexts = { ...nodeTexts, ...buildTextById(foodNode) };
      leavesById = { ...leavesById, ...getLeavesById(foodNode) };
      allLeaves = [...allLeaves, ...getDescendantLeafIds(foodNode)];
    });
    return {
      memoizedLeavesById: leavesById,
      memoizedAllLeaves: allLeaves,
      memoizedNodeText: nodeTexts
    };
  }, [data]);

  const moveChecked = (toChosen: boolean) => {
    setChosenLeafIds(
      prevChosenIds =>
        toChosen
          ? [...prevChosenIds, ...checkedLeafIds] // add checked ids to chosen list
          : [...prevChosenIds.filter(x => !checkedLeafIds.includes(x))] // remove checked ids from chosen list
    );

    // uncheck checked ids that just moved
    setCheckedLeafIds(prevChecked =>
      toChosen
        ? [...prevChecked.filter(x => chosenLeafIds.includes(x))]
        : [...prevChecked.filter(x => !chosenLeafIds.includes(x))]
    );
  };

  const moveAll = (toChosen: boolean) => {
    if (toChosen) {
      setChosenLeafIds(memoizedAllLeaves);
    } else {
      setChosenLeafIds([]);
    }
  };

  const areAllDescendantsSelected = (node: FoodNode, isChosen: boolean) =>
    memoizedLeavesById[node.id].every(
      id => checkedLeafIds.includes(id) && (isChosen ? chosenLeafIds.includes(id) : !chosenLeafIds.includes(id))
    );
  const areSomeDescendantsSelected = (node: FoodNode, isChosen: boolean) =>
    memoizedLeavesById[node.id].some(
      id => checkedLeafIds.includes(id) && (isChosen ? chosenLeafIds.includes(id) : !chosenLeafIds.includes(id))
    );

  const isNodeChecked = (node: FoodNode, isChosen: boolean) => {
    if (areAllDescendantsSelected(node, isChosen)) {
      return true;
    }
    if (areSomeDescendantsSelected(node, isChosen)) {
      return null;
    }
    return false;
  };

  const onOptionCheck = (
    event: React.MouseEvent | React.ChangeEvent<HTMLInputElement> | React.KeyboardEvent,
    isChecked: boolean,
    node: DualListSelectorTreeItemData,
    isChosen: boolean
  ) => {
    const nodeIdsToCheck = memoizedLeavesById[node.id].filter(id =>
      isChosen
        ? chosenLeafIds.includes(id) && !hiddenChosen.includes(id)
        : !chosenLeafIds.includes(id) && !hiddenAvailable.includes(id)
    );
    if (isChosen) {
      hiddenChosen = [];
    } else {
      hiddenAvailable = [];
    }
    setCheckedLeafIds(prevChecked => {
      const otherCheckedNodeNames = prevChecked.filter(id => !nodeIdsToCheck.includes(id));
      return !isChecked ? otherCheckedNodeNames : [...otherCheckedNodeNames, ...nodeIdsToCheck];
    });
  };

  // builds a search input - used in each dual list selector pane
  const buildSearchInput = (isChosen: boolean) => {
    const onChange = value => (isChosen ? setChosenFilter(value) : setAvailableFilter(value));

    return (
      <SearchInput value={isChosen ? chosenFilter : availableFilter} onChange={onChange} onClear={() => onChange('')} />
    );
  };

  // Builds the DualListSelectorTreeItems from the FoodNodes
  const buildOptions = (
    isChosen: boolean,
    [node, ...remainingNodes]: FoodNode[],
    hasParentMatch: boolean
  ): DualListSelectorTreeItemData[] => {
    if (!node) {
      return [];
    }

    const isChecked = isNodeChecked(node, isChosen);

    const filterValue = isChosen ? chosenFilter : availableFilter;
    const descendentLeafIds = memoizedLeavesById[node.id];
    const descendentsOnThisPane = isChosen
      ? descendentLeafIds.filter(id => chosenLeafIds.includes(id))
      : descendentLeafIds.filter(id => !chosenLeafIds.includes(id));

    const hasMatchingChildren =
      filterValue && descendentsOnThisPane.some(id => memoizedNodeText[id].includes(filterValue));
    const isFilterMatch = filterValue && node.text.includes(filterValue) && descendentsOnThisPane.length > 0;

    // A node is displayed if either of the following is true:
    //   - There is no filter value and this node or its descendents belong on this pane
    //   - There is a filter value and this node or one of this node's descendents or ancestors match on this pane
    const isDisplayed =
      (!filterValue && descendentsOnThisPane.length > 0) ||
      hasMatchingChildren ||
      (hasParentMatch && descendentsOnThisPane.length > 0) ||
      isFilterMatch;

    if (!isDisplayed) {
      if (isChosen) {
        hiddenChosen.push(node.id);
      } else {
        hiddenAvailable.push(node.id);
      }
    }

    return [
      ...(isDisplayed
        ? [
            {
              id: node.id,
              text: node.text,
              isChecked,
              checkProps: { 'aria-label': `Select ${node.text}` },
              hasBadge: node.children && node.children.length > 0,
              badgeProps: { isRead: true },
              defaultExpanded: isChosen ? !!chosenFilter : !!availableFilter,
              children: node.children
                ? buildOptions(isChosen, node.children, isFilterMatch || hasParentMatch)
                : undefined
            }
          ]
        : []),
      ...(!isDisplayed && node.children && node.children.length
        ? buildOptions(isChosen, node.children, hasParentMatch)
        : []),
      ...(remainingNodes ? buildOptions(isChosen, remainingNodes, hasParentMatch) : [])
    ];
  };

  const buildPane = (isChosen: boolean): React.ReactNode => {
    const options: DualListSelectorTreeItemData[] = buildOptions(isChosen, data, false);
    const numOptions = isChosen ? chosenLeafIds.length : memoizedAllLeaves.length - chosenLeafIds.length;
    const numSelected = checkedLeafIds.filter(id =>
      isChosen ? chosenLeafIds.includes(id) : !chosenLeafIds.includes(id)
    ).length;
    const status = `${numSelected} of ${numOptions} options selected`;
    return (
      <DualListSelectorPane
        title={isChosen ? 'Chosen' : 'Available'}
        status={status}
        searchInput={buildSearchInput(isChosen)}
        isChosen={isChosen}
      >
        <DualListSelectorList>
          <DualListSelectorTree
            data={options}
            onOptionCheck={(e, isChecked, itemData) => onOptionCheck(e, isChecked, itemData, isChosen)}
          />
        </DualListSelectorList>
      </DualListSelectorPane>
    );
  };

  return (
    <DualListSelector isTree>
      {buildPane(false)}
      <DualListSelectorControlsWrapper aria-label="Selector controls">
        <DualListSelectorControl
          isDisabled={!checkedLeafIds.filter(x => !chosenLeafIds.includes(x)).length}
          onClick={() => moveChecked(true)}
          aria-label="Add selected"
        >
          <AngleRightIcon />
        </DualListSelectorControl>
        <DualListSelectorControl
          isDisabled={chosenLeafIds.length === memoizedAllLeaves.length}
          onClick={() => moveAll(true)}
          aria-label="Add all"
        >
          <AngleDoubleRightIcon />
        </DualListSelectorControl>
        <DualListSelectorControl
          isDisabled={chosenLeafIds.length === 0}
          onClick={() => moveAll(false)}
          aria-label="Remove all"
        >
          <AngleDoubleLeftIcon />
        </DualListSelectorControl>
        <DualListSelectorControl
          onClick={() => moveChecked(false)}
          isDisabled={!checkedLeafIds.filter(x => !!chosenLeafIds.includes(x)).length}
          aria-label="Remove selected"
        >
          <AngleLeftIcon />
        </DualListSelectorControl>
      </DualListSelectorControlsWrapper>
      {buildPane(true)}
    </DualListSelector>
  );
};

export const DualListSelectorComposableTreeExample: React.FunctionComponent = () => (
  <DualListSelectorComposableTree
    data={[
      {
        id: 'fruits',
        text: 'Fruits',
        children: [
          { id: 'apple', text: 'Apple' },
          {
            id: 'berries',
            text: 'Berries',
            children: [
              { id: 'blueberry', text: 'Blueberry' },
              { id: 'strawberry', text: 'Strawberry' }
            ]
          },
          { id: 'banana', text: 'Banana' }
        ]
      },
      { id: 'bread', text: 'Bread' },
      {
        id: 'vegetables',
        text: 'Vegetables',
        children: [
          { id: 'broccoli', text: 'Broccoli' },
          { id: 'cauliflower', text: 'Cauliflower' }
        ]
      },
      {
        id: 'proteins',
        text: 'Proteins',
        children: [
          { id: 'beans', text: 'Beans' },
          {
            id: 'meats',
            text: 'Meats',
            children: [
              {
                id: 'beef',
                text: 'Beef'
              },
              {
                id: 'chicken',
                text: 'Chicken'
              }
            ]
          },
          { id: 'tofu', text: 'Tofu' }
        ]
      }
    ]}
  />
);
