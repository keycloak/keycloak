import { createIcon } from '../createIcon';

export const EjectIconConfig = {
  name: 'EjectIcon',
  height: 512,
  width: 448,
  svgPath: 'M448 384v64c0 17.673-14.327 32-32 32H32c-17.673 0-32-14.327-32-32v-64c0-17.673 14.327-32 32-32h384c17.673 0 32 14.327 32 32zM48.053 320h351.886c41.651 0 63.581-49.674 35.383-80.435L259.383 47.558c-19.014-20.743-51.751-20.744-70.767 0L12.67 239.565C-15.475 270.268 6.324 320 48.053 320z',
  yOffset: 0,
  xOffset: 0,
};

export const EjectIcon = createIcon(EjectIconConfig);

export default EjectIcon;