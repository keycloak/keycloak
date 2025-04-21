import { createIcon } from '../createIcon';

export const WindowRestoreIconConfig = {
  name: 'WindowRestoreIcon',
  height: 512,
  width: 512,
  svgPath: 'M512 48v288c0 26.5-21.5 48-48 48h-48V176c0-44.1-35.9-80-80-80H128V48c0-26.5 21.5-48 48-48h288c26.5 0 48 21.5 48 48zM384 176v288c0 26.5-21.5 48-48 48H48c-26.5 0-48-21.5-48-48V176c0-26.5 21.5-48 48-48h288c26.5 0 48 21.5 48 48zm-68 28c0-6.6-5.4-12-12-12H76c-6.6 0-12 5.4-12 12v52h252v-52z',
  yOffset: 0,
  xOffset: 0,
};

export const WindowRestoreIcon = createIcon(WindowRestoreIconConfig);

export default WindowRestoreIcon;