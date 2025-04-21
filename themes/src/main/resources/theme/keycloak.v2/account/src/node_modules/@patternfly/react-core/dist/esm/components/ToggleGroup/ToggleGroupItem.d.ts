import * as React from 'react';
export interface ToggleGroupItemProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onChange'> {
    /** Text rendered inside the toggle group item */
    text?: React.ReactNode;
    /** Icon rendered inside the toggle group item */
    icon?: React.ReactNode;
    /** Additional classes added to the toggle group item */
    className?: string;
    /** Flag indicating if the toggle group item is disabled */
    isDisabled?: boolean;
    /** Flag indicating if the toggle group item is selected */
    isSelected?: boolean;
    /** required when icon is used with no supporting text */
    'aria-label'?: string;
    /** Optional id for the button within the toggle group item */
    buttonId?: string;
    /** A callback for when the toggle group item selection changes. */
    onChange?: (selected: boolean, event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent) => void;
}
export declare const ToggleGroupItem: React.FunctionComponent<ToggleGroupItemProps>;
//# sourceMappingURL=ToggleGroupItem.d.ts.map