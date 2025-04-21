import * as React from 'react';
export interface PageNavigationProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes to apply to the PageNavigation */
    className?: string;
    /** Content rendered inside of the PageNavigation */
    children?: React.ReactNode;
    /** Limits the width of the PageNavigation */
    isWidthLimited?: boolean;
    /** Modifier indicating if the PageNavigation is sticky to the top or bottom */
    sticky?: 'top' | 'bottom';
    /** Flag indicating if PageNavigation should have a shadow at the top */
    hasShadowTop?: boolean;
    /** Flag indicating if PageNavigation should have a shadow at the bottom */
    hasShadowBottom?: boolean;
    /** Flag indicating if the PageNavigation has a scrolling overflow */
    hasOverflowScroll?: boolean;
}
export declare const PageNavigation: {
    ({ className, children, isWidthLimited, sticky, hasShadowTop, hasShadowBottom, hasOverflowScroll, ...props }: PageNavigationProps): JSX.Element;
    displayName: string;
};
//# sourceMappingURL=PageNavigation.d.ts.map