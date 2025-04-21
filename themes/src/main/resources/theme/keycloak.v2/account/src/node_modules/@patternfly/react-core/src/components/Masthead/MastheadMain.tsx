import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Masthead/masthead';
import { css } from '@patternfly/react-styles';

export interface MastheadMainProps extends React.DetailedHTMLProps<React.HTMLProps<HTMLDivElement>, HTMLDivElement> {
  /** Content rendered inside of the masthead main block. */
  children?: React.ReactNode;
  /** Additional classes added to the masthead main. */
  className?: string;
}

export const MastheadMain: React.FunctionComponent<MastheadMainProps> = ({
  children,
  className,
  ...props
}: MastheadMainProps) => (
  <div className={css(styles.mastheadMain, className)} {...props}>
    {children}
  </div>
);
MastheadMain.displayName = 'MastheadMain';
