import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Banner/banner';
import { css } from '@patternfly/react-styles';

export interface BannerProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the banner */
  children?: React.ReactNode;
  /** Additional classes added to the banner */
  className?: string;
  /** Variant styles for the banner */
  variant?: 'default' | 'info' | 'danger' | 'success' | 'warning';
  /** If set to true, the banner sticks to the top of its container */
  isSticky?: boolean;
  /** Text announced by screen readers to indicate the type of banner. Defaults to "${variant} banner" if this prop is not passed in */
  screenReaderText?: string;
}

export const Banner: React.FunctionComponent<BannerProps> = ({
  children,
  className,
  variant = 'default',
  screenReaderText,
  isSticky = false,
  ...props
}: BannerProps) => (
  <div
    className={css(
      styles.banner,
      styles.modifiers[variant as 'success' | 'danger' | 'warning' | 'info'],
      isSticky && styles.modifiers.sticky,
      className
    )}
    {...props}
  >
    {children}
    <span className="pf-u-screen-reader">{screenReaderText || `${variant} banner`}</span>
  </div>
);
Banner.displayName = 'Banner';
