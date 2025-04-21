import { createIcon } from '../createIcon';

export const CircleIconConfig = {
  name: 'CircleIcon',
  height: 512,
  width: 512,
  svgPath: 'M256 8C119 8 8 119 8 256s111 248 248 248 248-111 248-248S393 8 256 8z',
  yOffset: 0,
  xOffset: 0,
};

export const CircleIcon = createIcon(CircleIconConfig);

export default CircleIcon;