import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Login/login';
import { css } from '@patternfly/react-styles';

export interface LoginMainFooterLinksItemProps extends React.HTMLProps<HTMLLIElement> {
  /** Content rendered inside the footer Link Item */
  children?: React.ReactNode;
  /** HREF for Footer Link Item */
  href?: string;
  /** Target for Footer Link Item */
  target?: string;
  /** Additional classes added to the Footer Link Item  */
  className?: string;
  /** Component used to render the Footer Link Item */
  linkComponent?: React.ReactNode;
  /** Props for the LinkComponent */
  linkComponentProps?: any;
}

export const LoginMainFooterLinksItem: React.FunctionComponent<LoginMainFooterLinksItemProps> = ({
  children = null,
  href = '',
  target = '',
  className = '',
  linkComponent = 'a',
  linkComponentProps,
  ...props
}: LoginMainFooterLinksItemProps) => {
  const LinkComponent = linkComponent as any;

  return (
    <li className={css(styles.loginMainFooterLinksItem, className)} {...props}>
      <LinkComponent
        className={css(styles.loginMainFooterLinksItemLink)}
        href={href}
        target={target}
        {...linkComponentProps}
      >
        {children}
      </LinkComponent>
    </li>
  );
};
LoginMainFooterLinksItem.displayName = 'LoginMainFooterLinksItem';
