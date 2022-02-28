import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Gallery/gallery';

export interface GalleryProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the Gallery layout */
  children?: React.ReactNode;
  /** additional classes added to the Gallery layout */
  className?: string;
  /** Adds space between children. */
  gutter?: 'sm' | 'md' | 'lg';
}
export const Gallery: React.FunctionComponent<GalleryProps> = ({
  children = null,
  className = '',
  gutter = null,
  ...props
}: GalleryProps) => (
  <div className={css(styles.gallery, gutter && styles.modifiers.gutter, className)} {...props}>
    {children}
  </div>
);
