import * as React from 'react';
import { OUIAProps } from '../../helpers';
export interface BreadcrumbProps extends React.HTMLProps<HTMLElement>, OUIAProps {
    /** Children nodes be rendered to the BreadCrumb. Should be of type BreadCrumbItem. */
    children?: React.ReactNode;
    /** Additional classes added to the breadcrumb nav. */
    className?: string;
    /** Aria label added to the breadcrumb nav. */
    'aria-label'?: string;
}
export declare const Breadcrumb: React.FunctionComponent<BreadcrumbProps>;
//# sourceMappingURL=Breadcrumb.d.ts.map