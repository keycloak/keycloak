import React from 'react';
import { BackgroundImage } from '@patternfly/react-core';

/**
 * Note: When using background-filter.svg, you must also include #image_overlay as the fragment identifier
 */
const images = {
  xs: '/assets/images/pfbg_576.jpg',
  xs2x: '/assets/images/pfbg_576@2x.jpg',
  sm: '/assets/images/pfbg_768.jpg',
  sm2x: '/assets/images/pfbg_768@2x.jpg',
  lg: '/assets/images/pfbg_1200.jpg'
};

export const BackgroundImageBasic: React.FunctionComponent = () => <BackgroundImage src={images} />;
