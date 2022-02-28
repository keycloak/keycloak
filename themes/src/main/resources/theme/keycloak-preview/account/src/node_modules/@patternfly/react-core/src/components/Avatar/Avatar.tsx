import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Avatar/avatar';
import { css } from '@patternfly/react-styles';

export interface AvatarProps
  extends React.DetailedHTMLProps<React.ImgHTMLAttributes<HTMLImageElement>, HTMLImageElement> {
  /** Additional classes added to the Avatar. */
  className?: string;
  /** Attribute that specifies the URL of the image for the Avatar. */
  src?: string;
  /** Attribute that specifies the alternate text of the image for the Avatar. */
  alt: string;
}

export const Avatar: React.FunctionComponent<AvatarProps> = ({
  className = '',
  src = '',
  alt,
  ...props
}: AvatarProps) => <img {...props} src={src} alt={alt} className={css(styles.avatar, className)} />;
