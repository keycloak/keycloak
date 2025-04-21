import { createIcon } from '../createIcon';

export const ArrowIconConfig = {
  name: 'ArrowIcon',
  height: 1024,
  width: 1070,
  svgPath: 'M832,768 L832,896 L128,896 L128,256.4 L224,256.4 L352,128 L36.6,128 C14.6,128 0,142.7 0,164.6 L0,987.4 C0,1009.4 14.6,1024 36.6,1024 L930.7,1024 C945.3,1024 960,1009.4 960,987.4 L960,640 L832,768 Z M638.3,219.4 L704.1,219.4 L704.1,0 L1070,329.2 L704.2,658.3 L704.2,438.9 L674.9,438.9 C448.1,453.5 353,651 411.6,775.3 C353.1,731.4 279.9,709.5 265.3,570.5 C243.3,351.1 418.9,219.4 638.3,219.4 Z',
  yOffset: 0,
  xOffset: 0,
};

export const ArrowIcon = createIcon(ArrowIconConfig);

export default ArrowIcon;