import { createIcon } from '../createIcon';

export const HospitalSymbolIconConfig = {
  name: 'HospitalSymbolIcon',
  height: 512,
  width: 512,
  svgPath: 'M256 0C114.6 0 0 114.6 0 256s114.6 256 256 256 256-114.6 256-256S397.4 0 256 0zm112 376c0 4.4-3.6 8-8 8h-48c-4.4 0-8-3.6-8-8v-88h-96v88c0 4.4-3.6 8-8 8h-48c-4.4 0-8-3.6-8-8V136c0-4.4 3.6-8 8-8h48c4.4 0 8 3.6 8 8v88h96v-88c0-4.4 3.6-8 8-8h48c4.4 0 8 3.6 8 8v240z',
  yOffset: 0,
  xOffset: 0,
};

export const HospitalSymbolIcon = createIcon(HospitalSymbolIconConfig);

export default HospitalSymbolIcon;