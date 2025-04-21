import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/JumpLinks/jump-links';

export interface JumpLinksListProps extends React.HTMLProps<HTMLUListElement> {
  /** Text to be rendered inside span */
  children?: React.ReactNode;
  /** Classname to add to ul. */
  className?: string;
}

export const JumpLinksList: React.FunctionComponent<JumpLinksListProps> = ({
  children,
  className,
  ...props
}: JumpLinksListProps) => (
  <ul className={css(styles.jumpLinksList, className)} {...props}>
    {children}
  </ul>
);
JumpLinksList.displayName = 'JumpLinksList';
