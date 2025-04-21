import { createIcon } from '../createIcon';

export const PauseCircleIconConfig = {
  name: 'PauseCircleIcon',
  height: 512,
  width: 512,
  svgPath: 'M256 8C119 8 8 119 8 256s111 248 248 248 248-111 248-248S393 8 256 8zm-16 328c0 8.8-7.2 16-16 16h-48c-8.8 0-16-7.2-16-16V176c0-8.8 7.2-16 16-16h48c8.8 0 16 7.2 16 16v160zm112 0c0 8.8-7.2 16-16 16h-48c-8.8 0-16-7.2-16-16V176c0-8.8 7.2-16 16-16h48c8.8 0 16 7.2 16 16v160z',
  yOffset: 0,
  xOffset: 0,
};

export const PauseCircleIcon = createIcon(PauseCircleIconConfig);

export default PauseCircleIcon;