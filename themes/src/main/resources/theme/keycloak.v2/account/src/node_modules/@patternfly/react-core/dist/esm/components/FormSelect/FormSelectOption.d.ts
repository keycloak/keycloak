import * as React from 'react';
export interface FormSelectOptionProps extends Omit<React.HTMLProps<HTMLOptionElement>, 'disabled'> {
    /** additional classes added to the Select Option */
    className?: string;
    /** the value for the option */
    value?: any;
    /** the label for the option */
    label: string;
    /** flag indicating if the option is disabled */
    isDisabled?: boolean;
    /** flag indicating if option will have placeholder styling applied when selected **/
    isPlaceholder?: boolean;
}
export declare const FormSelectOption: React.FunctionComponent<FormSelectOptionProps>;
//# sourceMappingURL=FormSelectOption.d.ts.map