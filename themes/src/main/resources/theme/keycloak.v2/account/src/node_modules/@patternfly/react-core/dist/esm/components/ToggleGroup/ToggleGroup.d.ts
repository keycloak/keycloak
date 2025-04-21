import * as React from 'react';
export interface ToggleGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the toggle group */
    children?: React.ReactNode;
    /** Additional classes added to the toggle group */
    className?: string;
    /** Modifies the toggle group to include compact styling. */
    isCompact?: boolean;
    /** Disable all toggle group items under this component. */
    areAllGroupsDisabled?: boolean;
    /** Accessible label for the toggle group */
    'aria-label'?: string;
}
export declare const ToggleGroup: React.FunctionComponent<ToggleGroupProps>;
//# sourceMappingURL=ToggleGroup.d.ts.map