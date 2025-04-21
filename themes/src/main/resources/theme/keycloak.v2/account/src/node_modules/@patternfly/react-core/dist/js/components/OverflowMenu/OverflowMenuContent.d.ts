import * as React from 'react';
export interface OverflowMenuContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Any elements that can be rendered in the menu */
    children?: any;
    /** Additional classes added to the OverflowMenuContent */
    className?: string;
    /** Modifies the overflow menu content visibility */
    isPersistent?: boolean;
}
export declare const OverflowMenuContent: React.FunctionComponent<OverflowMenuContentProps>;
//# sourceMappingURL=OverflowMenuContent.d.ts.map