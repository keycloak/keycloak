import * as React from 'react';
export interface SelectGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Checkboxes within group. Must be React.ReactElement<SelectOptionProps>[] */
    children?: React.ReactNode;
    /** Additional classes added to the CheckboxSelectGroup control */
    className?: string;
    /** Group label */
    label?: string;
    /** ID for title label */
    titleId?: string;
}
export declare const SelectGroup: React.FunctionComponent<SelectGroupProps>;
//# sourceMappingURL=SelectGroup.d.ts.map