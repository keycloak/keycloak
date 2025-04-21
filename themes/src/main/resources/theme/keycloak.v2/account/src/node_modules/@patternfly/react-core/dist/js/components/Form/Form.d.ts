import * as React from 'react';
export interface FormProps extends React.HTMLProps<HTMLFormElement> {
    /** Anything that can be rendered as Form content. */
    children?: React.ReactNode;
    /** Additional classes added to the Form. */
    className?: string;
    /** Sets the Form to horizontal. */
    isHorizontal?: boolean;
    /** Limits the max-width of the form. */
    isWidthLimited?: boolean;
    /** Sets a custom max-width for the form. */
    maxWidth?: string;
}
export declare const Form: React.FunctionComponent<FormProps>;
//# sourceMappingURL=Form.d.ts.map