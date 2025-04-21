import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MenuToggle/menu-toggle';
import { css } from '@patternfly/react-styles';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import { BadgeProps } from '../Badge';

export interface MenuToggleProps
  extends Omit<React.DetailedHTMLProps<React.ButtonHTMLAttributes<HTMLButtonElement>, HTMLButtonElement>, 'ref'> {
  /** Content rendered inside the toggle */
  children?: React.ReactNode;
  /** Additional classes added to the toggle */
  className?: string;
  /** Flag indicating the toggle has expanded styling */
  isExpanded?: boolean;
  /** Flag indicating the toggle is disabled */
  isDisabled?: boolean;
  /** Flag indicating the toggle is full height */
  isFullHeight?: boolean;
  /** Flag indicating the toggle takes up the full width of its parent */
  isFullWidth?: boolean;
  /** Variant styles of the menu toggle */
  variant?: 'default' | 'plain' | 'primary' | 'plainText' | 'secondary';
  /** Optional icon rendered inside the toggle, before the children content */
  icon?: React.ReactNode;
  /** Optional badge rendered inside the toggle, after the children content */
  badge?: BadgeProps | React.ReactNode;
  /** Forwarded ref */
  innerRef?: React.Ref<HTMLButtonElement>;
}

export class MenuToggleBase extends React.Component<MenuToggleProps> {
  displayName = 'MenuToggleBase';
  static defaultProps = {
    className: '',
    isExpanded: false,
    isDisabled: false,
    isFullWidth: false,
    isFullHeight: false,
    variant: 'default'
  };

  render() {
    const {
      children,
      className,
      icon,
      badge,
      isExpanded,
      isDisabled,
      isFullHeight,
      isFullWidth,
      variant,
      innerRef,
      ...props
    } = this.props;

    const isPlain = variant === 'plain';
    const isPlainText = variant === 'plainText';

    const content = (
      <React.Fragment>
        {icon && <span className={css(styles.menuToggleIcon)}>{icon}</span>}
        <span className="pf-c-menu-toggle__text">{children}</span>
        {badge && <span className={css(styles.menuToggleCount)}>{badge as React.ReactNode}</span>}
        <span className={css(styles.menuToggleControls)}>
          <span className={css(styles.menuToggleToggleIcon)}>
            <CaretDownIcon aria-hidden />
          </span>
        </span>
      </React.Fragment>
    );
    return (
      <button
        className={css(
          styles.menuToggle,
          isExpanded && styles.modifiers.expanded,
          variant === 'primary' && styles.modifiers.primary,
          variant === 'secondary' && styles.modifiers.secondary,
          (isPlain || isPlainText) && styles.modifiers.plain,
          isPlainText && styles.modifiers.text,
          isFullHeight && styles.modifiers.fullHeight,
          isFullWidth && styles.modifiers.fullWidth,
          className
        )}
        type="button"
        aria-expanded={false}
        ref={innerRef}
        {...(isExpanded && { 'aria-expanded': true })}
        {...(isDisabled && { disabled: true })}
        {...props}
      >
        {isPlain && children}
        {!isPlain && content}
      </button>
    );
  }
}

export const MenuToggle = React.forwardRef((props: MenuToggleProps, ref: React.Ref<HTMLButtonElement>) => (
  <MenuToggleBase innerRef={ref} {...props} />
));

MenuToggle.displayName = 'MenuToggle';
