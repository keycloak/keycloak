import { createIcon } from '../createIcon';

export const CrossIconConfig = {
  name: 'CrossIcon',
  height: 512,
  width: 384,
  svgPath: 'M352 128h-96V32c0-17.67-14.33-32-32-32h-64c-17.67 0-32 14.33-32 32v96H32c-17.67 0-32 14.33-32 32v64c0 17.67 14.33 32 32 32h96v224c0 17.67 14.33 32 32 32h64c17.67 0 32-14.33 32-32V256h96c17.67 0 32-14.33 32-32v-64c0-17.67-14.33-32-32-32z',
  yOffset: 0,
  xOffset: 0,
};

export const CrossIcon = createIcon(CrossIconConfig);

export default CrossIcon;