import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Breadcrumb/breadcrumb';
import { css } from '@patternfly/react-styles';

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
}

export const BreadcrumbHeading: React.FunctionComponent<BreadcrumbHeadingProps> = ({
  children = null,
  className = '',
  to = null,
  target = null,
  component = 'a',
  ...props
}: BreadcrumbHeadingProps) => {
  const Component = component as any;
  return (
    <li {...props} className={css(styles.breadcrumbItem, className)}>
      <h1 className={css(styles.breadcrumbHeading)}>
        {to && (
          <Component
            href={to}
            target={target}
            className={css(styles.breadcrumbLink, styles.modifiers.current)}
            aria-current="page"
          >
            {children}
          </Component>
        )}
        {!to && <React.Fragment>{children}</React.Fragment>}
      </h1>
    </li>
  );
};
