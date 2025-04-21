import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';

export interface WizardNavItemProps {
  /** Can nest a WizardNav component for substeps */
  children?: React.ReactNode;
  /** The content to display in the nav item */
  content?: React.ReactNode;
  /** Whether the nav item is the currently active item */
  isCurrent?: boolean;
  /** Whether the nav item is disabled */
  isDisabled?: boolean;
  /** The step passed into the onNavItemClick callback */
  step: number;
  /** Callback for when the nav item is clicked */
  onNavItemClick?: (step: number) => any;
  /** Component used to render WizardNavItem */
  navItemComponent?: 'button' | 'a';
  /** An optional url to use for when using an anchor component */
  href?: string;
  /** Flag indicating that this NavItem has child steps and is expandable */
  isExpandable?: boolean;
  /** The id for the nav item */
  id?: string | number;
}

export const WizardNavItem: React.FunctionComponent<WizardNavItemProps> = ({
  children = null,
  content = '',
  isCurrent = false,
  isDisabled = false,
  step,
  onNavItemClick = () => undefined,
  navItemComponent = 'button',
  href = null,
  isExpandable = false,
  id,
  ...rest
}: WizardNavItemProps) => {
  const NavItemComponent = navItemComponent;

  const [isExpanded, setIsExpanded] = React.useState(false);

  React.useEffect(() => {
    setIsExpanded(isCurrent);
  }, [isCurrent]);

  if (navItemComponent === 'a' && !href && process.env.NODE_ENV !== 'production') {
    // eslint-disable-next-line no-console
    console.error('WizardNavItem: When using an anchor, please provide an href');
  }

  const btnProps = {
    disabled: isDisabled
  };

  const linkProps = {
    tabIndex: isDisabled ? -1 : undefined,
    href
  };

  return (
    <li
      className={css(
        styles.wizardNavItem,
        isExpandable && styles.modifiers.expandable,
        isExpandable && isExpanded && styles.modifiers.expanded
      )}
    >
      <NavItemComponent
        {...rest}
        {...(navItemComponent === 'a' ? { ...linkProps } : { ...btnProps })}
        {...(id && { id: id.toString() })}
        onClick={() => (isExpandable ? setIsExpanded(!isExpanded || isCurrent) : onNavItemClick(step))}
        className={css(
          styles.wizardNavLink,
          isCurrent && styles.modifiers.current,
          isDisabled && styles.modifiers.disabled
        )}
        aria-disabled={isDisabled ? true : null}
        aria-current={isCurrent && !children ? 'step' : false}
        {...(isExpandable && { 'aria-expanded': isExpanded })}
      >
        {isExpandable ? (
          <>
            <span className={css(styles.wizardNavLinkText)}>{content}</span>
            <span className={css(styles.wizardNavLinkToggle)}>
              <span className={css(styles.wizardNavLinkToggleIcon)}>
                <AngleRightIcon />
              </span>
            </span>
          </>
        ) : (
          content
        )}
      </NavItemComponent>
      {children}
    </li>
  );
};
WizardNavItem.displayName = 'WizardNavItem';
