import * as React from 'react';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import styles from '@patternfly/react-styles/css/components/Breadcrumb/breadcrumb';
import { css, getModifier } from '@patternfly/react-styles';

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

export const BreadcrumbItem: React.FunctionComponent<BreadcrumbItemProps> = ({
  children = null,
  className = '',
  to = null,
  isActive = false,
  target = null,
  component = 'a',
  ...props
}: BreadcrumbItemProps) => {
  const Component = component as any;
  return (
    <li {...props} className={css(styles.breadcrumbItem, className)}>
      {to && (
        <Component
          href={to}
          target={target}
          className={css(styles.breadcrumbLink, isActive ? getModifier(styles, 'current') : '')}
          aria-current={isActive ? 'page' : undefined}
        >
          {children}
        </Component>
      )}
      {!to && <React.Fragment>{children}</React.Fragment>}
      {!isActive && (
        <span className={css(styles.breadcrumbItemDivider)}>
          <AngleRightIcon />
        </span>
      )}
    </li>
  );
};
