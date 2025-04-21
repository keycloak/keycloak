import { createIcon } from '../createIcon';

export const MouseIconConfig = {
  name: 'MouseIcon',
  height: 512,
  width: 384,
  svgPath: 'M0 352a160 160 0 0 0 160 160h64a160 160 0 0 0 160-160V224H0zM176 0h-16A160 160 0 0 0 0 160v32h176zm48 0h-16v192h176v-32A160 160 0 0 0 224 0z',
  yOffset: 0,
  xOffset: 0,
};

export const MouseIcon = createIcon(MouseIconConfig);

export default MouseIcon;