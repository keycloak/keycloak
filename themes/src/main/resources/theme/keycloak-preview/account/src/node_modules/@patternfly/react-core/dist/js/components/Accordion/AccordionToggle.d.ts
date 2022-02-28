import * as React from 'react';
export interface AccordionToggleProps extends Omit<React.HTMLProps<HTMLButtonElement>, 'type'> {
    /** Content rendered inside the Accordion toggle  */
    children?: React.ReactNode;
    /** Additional classes added to the Accordion Toggle  */
    className?: string;
    /** Flag to show if the expanded content of the Accordion item is visible  */
    isExpanded?: boolean;
    /** Identify the Accordion toggle number  */
    id: string;
    /** Container to override the default for toggle */
    component?: React.ElementType;
}
export declare const AccordionToggle: React.FunctionComponent<AccordionToggleProps>;
