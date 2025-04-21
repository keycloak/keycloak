import * as React from 'react';
export interface AccordionProps extends React.HTMLProps<HTMLDListElement> {
    /** Content rendered inside the Accordion  */
    children?: React.ReactNode;
    /** Additional classes added to the Accordion  */
    className?: string;
    /** Adds accessible text to the Accordion */
    'aria-label'?: string;
    /** Heading level to use */
    headingLevel?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
    /** Flag to indicate whether use definition list or div */
    asDefinitionList?: boolean;
    /** Flag to indicate the accordion had a border */
    isBordered?: boolean;
    /** Display size variant. */
    displaySize?: 'default' | 'large';
}
export declare const Accordion: React.FunctionComponent<AccordionProps>;
//# sourceMappingURL=Accordion.d.ts.map