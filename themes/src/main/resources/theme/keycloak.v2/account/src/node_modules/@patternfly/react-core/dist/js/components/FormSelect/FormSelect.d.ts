import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
import { OUIAProps } from '../../helpers';
export interface FormSelectProps extends Omit<React.HTMLProps<HTMLSelectElement>, 'onChange' | 'onBlur' | 'onFocus' | 'disabled'>, OUIAProps {
    /** content rendered inside the FormSelect */
    children: React.ReactNode;
    /** additional classes added to the FormSelect control */
    className?: string;
    /** value of selected option */
    value?: any;
    /** Value to indicate if the select is modified to show that validation state.
     * If set to success, select will be modified to indicate valid state.
     * If set to error, select will be modified to indicate error state.
     */
    validated?: 'success' | 'warning' | 'error' | 'default';
    /** Flag indicating the FormSelect is disabled */
    isDisabled?: boolean;
    /** Sets the FormSelect required. */
    isRequired?: boolean;
    /** Use the external file instead of a data URI */
    isIconSprite?: boolean;
    /** Optional callback for updating when selection loses focus */
    onBlur?: (event: React.FormEvent<HTMLSelectElement>) => void;
    /** Optional callback for updating when selection gets focus */
    onFocus?: (event: React.FormEvent<HTMLSelectElement>) => void;
    /** Optional callback for updating when selection changes */
    onChange?: (value: string, event: React.FormEvent<HTMLSelectElement>) => void;
    /** Custom flag to show that the FormSelect requires an associated id or aria-label. */
    'aria-label'?: string;
}
export declare class FormSelect extends React.Component<FormSelectProps, {
    ouiaStateId: string;
}> {
    static displayName: string;
    constructor(props: FormSelectProps);
    static defaultProps: PickOptional<FormSelectProps>;
    handleChange: (event: any) => void;
    render(): JSX.Element;
}
//# sourceMappingURL=FormSelect.d.ts.map