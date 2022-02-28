import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import { css } from '@patternfly/react-styles';

export interface CheckboxSelectGroupProps extends React.HTMLProps<HTMLDivElement> {
  /** Checkboxes within group */
  children?: React.ReactNode;
  /** Additional classes added to the CheckboxSelectGroup control */
  className?: string;
  /** Group label */
  label?: string;
  /** ID for title label */
  titleId?: string;
}

export const CheckboxSelectGroup: React.FunctionComponent<CheckboxSelectGroupProps> = ({
  children = [] as React.ReactElement[],
  className = '',
  label = '',
  titleId = '',
  ...props
}: CheckboxSelectGroupProps) => (
  <div {...props} className={css(styles.selectMenuGroup, className)}>
    <div className={css(styles.selectMenuGroupTitle)} id={titleId} aria-hidden>
      {label}
    </div>
    {children}
  </div>
);
