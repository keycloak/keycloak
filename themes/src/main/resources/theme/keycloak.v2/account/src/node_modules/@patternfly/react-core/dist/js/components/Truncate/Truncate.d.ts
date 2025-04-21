import * as React from 'react';
import { TooltipPosition } from '../Tooltip';
export declare enum TruncatePosition {
    start = "start",
    end = "end",
    middle = "middle"
}
interface TruncateProps extends React.HTMLProps<HTMLSpanElement> {
    /** Class to add to outer span */
    className?: string;
    /** Text to truncate */
    content: string;
    /** The number of characters displayed in the second half of the truncation */
    trailingNumChars?: number;
    /** Where the text will be truncated */
    position?: 'start' | 'middle' | 'end';
    /** Tooltip position */
    tooltipPosition?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
}
export declare const Truncate: React.FunctionComponent<TruncateProps>;
export {};
//# sourceMappingURL=Truncate.d.ts.map