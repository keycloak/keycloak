import * as React from 'react';
export interface FormFieldGroupExpandableProps extends React.HTMLProps<HTMLDivElement> {
    /** Anything that can be rendered as form field group content. */
    children?: React.ReactNode;
    /** Additional classes added to the form field group. */
    className?: string;
    /** Form field group header */
    header?: React.ReactNode;
    /** Flag indicating if the form field group is initially expanded */
    isExpanded?: boolean;
    /** Aria-label to use on the form field group toggle button */
    toggleAriaLabel?: string;
}
export declare const FormFieldGroupExpandable: React.FunctionComponent<FormFieldGroupExpandableProps>;
//# sourceMappingURL=FormFieldGroupExpandable.d.ts.map