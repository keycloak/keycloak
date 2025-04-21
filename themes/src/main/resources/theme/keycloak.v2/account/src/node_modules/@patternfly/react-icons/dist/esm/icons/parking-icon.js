import { createIcon } from '../createIcon';

export const ParkingIconConfig = {
  name: 'ParkingIcon',
  height: 512,
  width: 448,
  svgPath: 'M400 32H48C21.5 32 0 53.5 0 80v352c0 26.5 21.5 48 48 48h352c26.5 0 48-21.5 48-48V80c0-26.5-21.5-48-48-48zM240 320h-48v48c0 8.8-7.2 16-16 16h-32c-8.8 0-16-7.2-16-16V144c0-8.8 7.2-16 16-16h96c52.9 0 96 43.1 96 96s-43.1 96-96 96zm0-128h-48v64h48c17.6 0 32-14.4 32-32s-14.4-32-32-32z',
  yOffset: 0,
  xOffset: 0,
};

export const ParkingIcon = createIcon(ParkingIconConfig);

export default ParkingIcon;