import React from 'react';
export interface MenuBreadcrumbProps extends Omit<React.HTMLAttributes<HTMLDivElement>, 'ref' | 'onSelect'> {
    /** Items within breadcrumb menu container */
    children?: React.ReactNode;
}
export declare const MenuBreadcrumb: React.FunctionComponent<MenuBreadcrumbProps>;
//# sourceMappingURL=MenuBreadcrumb.d.ts.map