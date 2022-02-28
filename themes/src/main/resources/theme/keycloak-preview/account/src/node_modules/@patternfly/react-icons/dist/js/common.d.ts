import { HTMLProps } from 'react';

export const IconSize: {
  sm: 'sm';
  md: 'md';
  lg: 'lg';
  xl: 'xl';
};

export interface IconProps extends Omit<HTMLProps<SVGElement>, 'size'> {
  color?: string;
  size?: keyof typeof IconSize;
  title?: string;
  noVerticalAlign?: boolean;
}

export const propTypes: Record<'size' | 'color', any>;

export const defaultProps: typeof propTypes;

export const getSize: (size: keyof typeof IconSize) => string;
