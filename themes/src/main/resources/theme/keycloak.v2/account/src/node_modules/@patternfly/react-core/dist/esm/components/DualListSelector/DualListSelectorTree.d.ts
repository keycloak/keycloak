import * as React from 'react';
export interface DualListSelectorTreeItemData {
    /** Content rendered inside the dual list selector. */
    children?: DualListSelectorTreeItemData[];
    /** Additional classes applied to the dual list selector. */
    className?: string;
    /** Flag indicating this option is expanded by default. */
    defaultExpanded?: boolean;
    /** Flag indicating this option has a badge */
    hasBadge?: boolean;
    /** Callback fired when an option is checked */
    onOptionCheck?: (event: React.MouseEvent | React.ChangeEvent<HTMLInputElement> | React.KeyboardEvent, isChecked: boolean, isChosen: boolean, itemData: DualListSelectorTreeItemData) => void;
    /** ID of the option */
    id: string;
    /** Text of the option */
    text: string;
    /** Parent id of an option */
    parentId?: string;
    /** Checked state of the option */
    isChecked: boolean;
    /** Additional properties to pass to the option checkbox */
    checkProps?: any;
    /** Additional properties to pass to the option badge */
    badgeProps?: any;
    /** Flag indicating whether the component is disabled. */
    isDisabled?: boolean;
}
export interface DualListSelectorTreeProps {
    /** Data of the tree view */
    data: DualListSelectorTreeItemData[] | (() => DualListSelectorTreeItemData[]);
    /** ID of the tree view */
    id?: string;
    /** @hide Flag indicating if the list is nested */
    isNested?: boolean;
    /** Flag indicating if all options should have badges */
    hasBadges?: boolean;
    /** Sets the default expanded behavior */
    defaultAllExpanded?: boolean;
    /** Callback fired when an option is checked */
    isDisabled?: boolean;
    onOptionCheck?: (event: React.MouseEvent | React.ChangeEvent<HTMLInputElement> | React.KeyboardEvent, isChecked: boolean, itemData: DualListSelectorTreeItemData) => void;
}
export declare const DualListSelectorTree: React.FunctionComponent<DualListSelectorTreeProps>;
//# sourceMappingURL=DualListSelectorTree.d.ts.map