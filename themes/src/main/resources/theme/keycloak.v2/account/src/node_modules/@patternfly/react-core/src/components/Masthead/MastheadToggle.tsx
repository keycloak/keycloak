import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Masthead/masthead';
import { css } from '@patternfly/react-styles';

export interface MastheadToggleProps extends React.DetailedHTMLProps<React.HTMLProps<HTMLDivElement>, HTMLDivElement> {
  /** Content rendered inside of the masthead toggle. */
  children?: React.ReactNode;
  /** Additional classes added to the masthead toggle. */
  className?: string;
}

export const MastheadToggle: React.FunctionComponent<MastheadToggleProps> = ({
  children,
  className,
  ...props
}: MastheadToggleProps) => (
  <span className={css(styles.mastheadToggle, className)} {...props}>
    {children}
  </span>
);
MastheadToggle.displayName = 'MastheadToggle';
