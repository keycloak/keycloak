import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AppLauncher/app-launcher';

export interface ApplicationLauncherIconProps extends React.HTMLProps<HTMLSpanElement> {
  /** content rendered inside the icon container */
  children: React.ReactNode;
  /** Additional classes added to the icon container */
  className?: string;
}

export const ApplicationLauncherIcon: React.FunctionComponent<ApplicationLauncherIconProps> = ({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  className = '',
  children,
  ...props
}: ApplicationLauncherIconProps) => (
  <span className={css(styles.appLauncherMenuItemIcon)} {...props}>
    {children}
  </span>
);
ApplicationLauncherIcon.displayName = 'ApplicationLauncherIcon';
