import * as React from 'react';
import cssVar from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage';
import cssVarName2x from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage_2x';
import cssVarNameSm from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage_sm';
import cssVarNameSm2x from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage_sm_2x';
import cssVarNameLg from '@patternfly/react-tokens/dist/esm/c_background_image_BackgroundImage_lg';
import cssVarNameFilter from '@patternfly/react-tokens/dist/esm/c_background_image_Filter';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/BackgroundImage/background-image';

export interface BackgroundImageSrcMap {
  xs: string;
  xs2x: string;
  sm: string;
  sm2x: string;
  lg: string;
}

const defaultFilter = (
  <filter>
    <feColorMatrix type="matrix" values="1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 0 0 0 1 0"></feColorMatrix>
    <feComponentTransfer colorInterpolationFilters="sRGB" result="duotone">
      <feFuncR type="table" tableValues="0.086274509803922 0.43921568627451"></feFuncR>
      <feFuncG type="table" tableValues="0.086274509803922 0.43921568627451"></feFuncG>
      <feFuncB type="table" tableValues="0.086274509803922 0.43921568627451"></feFuncB>
      <feFuncA type="table" tableValues="0 1"></feFuncA>
    </feComponentTransfer>
  </filter>
);

export interface BackgroundImageProps extends Omit<React.HTMLProps<HTMLDivElement>, 'src'> {
  /** Additional classes added to the background. */
  className?: string;
  /** Override svg filter to use */
  filter?: React.ReactElement;
  /** Override image styles using a string or BackgroundImageSrc */
  src: string | BackgroundImageSrcMap;
}

let filterCounter = 0;

export const BackgroundImage: React.FunctionComponent<BackgroundImageProps> = ({
  className,
  src,
  filter = defaultFilter,
  ...props
}: BackgroundImageProps) => {
  const getUrlValue = (size: keyof BackgroundImageSrcMap) => {
    if (typeof src === 'string') {
      return `url(${src})`;
    } else if (typeof src === 'object') {
      return `url(${src[size]})`;
    }

    return '';
  };

  const filterNum = React.useMemo(() => filterCounter++, []);
  const filterId = `patternfly-background-image-filter-overlay${filterNum}`;
  const style = {
    [cssVar.name]: getUrlValue('xs'),
    [cssVarName2x.name]: getUrlValue('xs2x'),
    [cssVarNameSm.name]: getUrlValue('sm'),
    [cssVarNameSm2x.name]: getUrlValue('sm2x'),
    [cssVarNameLg.name]: getUrlValue('lg'),
    [cssVarNameFilter.name]: `url(#${filterId})`
  } as React.CSSProperties;

  return (
    <div className={css(styles.backgroundImage, className)} style={style} {...props}>
      <svg xmlns="http://www.w3.org/2000/svg" className="pf-c-background-image__filter" width="0" height="0">
        {React.cloneElement(filter, { id: filterId })}
      </svg>
    </div>
  );
};
BackgroundImage.displayName = 'BackgroundImage';
