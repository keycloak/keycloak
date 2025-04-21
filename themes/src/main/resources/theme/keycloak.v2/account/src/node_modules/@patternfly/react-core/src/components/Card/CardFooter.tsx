import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Card/card';
import { css } from '@patternfly/react-styles';

export interface CardFooterProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the Card Footer */
  children?: React.ReactNode;
  /** Additional classes added to the Footer */
  className?: string;
  /** Sets the base component to render. defaults to div */
  component?: keyof JSX.IntrinsicElements;
}

export const CardFooter: React.FunctionComponent<CardFooterProps> = ({
  children = null,
  className = '',
  component = 'div',
  ...props
}: CardFooterProps) => {
  const Component = component as any;
  return (
    <Component className={css(styles.cardFooter, className)} {...props}>
      {children}
    </Component>
  );
};
CardFooter.displayName = 'CardFooter';
