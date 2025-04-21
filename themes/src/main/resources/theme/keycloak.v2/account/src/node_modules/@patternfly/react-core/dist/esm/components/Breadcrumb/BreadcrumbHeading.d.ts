import * as React from 'react';
export interface BreadcrumbHeadingProps extends React.HTMLProps<HTMLLIElement> {
    /** Content rendered inside the breadcrumb title. */
    children?: React.ReactNode;
    /** Additional classes added to the breadcrumb item. */
    className?: string;
    /** HREF for breadcrumb link. */
    to?: string;
    /** Target for breadcrumb link. */
    target?: string;
    /** Sets the base component to render. Defaults to <a> */
    component?: React.ReactNode;
    /** Internal prop set by Breadcrumb on all but the first crumb */
    showDivider?: boolean;
}
export declare const BreadcrumbHeading: React.FunctionComponent<BreadcrumbHeadingProps>;
//# sourceMappingURL=BreadcrumbHeading.d.ts.map