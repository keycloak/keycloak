import * as React from 'react';
import { DualListSelectorTreeItemData } from './DualListSelectorTree';
export interface DualListSelectorTreeItemProps extends React.HTMLProps<HTMLLIElement> {
    /** Content rendered inside the dual list selector. */
    children?: React.ReactNode;
    /** Additional classes applied to the dual list selector. */
    className?: string;
    /** Flag indicating this option is expanded by default. */
    defaultExpanded?: boolean;
    /** Flag indicating this option has a badge */
    hasBadge?: boolean;
    /** Callback fired when an option is checked */
    onOptionCheck?: (event: React.MouseEvent | React.ChangeEvent<HTMLInputElement> | React.KeyboardEvent, isChecked: boolean, itemData: DualListSelectorTreeItemData) => void;
    /** ID of the option */
    id: string;
    /** Text of the option */
    text: string;
    /** Flag indicating if this open is checked. */
    isChecked?: boolean;
    /** Additional properties to pass to the option checkbox */
    checkProps?: any;
    /** Additional properties to pass to the option badge */
    badgeProps?: any;
    /** Raw data of the option */
    itemData?: DualListSelectorTreeItemData;
    /** Flag indicating whether the component is disabled. */
    isDisabled?: boolean;
    /** Flag indicating the DualListSelector tree should utilize memoization to help render large data sets. */
    useMemo?: boolean;
}
export declare const DualListSelectorTreeItem: React.NamedExoticComponent<DualListSelectorTreeItemProps>;
//# sourceMappingURL=DualListSelectorTreeItem.d.ts.map