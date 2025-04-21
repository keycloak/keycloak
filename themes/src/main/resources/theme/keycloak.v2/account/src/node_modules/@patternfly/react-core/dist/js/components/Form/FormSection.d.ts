import * as React from 'react';
export interface FormSectionProps extends Omit<React.HTMLProps<HTMLDivElement>, 'title'> {
    /** Content rendered inside the section */
    children?: React.ReactNode;
    /** Additional classes added to the section */
    className?: string;
    /** Title for the section */
    title?: React.ReactNode;
    /** Element to wrap the section title*/
    titleElement?: 'div' | 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}
export declare const FormSection: React.FunctionComponent<FormSectionProps>;
//# sourceMappingURL=FormSection.d.ts.map