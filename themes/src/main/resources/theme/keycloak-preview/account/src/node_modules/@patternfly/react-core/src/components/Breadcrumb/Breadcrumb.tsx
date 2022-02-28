import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Breadcrumb/breadcrumb';
import { css } from '@patternfly/react-styles';
import { InjectedOuiaProps, withOuiaContext } from '../withOuia';

export interface BreadcrumbProps extends React.HTMLProps<HTMLElement> {
  /** Children nodes be rendered to the BreadCrumb. Should be of type BreadCrumbItem. */
  children?: React.ReactNode;
  /** Additional classes added to the breadcrumb nav. */
  className?: string;
  /** Aria label added to the breadcrumb nav. */
  'aria-label'?: string;
}

const Breadcrumb: React.FunctionComponent<BreadcrumbProps & InjectedOuiaProps> = ({
  children = null,
  className = '',
  'aria-label': ariaLabel = 'Breadcrumb',
  ouiaContext = null,
  ouiaId = null,
  ...props
}: BreadcrumbProps & InjectedOuiaProps) => (
  <nav
    {...props}
    aria-label={ariaLabel}
    className={css(styles.breadcrumb, className)}
    {...(ouiaContext.isOuia && {
      'data-ouia-component-type': 'Breadcrumb',
      'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
    })}
  >
    <ol className={css(styles.breadcrumbList)}>{children}</ol>
  </nav>
);

const BreadcrumbWithOuiaContext = withOuiaContext(Breadcrumb);
export { BreadcrumbWithOuiaContext as Breadcrumb };
