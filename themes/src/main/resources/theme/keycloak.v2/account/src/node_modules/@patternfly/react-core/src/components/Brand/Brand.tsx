import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Brand/brand';
import { setBreakpointCssVars } from '../../helpers';
export interface BrandProps
  extends React.DetailedHTMLProps<React.ImgHTMLAttributes<HTMLImageElement>, HTMLImageElement> {
  /** Transforms the Brand into a <picture> element from an <img> element. Container for <source> child elements. */
  children?: React.ReactNode;
  /** Additional classes added to the either type of Brand. */
  className?: string;
  /** Attribute that specifies the URL of a <img> Brand. For a <picture> Brand this specifies the fallback <img> URL. */
  src?: string;
  /** Attribute that specifies the alt text of a <img> Brand. For a <picture> Brand this specifies the fallback <img> alt text. */
  alt: string;
  /** Widths at various breakpoints for a <picture> Brand. */
  widths?: {
    default?: string;
    sm?: string;
    md?: string;
    lg?: string;
    xl?: string;
    '2xl'?: string;
  };
  /** Heights at various breakpoints for a <picture> Brand. */
  heights?: {
    default?: string;
    sm?: string;
    md?: string;
    lg?: string;
    xl?: string;
    '2xl'?: string;
  };
}

export const Brand: React.FunctionComponent<BrandProps> = ({
  className = '',
  src = '',
  alt,
  children,
  widths,
  heights,
  style,
  ...props
}: BrandProps) => {
  if (children !== undefined && widths !== undefined) {
    style = {
      ...style,
      ...setBreakpointCssVars(widths, '--pf-c-brand--Width')
    };
  }

  if (children !== undefined && heights !== undefined) {
    style = {
      ...style,
      ...setBreakpointCssVars(heights, '--pf-c-brand--Height')
    };
  }

  return (
    /** the brand component currently contains no styling the 'pf-c-brand' string will be used for the className */
    children !== undefined ? (
      <picture className={css(styles.brand, styles.modifiers.picture, className)} style={style} {...props}>
        {children}
        <img src={src} alt={alt} />
      </picture>
    ) : (
      <img {...props} className={css(styles.brand, className)} src={src} alt={alt} />
    )
  );
};
Brand.displayName = 'Brand';
