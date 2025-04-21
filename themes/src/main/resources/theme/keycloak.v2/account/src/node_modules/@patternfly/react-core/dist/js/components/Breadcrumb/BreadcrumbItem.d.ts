import * as React from 'react';
export interface BreadcrumbItemRenderArgs {
    className: string;
    ariaCurrent: 'page' | undefined;
}
export interface BreadcrumbItemProps extends React.HTMLProps<HTMLLIElement> {
    /** Content rendered inside the breadcrumb item. */
    children?: React.ReactNode;
    /** Additional classes added to the breadcrumb item. */
    className?: string;
    /** HREF for breadcrumb link. */
    to?: string;
    /** Flag indicating whether the item is active. */
    isActive?: boolean;
    /** Flag indicating whether the item contains a dropdown. */
    isDropdown?: boolean;
    /** Internal prop set by Breadcrumb on all but the first crumb */
    showDivider?: boolean;
    /** Target for breadcrumb link. */
    target?: string;
    /** Sets the base component to render. Defaults to <a> */
    component?: React.ElementType;
    /** A render function to render the component inside the breadcrumb item. */
    render?: (props: BreadcrumbItemRenderArgs) => React.ReactNode;
}
export declare const BreadcrumbItem: React.FunctionComponent<BreadcrumbItemProps>;
//# sourceMappingURL=BreadcrumbItem.d.ts.map