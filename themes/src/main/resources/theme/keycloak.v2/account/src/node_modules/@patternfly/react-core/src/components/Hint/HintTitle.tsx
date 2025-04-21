import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Hint/hint';
import { css } from '@patternfly/react-styles';

export interface HintTitleProps {
  /** Content rendered inside the hint title. */
  children?: React.ReactNode;
  /** Additional classes applied to the hint title. */
  className?: string;
}

export const HintTitle: React.FunctionComponent<HintTitleProps> = ({
  children,
  className,
  ...props
}: HintTitleProps) => (
  <div className={css(styles.hintTitle, className)} {...props}>
    {children}
  </div>
);
HintTitle.displayName = 'HintTitle';
