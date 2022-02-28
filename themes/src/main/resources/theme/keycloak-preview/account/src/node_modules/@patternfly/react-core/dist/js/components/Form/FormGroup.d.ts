import * as React from 'react';
export interface FormGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label'> {
    /** Anything that can be rendered as FormGroup content. */
    children?: React.ReactNode;
    /** Additional classes added to the FormGroup. */
    className?: string;
    /** Label text before the field. */
    label?: React.ReactNode;
    /** Sets the FormGroup required. */
    isRequired?: boolean;
    /** Sets the FormGroup isValid. This prop will be deprecated. You should use validated instead. */
    isValid?: boolean;
    /** Sets the FormGroup validated. If you set to success, text color of helper text will be modified to indicate valid state.
     * If set to error,  text color of helper text will be modified to indicate error state.
     */
    validated?: 'success' | 'error' | 'default';
    /** Sets the FormGroup isInline. */
    isInline?: boolean;
    /** Helper text after the field. It can be a simple text or an object. */
    helperText?: React.ReactNode;
    /** Helper text after the field when the field is invalid. It can be a simple text or an object. */
    helperTextInvalid?: React.ReactNode;
    /** ID of the included field. It has to be the same for proper working. */
    fieldId: string;
}
export declare const FormGroup: React.FunctionComponent<FormGroupProps>;
