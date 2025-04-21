import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
import { OUIAProps } from '../../helpers';
export interface CheckboxProps extends Omit<React.HTMLProps<HTMLInputElement>, 'type' | 'onChange' | 'disabled' | 'label'>, OUIAProps {
    /** Additional classes added to the Checkbox. */
    className?: string;
    /** Flag to show if the Checkbox selection is valid or invalid. */
    isValid?: boolean;
    /** Flag to show if the Checkbox is disabled. */
    isDisabled?: boolean;
    /** Flag to show if the Checkbox is checked. If null, the checkbox will be indeterminate (partially checked). */
    isChecked?: boolean | null;
    checked?: boolean;
    /** A callback for when the Checkbox selection changes. */
    onChange?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
    /** Label text of the checkbox. */
    label?: React.ReactNode;
    /** Id of the checkbox. */
    id: string;
    /** Aria-label of the checkbox. */
    'aria-label'?: string;
    /** Description text of the checkbox. */
    description?: React.ReactNode;
    /** Body text of the checkbox */
    body?: React.ReactNode;
    /** Sets the input wrapper component to render. Defaults to <div> */
    component?: React.ElementType;
}
interface CheckboxState {
    ouiaStateId: string;
}
export declare class Checkbox extends React.Component<CheckboxProps, CheckboxState> {
    static displayName: string;
    static defaultProps: PickOptional<CheckboxProps>;
    constructor(props: any);
    private handleChange;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=Checkbox.d.ts.map