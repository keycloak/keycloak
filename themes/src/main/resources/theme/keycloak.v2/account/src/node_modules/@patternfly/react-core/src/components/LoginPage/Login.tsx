import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Login/login';
import { css } from '@patternfly/react-styles';

export interface LoginProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the main section of the login layout */
  children?: React.ReactNode;
  /** Additional classes added to the login layout */
  className?: string;
  /** Footer component (e.g. <LoginFooter />) */
  footer?: React.ReactNode;
  /** Header component (e.g. <LoginHeader />) */
  header?: React.ReactNode;
}

export const Login: React.FunctionComponent<LoginProps> = ({
  className = '',
  children = null,
  footer = null,
  header = null,
  ...props
}: LoginProps) => (
  <div {...props} className={css(styles.login, className)}>
    <div className={css(styles.loginContainer)}>
      {header}
      <main className={css(styles.loginMain)}>{children}</main>
      {footer}
    </div>
  </div>
);
Login.displayName = 'Login';
