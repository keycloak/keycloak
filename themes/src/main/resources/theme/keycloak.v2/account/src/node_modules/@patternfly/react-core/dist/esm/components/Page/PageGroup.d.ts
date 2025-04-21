import * as React from 'react';
export interface PageGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes to apply to the PageGroup */
    className?: string;
    /** Content rendered inside of the PageGroup */
    children?: React.ReactNode;
    /** Modifier indicating if PageGroup is sticky to the top or bottom */
    sticky?: 'top' | 'bottom';
    /** Modifier indicating if PageGroup should have a shadow at the top */
    hasShadowTop?: boolean;
    /** Modifier indicating if PageGroup should have a shadow at the bottom */
    hasShadowBottom?: boolean;
    /** Flag indicating if the PageGroup has a scrolling overflow */
    hasOverflowScroll?: boolean;
}
export declare const PageGroup: {
    ({ className, children, sticky, hasShadowTop, hasShadowBottom, hasOverflowScroll, ...props }: PageGroupProps): JSX.Element;
    displayName: string;
};
//# sourceMappingURL=PageGroup.d.ts.map