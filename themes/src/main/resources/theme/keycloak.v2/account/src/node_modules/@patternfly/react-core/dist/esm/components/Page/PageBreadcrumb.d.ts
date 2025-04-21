import * as React from 'react';
export interface PageBreadcrumbProps extends React.HTMLProps<HTMLElement> {
    /** Additional classes to apply to the PageBreadcrumb */
    className?: string;
    /** Content rendered inside of the PageBreadcrumb */
    children?: React.ReactNode;
    /** Limits the width of the breadcrumb */
    isWidthLimited?: boolean;
    /** Modifier indicating if the PageBreadcrumb is sticky to the top or bottom */
    sticky?: 'top' | 'bottom';
    /** Flag indicating if PageBreadcrumb should have a shadow at the top */
    hasShadowTop?: boolean;
    /** Flag indicating if PageBreadcrumb should have a shadow at the bottom */
    hasShadowBottom?: boolean;
    /** Flag indicating if the PageBreadcrumb has a scrolling overflow */
    hasOverflowScroll?: boolean;
}
export declare const PageBreadcrumb: {
    ({ className, children, isWidthLimited, sticky, hasShadowTop, hasShadowBottom, hasOverflowScroll, ...props }: PageBreadcrumbProps): JSX.Element;
    displayName: string;
};
//# sourceMappingURL=PageBreadcrumb.d.ts.map