import * as React from 'react';
export interface BreadcrumbItemProps extends React.HTMLProps<HTMLLIElement> {
    /** Content rendered inside the breadcrumb item. */
    children?: React.ReactNode;
    /** Additional classes added to the breadcrumb item. */
    className?: string;
    /** HREF for breadcrumb link. */
    to?: string;
    /** Flag indicating whether the item is active. */
    isActive?: boolean;
    /** Target for breadcrumb link. */
    target?: string;
    /** Sets the base component to render. Defaults to <a> */
    component?: React.ReactNode;
}
export declare const BreadcrumbItem: React.FunctionComponent<BreadcrumbItemProps>;
