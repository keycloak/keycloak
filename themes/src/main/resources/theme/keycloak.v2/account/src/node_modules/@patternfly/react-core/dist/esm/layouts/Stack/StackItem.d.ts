import * as React from 'react';
export interface StackItemProps extends React.HTMLProps<HTMLDivElement> {
    /** Flag indicating if this Stack Layout item should fill the available vertical space. */
    isFilled?: boolean;
    /** additional classes added to the Stack Layout Item */
    children?: React.ReactNode;
    /** content rendered inside the Stack Layout Item */
    className?: string;
}
export declare const StackItem: React.FunctionComponent<StackItemProps>;
//# sourceMappingURL=StackItem.d.ts.map