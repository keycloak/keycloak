import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';

export const NavItemSeparator: React.FunctionComponent<React.HTMLProps<HTMLLIElement>> = ({
  className = '',
  ...props
}: React.HTMLProps<HTMLLIElement>) => (
  <li className={css(styles.navSeparator, className)} role="separator" {...props} />
);
