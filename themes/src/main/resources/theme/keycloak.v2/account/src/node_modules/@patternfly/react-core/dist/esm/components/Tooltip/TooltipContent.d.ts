import * as React from 'react';
export interface TooltipContentProps extends React.HTMLProps<HTMLDivElement> {
    /** PopoverContent additional class */
    className?: string;
    /** PopoverContent content */
    children: React.ReactNode;
    /** Flag to align text to the left */
    isLeftAligned?: boolean;
}
export declare const TooltipContent: React.FunctionComponent<TooltipContentProps>;
//# sourceMappingURL=TooltipContent.d.ts.map