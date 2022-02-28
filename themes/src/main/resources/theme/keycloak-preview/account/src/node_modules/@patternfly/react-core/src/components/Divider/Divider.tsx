import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Divider/divider';

export enum DividerVariant {
  hr = 'hr',
  li = 'li',
  div = 'div'
}

export interface DividerProps extends React.HTMLProps<HTMLElement> {
  /** Additional classes added to the divider */
  className?: string;
  /** The component type to use */
  component?: 'hr' | 'li' | 'div';
}

export const Divider: React.FunctionComponent<DividerProps> = ({
  className = '',
  component = DividerVariant.hr,
  ...props
}: DividerProps) => {
  const Component: any = component;

  return (
    <Component
      className={css(styles.divider, className)}
      {...(component !== 'hr' && { role: 'separator' })}
      {...props}
    />
  );
};
