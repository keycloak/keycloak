import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Card/card';
import titleStyles from '@patternfly/react-styles/css/components/Title/title';

export interface CardHeaderProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the Card Footer */
  children?: React.ReactNode;
  /** Additional classes added to the Header */
  className?: string;
}

export const CardHeader: React.FunctionComponent<CardHeaderProps> = ({
  children = null,
  className = '',
  ...props
}: CardHeaderProps) => (
  <div className={css(styles.cardHeader, titleStyles.title, titleStyles.modifiers.md, className)} {...props}>
    {children}
  </div>
);
