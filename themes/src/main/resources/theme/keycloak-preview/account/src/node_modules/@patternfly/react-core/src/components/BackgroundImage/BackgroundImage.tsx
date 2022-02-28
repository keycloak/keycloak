import * as React from 'react';

import { css, StyleSheet } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/BackgroundImage/background-image';

/* eslint-disable camelcase */
import c_background_image_BackgroundImage from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage';
import c_background_image_BackgroundImage_2x from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_2x';
import c_background_image_BackgroundImage_sm from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm';
import c_background_image_BackgroundImage_sm_2x from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm_2x';
import c_background_image_BackgroundImage_lg from '@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_lg';

export enum BackgroundImageSrc {
  xs = 'xs',
  xs2x = 'xs2x',
  sm = 'sm',
  sm2x = 'sm2x',
  lg = 'lg',
  filter = 'filter'
}

const cssVariables = {
  [BackgroundImageSrc.xs]: c_background_image_BackgroundImage && c_background_image_BackgroundImage.name,
  [BackgroundImageSrc.xs2x]: c_background_image_BackgroundImage_2x && c_background_image_BackgroundImage_2x.name,
  [BackgroundImageSrc.sm]: c_background_image_BackgroundImage_sm && c_background_image_BackgroundImage_sm.name,
  [BackgroundImageSrc.sm2x]: c_background_image_BackgroundImage_sm_2x && c_background_image_BackgroundImage_sm_2x.name,
  [BackgroundImageSrc.lg]: c_background_image_BackgroundImage_lg && c_background_image_BackgroundImage_lg.name
};

export interface BackgroundImageSrcMap {
  xs: string;
  xs2x: string;
  sm: string;
  sm2x: string;
  lg: string;
  filter?: string;
}

export interface BackgroundImageProps extends Omit<React.HTMLProps<HTMLDivElement>, 'src'> {
  /** Additional classes added to the background. */
  className?: string;
  /** Override image styles using a string or BackgroundImageSrc */
  src: string | BackgroundImageSrcMap;
}

export const BackgroundImage: React.FunctionComponent<BackgroundImageProps> = ({
  className = '',
  src,
  ...props
}: BackgroundImageProps) => {
  let srcMap = src;
  // Default string value to handle all sizes
  if (typeof src === 'string') {
    srcMap = {
      [BackgroundImageSrc.xs]: src,
      [BackgroundImageSrc.xs2x]: src,
      [BackgroundImageSrc.sm]: src,
      [BackgroundImageSrc.sm2x]: src,
      [BackgroundImageSrc.lg]: src,
      [BackgroundImageSrc.filter]: '' // unused
    };
  }

  // Build stylesheet string based on cssVariables
  let cssSheet = '';
  (Object.keys(cssVariables) as [keyof typeof srcMap]).forEach(size => {
    cssSheet += `${cssVariables[size as keyof typeof cssVariables]}: url('${srcMap[size]}');`;
  });

  // Create emotion stylesheet to inject new css
  const bgStyles = StyleSheet.create({
    bgOverrides: `&.pf-c-background-image {
      ${cssSheet}
    }`
  });

  return (
    <div className={css(styles.backgroundImage, bgStyles.bgOverrides, className)} {...props}>
      <svg xmlns="http://www.w3.org/2000/svg" className="pf-c-background-image__filter" width="0" height="0">
        <filter id="image_overlay">
          <feColorMatrix
            type="matrix"
            values="1 0 0 0 0
            1 0 0 0 0
            1 0 0 0 0
            0 0 0 1 0"
          />
          <feComponentTransfer colorInterpolationFilters="sRGB" result="duotone">
            <feFuncR type="table" tableValues="0.086274509803922 0.43921568627451" />
            <feFuncG type="table" tableValues="0.086274509803922 0.43921568627451" />
            <feFuncB type="table" tableValues="0.086274509803922 0.43921568627451" />
            <feFuncA type="table" tableValues="0 1" />
          </feComponentTransfer>
        </filter>
      </svg>
    </div>
  );
};
