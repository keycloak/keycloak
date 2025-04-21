import * as React from 'react';
export interface FormGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label'> {
    /** Anything that can be rendered as FormGroup content. */
    children?: React.ReactNode;
    /** Additional classes added to the FormGroup. */
    className?: string;
    /** Label text before the field. */
    label?: React.ReactNode;
    /** Additional label information displayed after the label. */
    labelInfo?: React.ReactNode;
    /** Sets an icon for the label. For providing additional context. Host element for Popover  */
    labelIcon?: React.ReactElement;
    /** Sets the FormGroup required. */
    isRequired?: boolean;
    /**
     * Sets the FormGroup validated. If you set to success, text color of helper text will be modified to indicate valid state.
     * If set to error, text color of helper text will be modified to indicate error state.
     * If set to warning, text color of helper text will be modified to indicate warning state.
     */
    validated?: 'success' | 'warning' | 'error' | 'default';
    /** Sets the FormGroup isInline. */
    isInline?: boolean;
    /** Sets the FormGroupControl to be stacked */
    isStack?: boolean;
    /** Removes top spacer from label. */
    hasNoPaddingTop?: boolean;
    /** Helper text regarding the field. It can be a simple text or an object. */
    helperText?: React.ReactNode;
    /** Flag to position the helper text before the field. False by default */
    isHelperTextBeforeField?: boolean;
    /** Helper text after the field when the field is invalid. It can be a simple text or an object. */
    helperTextInvalid?: React.ReactNode;
    /** Icon displayed to the left of the helper text. */
    helperTextIcon?: React.ReactNode;
    /** Icon displayed to the left of the helper text when the field is invalid. */
    helperTextInvalidIcon?: React.ReactNode;
    /** ID of an individual field or a group of multiple fields. Required when a role of "group" or "radiogroup" is passed in.
     * If only one field is included, its ID attribute and this prop must be the same.
     */
    fieldId?: string;
    /** Sets the role of the form group. Pass in "radiogroup" when the form group contains multiple
     * radio inputs, or pass in "group" when the form group contains multiple of any other input type.
     */
    role?: string;
}
export declare const FormGroup: React.FunctionComponent<FormGroupProps>;
//# sourceMappingURL=FormGroup.d.ts.map