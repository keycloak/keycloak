import { createIcon } from '../createIcon';

export const SuitcaseIconConfig = {
  name: 'SuitcaseIcon',
  height: 512,
  width: 512,
  svgPath: 'M128 480h256V80c0-26.5-21.5-48-48-48H176c-26.5 0-48 21.5-48 48v400zm64-384h128v32H192V96zm320 80v256c0 26.5-21.5 48-48 48h-48V128h48c26.5 0 48 21.5 48 48zM96 480H48c-26.5 0-48-21.5-48-48V176c0-26.5 21.5-48 48-48h48v352z',
  yOffset: 0,
  xOffset: 0,
};

export const SuitcaseIcon = createIcon(SuitcaseIconConfig);

export default SuitcaseIcon;