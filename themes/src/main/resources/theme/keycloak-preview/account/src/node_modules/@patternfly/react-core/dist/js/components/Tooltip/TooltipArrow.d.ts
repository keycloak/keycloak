import * as React from 'react';
export interface TooltipArrowProps extends React.HTMLProps<HTMLDivElement> {
    /** className */
    className?: string;
}
export declare const TooltipArrow: ({ className, ...props }: TooltipArrowProps) => JSX.Element;
