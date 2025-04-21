import * as React from 'react';
export interface AccordionContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Accordion  */
    children?: React.ReactNode;
    /** Additional classes added to the Accordion content  */
    className?: string;
    /** Identify the AccordionContent item  */
    id?: string;
    /** Flag to show if the expanded content of the Accordion item is visible  */
    isHidden?: boolean;
    /** Flag to indicate Accordion content is fixed  */
    isFixed?: boolean;
    /** Adds accessible text to the Accordion content */
    'aria-label'?: string;
    /** Component to use as content container */
    component?: React.ElementType;
    /** Flag indicating content is custom. Expanded content Body wrapper will be removed from children.  This allows multiple bodies to be rendered as content. */
    isCustomContent?: React.ReactNode;
}
export declare const AccordionContent: React.FunctionComponent<AccordionContentProps>;
//# sourceMappingURL=AccordionContent.d.ts.map