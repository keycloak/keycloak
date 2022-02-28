import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface FormSelectProps extends Omit<React.HTMLProps<HTMLSelectElement>, 'onChange' | 'onBlur' | 'onFocus' | 'disabled'> {
    /** content rendered inside the FormSelect */
    children: React.ReactNode;
    /** additional classes added to the FormSelect control */
    className?: string;
    /** value of selected option */
    value?: any;
    /** Flag indicating selection is valid. This prop will be deprecated. You should use validated instead. */
    isValid?: boolean;
    validated?: 'success' | 'error' | 'default';
    /** Flag indicating the FormSelect is disabled */
    isDisabled?: boolean;
    /** Sets the FormSelectrequired. */
    isRequired?: boolean;
    /** Optional callback for updating when selection loses focus */
    onBlur?: (event: React.FormEvent<HTMLSelectElement>) => void;
    /** Optional callback for updating when selection gets focus */
    onFocus?: (event: React.FormEvent<HTMLSelectElement>) => void;
    /** Optional callback for updating when selection changes */
    onChange?: (value: string, event: React.FormEvent<HTMLSelectElement>) => void;
    /** Custom flag to show that the FormSelect requires an associated id or aria-label. */
    'aria-label'?: string;
}
export declare class FormSelect extends React.Component<FormSelectProps> {
    constructor(props: FormSelectProps);
    static defaultProps: PickOptional<FormSelectProps>;
    handleChange: (event: any) => void;
    render(): JSX.Element;
}
