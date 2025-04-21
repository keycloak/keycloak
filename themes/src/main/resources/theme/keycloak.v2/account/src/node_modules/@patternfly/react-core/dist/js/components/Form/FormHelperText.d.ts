import * as React from 'react';
export interface FormHelperTextProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Helper Text Item */
    children?: React.ReactNode;
    /** Adds error styling to the Helper Text  * */
    isError?: boolean;
    /** Hides the helper text * */
    isHidden?: boolean;
    /** Additional classes added to the Helper Text Item  */
    className?: string;
    /** Icon displayed to the left of the helper text. */
    icon?: React.ReactNode;
    /** Component type of the form helper text */
    component?: 'p' | 'div';
}
export declare const FormHelperText: React.FunctionComponent<FormHelperTextProps>;
//# sourceMappingURL=FormHelperText.d.ts.map