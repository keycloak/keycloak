import * as React from 'react';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import styles from '@patternfly/react-styles/css/components/Breadcrumb/breadcrumb';
import { css } from '@patternfly/react-styles';

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

export const BreadcrumbItem: React.FunctionComponent<BreadcrumbItemProps> = ({
  children = null,
  className: classNameProp = '',
  to = undefined,
  isActive = false,
  isDropdown = false,
  showDivider,
  target = undefined,
  component = 'a',
  render = undefined,
  ...props
}: BreadcrumbItemProps) => {
  const Component = component;
  const ariaCurrent = isActive ? 'page' : undefined;
  const className = css(styles.breadcrumbLink, isActive && styles.modifiers.current);
  return (
    <li {...props} className={css(styles.breadcrumbItem, classNameProp)}>
      {showDivider && (
        <span className={styles.breadcrumbItemDivider}>
          <AngleRightIcon />
        </span>
      )}
      {component === 'button' && (
        <button className={className} aria-current={ariaCurrent} type="button">
          {children}
        </button>
      )}
      {isDropdown && <span className={css(styles.breadcrumbDropdown)}>{children}</span>}
      {render && render({ className, ariaCurrent })}
      {to && !render && (
        <Component href={to} target={target} className={className} aria-current={ariaCurrent}>
          {children}
        </Component>
      )}
      {!to && component !== 'button' && !isDropdown && children}
    </li>
  );
};
BreadcrumbItem.displayName = 'BreadcrumbItem';
