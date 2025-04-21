import * as React from 'react';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
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
  /** Internal prop set by Breadcrumb on all but the first crumb */
  showDivider?: boolean;
}

export const BreadcrumbHeading: React.FunctionComponent<BreadcrumbHeadingProps> = ({
  children = null,
  className = '',
  to = undefined,
  target = undefined,
  component = 'a',
  showDivider,
  ...props
}: BreadcrumbHeadingProps) => {
  const Component = component as any;
  return (
    <li {...props} className={css(styles.breadcrumbItem, className)}>
      {showDivider && (
        <span className={styles.breadcrumbItemDivider}>
          <AngleRightIcon />
        </span>
      )}
      <h1 className={styles.breadcrumbHeading}>
        {!to && component === 'button' && (
          <button className={css(styles.breadcrumbLink, styles.modifiers.current)} aria-current type="button">
            {children}
          </button>
        )}
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
        {!to && component !== 'button' && <React.Fragment>{children}</React.Fragment>}
      </h1>
    </li>
  );
};
BreadcrumbHeading.displayName = 'BreadcrumbHeading';
