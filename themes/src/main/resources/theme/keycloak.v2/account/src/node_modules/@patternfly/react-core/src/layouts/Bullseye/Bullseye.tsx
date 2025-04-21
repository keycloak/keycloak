import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Bullseye/bullseye';

export interface BullseyeProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the Bullseye layout */
  children?: React.ReactNode;
  /** additional classes added to the Bullseye layout */
  className?: string;
  /** Sets the base component to render. defaults to div */
  component?: keyof JSX.IntrinsicElements;
}

export const Bullseye: React.FunctionComponent<BullseyeProps> = ({
  children = null,
  className = '',
  component = 'div',
  ...props
}: BullseyeProps) => {
  const Component = component as any;
  return (
    <Component className={css(styles.bullseye, className)} {...props}>
      {children}
    </Component>
  );
};
Bullseye.displayName = 'Bullseye';
