import React from 'react';
import { TreeViewDataItem } from './TreeView';
export interface TreeViewCheckProps extends Partial<React.InputHTMLAttributes<HTMLInputElement>> {
    checked?: boolean | null;
}
export interface TreeViewListItemProps {
    /** Internal content of a tree view item */
    name: React.ReactNode;
    /** Title a tree view item */
    title: React.ReactNode;
    /** ID of a tree view item */
    id?: string;
    /** Flag indicating if the node is expanded, overrides internal state */
    isExpanded?: boolean;
    /** Flag indicating if node is expanded by default */
    defaultExpanded?: boolean;
    /** Child nodes of a tree view item */
    children?: React.ReactNode;
    /** Callback for item selection. Note: calling event.preventDefault() will prevent the node from toggling. */
    onSelect?: (event: React.MouseEvent, item: TreeViewDataItem, parent: TreeViewDataItem) => void;
    /** Callback for item checkbox selection */
    onCheck?: (event: React.ChangeEvent, item: TreeViewDataItem, parent: TreeViewDataItem) => void;
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
    /** Flag indicating if the tree view is using a compact variation. */
    isCompact?: boolean;
    /** Active items of tree view */
    activeItems?: TreeViewDataItem[];
    /** Data structure of tree view item */
    itemData?: TreeViewDataItem;
    /** Parent item of tree view item */
    parentItem?: TreeViewDataItem;
    /** Default icon of a tree view item */
    icon?: React.ReactNode;
    /** Expanded icon of a tree view item */
    expandedIcon?: React.ReactNode;
    /** Action of a tree view item, can be a Button or Dropdown */
    action?: React.ReactNode;
    /** Callback for item comparison function */
    compareItems?: (item: TreeViewDataItem, itemToCheck: TreeViewDataItem) => boolean;
    /** Flag indicating the TreeView should utilize memoization to help render large data sets. Setting this property requires that `activeItems` pass in an array containing every node in the selected item's path. */
    useMemo?: boolean;
}
export declare const TreeViewListItem: React.NamedExoticComponent<TreeViewListItemProps>;
//# sourceMappingURL=TreeViewListItem.d.ts.map