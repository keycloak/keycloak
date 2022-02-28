import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/EmptyState/empty-state';

export enum IconSize {
  sm = 'sm',
  md = 'md',
  lg = 'lg',
  xl = 'xl'
}

export interface IconProps extends Omit<React.HTMLProps<SVGElement>, 'size'> {
  /** deprecated */
  color?: string;
  /** deprecated */
  size?: 'sm' | 'md' | 'lg' | 'xl';
  /** deprecated */
  title?: string;
}

export interface EmptyStateIconProps extends IconProps {
  /** Additional classes added to the EmptyState */
  className?: string;
  /** Icon component to be rendered inside the EmptyState on icon variant */
  icon?: string | React.FunctionComponent<IconProps>;
  /** Component to be rendered inside the EmptyState on container variant */
  component?: React.FunctionComponent<any>;
  /** Adds empty state icon variant styles  */
  variant?: 'icon' | 'container';
}

export const EmptyStateIcon: React.FunctionComponent<EmptyStateIconProps> = ({
  className = '',
  icon: IconComponent = null,
  component: AnyComponent = null,
  variant = 'icon',
  ...props
}: EmptyStateIconProps) => {
  const classNames = css(styles.emptyStateIcon, className);
  return variant === 'icon' ? (
    <IconComponent className={classNames} {...props} aria-hidden="true" />
  ) : (
    <div className={classNames}>
      <AnyComponent />
    </div>
  );
};
