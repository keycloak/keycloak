import * as React from 'react';
export interface OverflowMenuControlProps extends React.HTMLProps<HTMLDivElement> {
    /** Any elements that can be rendered in the menu */
    children?: any;
    /** Additional classes added to the OverflowMenuControl */
    className?: string;
    /** Triggers the overflow dropdown to persist at all viewport sizes */
    hasAdditionalOptions?: boolean;
}
export declare const OverflowMenuControl: React.FunctionComponent<OverflowMenuControlProps>;
//# sourceMappingURL=OverflowMenuControl.d.ts.map