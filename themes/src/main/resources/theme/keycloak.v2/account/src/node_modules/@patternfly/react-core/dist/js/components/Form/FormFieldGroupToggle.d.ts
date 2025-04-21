import * as React from 'react';
export interface FormFieldGroupToggleProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the section */
    className?: string;
    /** Callback for onClick */
    onToggle: () => void;
    /** Flag indicating if the toggle is expanded */
    isExpanded: boolean;
    /** Aria label of the toggle button */
    'aria-label'?: string;
    /** Sets the aria-labelledby attribute on the toggle button element */
    'aria-labelledby'?: string;
    /** The id applied to the toggle button */
    toggleId?: string;
}
export declare const FormFieldGroupToggle: React.FunctionComponent<FormFieldGroupToggleProps>;
//# sourceMappingURL=FormFieldGroupToggle.d.ts.map