import * as React from 'react';
import { TreeViewCheckProps } from './TreeViewListItem';
export interface TreeViewDataItem {
    /** Internal content of a tree view item */
    name: React.ReactNode;
    /** Title a tree view item. Only used in Compact presentations. */
    title?: React.ReactNode;
    /** ID of a tree view item */
    id?: string;
    /** Child nodes of a tree view item */
    children?: TreeViewDataItem[];
    /** Flag indicating if node is expanded by default */
    defaultExpanded?: boolean;
    /** Default icon of a tree view item */
    icon?: React.ReactNode;
    /** Expanded icon of a tree view item */
    expandedIcon?: React.ReactNode;
    /** Flag indicating if a tree view item has a checkbox */
    hasCheck?: boolean;
    /** Additional properties of the tree view item checkbox */
    checkProps?: TreeViewCheckProps;
    /** Flag indicating if a tree view item has a badge */
    hasBadge?: boolean;
    /** Optional prop for custom badge */
    customBadgeContent?: React.ReactNode;
    /** Additional properties of the tree view item badge */
    badgeProps?: any;
    /** Action of a tree view item, can be a Button or Dropdown */
    action?: React.ReactNode;
}
export interface TreeViewProps {
    /** Data of the tree view */
    data: TreeViewDataItem[];
    /** ID of the tree view */
    id?: string;
    /** Flag indicating if the tree view is nested */
    isNested?: boolean;
    /** Flag indicating if all nodes in the tree view should have checkboxes */
    hasChecks?: boolean;
    /** Flag indicating if all nodes in the tree view should have badges */
    hasBadges?: boolean;
    /** Flag indicating if tree view has guide lines. */
    hasGuides?: boolean;
    /** Variant presentation styles for the tree view. */
    variant?: 'default' | 'compact' | 'compactNoBackground';
    /** Icon for all leaf or unexpanded node items */
    icon?: React.ReactNode;
    /** Icon for all expanded node items */
    expandedIcon?: React.ReactNode;
    /** Sets the expanded state on all tree nodes, overriding default behavior and current internal state */
    allExpanded?: boolean;
    /** Sets the default expanded behavior */
    defaultAllExpanded?: boolean;
    /** Callback for item selection */
    onSelect?: (event: React.MouseEvent, item: TreeViewDataItem, parentItem: TreeViewDataItem) => void;
    /** Callback for item checkbox selection */
    onCheck?: (event: React.ChangeEvent, item: TreeViewDataItem, parentItem: TreeViewDataItem) => void;
    /** Active items of tree view */
    activeItems?: TreeViewDataItem[];
    /** Internal. Parent item of a TreeViewListItem */
    parentItem?: TreeViewDataItem;
    /** Comparison function for determining active items */
    compareItems?: (item: TreeViewDataItem, itemToCheck: TreeViewDataItem) => boolean;
    /** Class to add to add if not passed a parentItem */
    className?: string;
    /** Toolbar to display above the tree view */
    toolbar?: React.ReactNode;
    /** Flag indicating the TreeView should utilize memoization to help render large data sets. Setting this property requires that `activeItems` pass in an array containing every node in the selected item's path. */
    useMemo?: boolean;
}
export declare const TreeView: React.FunctionComponent<TreeViewProps>;
//# sourceMappingURL=TreeView.d.ts.map