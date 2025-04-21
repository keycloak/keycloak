import * as React from 'react';
export interface TreeViewRootProps {
    /** Child nodes of the tree view */
    children: React.ReactNode;
    /** Flag indicating if the tree view has checkboxes */
    hasChecks?: boolean;
    /** Flag indicating if tree view has guide lines. */
    hasGuides?: boolean;
    /** Variant presentation styles for the tree view. */
    variant?: 'default' | 'compact' | 'compactNoBackground';
    /** Class to add to add if not passed a parentItem */
    className?: string;
}
export declare class TreeViewRoot extends React.Component<TreeViewRootProps> {
    displayName: string;
    private treeRef;
    componentDidMount(): void;
    componentWillUnmount(): void;
    handleKeys: (event: KeyboardEvent) => void;
    handleKeysCheckbox: (event: KeyboardEvent) => void;
    variantStyleModifiers: {
        [key in TreeViewRootProps['variant']]: string | string[];
    };
    render(): JSX.Element;
}
//# sourceMappingURL=TreeViewRoot.d.ts.map