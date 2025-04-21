import { createIcon } from '../createIcon';

export const ClockIconConfig = {
  name: 'ClockIcon',
  height: 512,
  width: 512,
  svgPath: 'M256,8C119,8,8,119,8,256S119,504,256,504,504,393,504,256,393,8,256,8Zm92.49,313h0l-20,25a16,16,0,0,1-22.49,2.5h0l-67-49.72a40,40,0,0,1-15-31.23V112a16,16,0,0,1,16-16h32a16,16,0,0,1,16,16V256l58,42.5A16,16,0,0,1,348.49,321Z',
  yOffset: 0,
  xOffset: 0,
};

export const ClockIcon = createIcon(ClockIconConfig);

export default ClockIcon;