import * as React from 'react';
export interface FormSelectOptionGroupProps extends Omit<React.HTMLProps<HTMLOptGroupElement>, 'disabled'> {
    /** content rendered inside the Select Option Group */
    children?: React.ReactNode;
    /** additional classes added to the Select Option */
    className?: string;
    /** the label for the option */
    label: string;
    /** flag indicating if the Option Group is disabled */
    isDisabled?: boolean;
}
export declare const FormSelectOptionGroup: React.FunctionComponent<FormSelectOptionGroupProps>;
//# sourceMappingURL=FormSelectOptionGroup.d.ts.map