import * as React from 'react';
import { InjectedOuiaProps } from '../withOuia';
export interface BreadcrumbProps extends React.HTMLProps<HTMLElement> {
    /** Children nodes be rendered to the BreadCrumb. Should be of type BreadCrumbItem. */
    children?: React.ReactNode;
    /** Additional classes added to the breadcrumb nav. */
    className?: string;
    /** Aria label added to the breadcrumb nav. */
    'aria-label'?: string;
}
declare const BreadcrumbWithOuiaContext: React.FunctionComponent<BreadcrumbProps & InjectedOuiaProps>;
export { BreadcrumbWithOuiaContext as Breadcrumb };
