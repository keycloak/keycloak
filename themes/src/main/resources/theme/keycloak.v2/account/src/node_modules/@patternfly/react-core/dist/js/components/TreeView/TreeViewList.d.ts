import * as React from 'react';
export interface TreeViewListProps extends React.HTMLProps<HTMLUListElement> {
    /** Flag indicating if the tree view is nested under another tree view */
    isNested?: boolean;
    /** Toolbar to display above the tree view */
    toolbar?: React.ReactNode;
    /** Child nodes of the current tree view */
    children: React.ReactNode;
}
export declare const TreeViewList: React.FunctionComponent<TreeViewListProps>;
//# sourceMappingURL=TreeViewList.d.ts.map