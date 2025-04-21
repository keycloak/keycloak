import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';

export interface ContextSelectorFooterProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the ContextSelectorFooter */
  children?: React.ReactNode;
  /** Additional classes added to the ContextSelectorFooter */
  className?: string;
}

export const ContextSelectorFooter: React.FunctionComponent<ContextSelectorFooterProps> = ({
  children = null,
  className = '',
  ...props
}: ContextSelectorFooterProps) => (
  <div {...props} className={css(styles.contextSelectorMenuFooter, className)}>
    {children}
  </div>
);
ContextSelectorFooter.displayName = 'ContextSelectorFooter';
