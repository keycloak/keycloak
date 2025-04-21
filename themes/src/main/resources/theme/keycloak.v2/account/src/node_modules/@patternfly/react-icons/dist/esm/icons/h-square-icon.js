import { createIcon } from '../createIcon';

export const HSquareIconConfig = {
  name: 'HSquareIcon',
  height: 512,
  width: 448,
  svgPath: 'M448 80v352c0 26.51-21.49 48-48 48H48c-26.51 0-48-21.49-48-48V80c0-26.51 21.49-48 48-48h352c26.51 0 48 21.49 48 48zm-112 48h-32c-8.837 0-16 7.163-16 16v80H160v-80c0-8.837-7.163-16-16-16h-32c-8.837 0-16 7.163-16 16v224c0 8.837 7.163 16 16 16h32c8.837 0 16-7.163 16-16v-80h128v80c0 8.837 7.163 16 16 16h32c8.837 0 16-7.163 16-16V144c0-8.837-7.163-16-16-16z',
  yOffset: 0,
  xOffset: 0,
};

export const HSquareIcon = createIcon(HSquareIconConfig);

export default HSquareIcon;