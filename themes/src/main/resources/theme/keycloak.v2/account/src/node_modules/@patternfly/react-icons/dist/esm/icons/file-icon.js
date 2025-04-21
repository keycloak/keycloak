import { createIcon } from '../createIcon';

export const FileIconConfig = {
  name: 'FileIcon',
  height: 512,
  width: 384,
  svgPath: 'M224 136V0H24C10.7 0 0 10.7 0 24v464c0 13.3 10.7 24 24 24h336c13.3 0 24-10.7 24-24V160H248c-13.2 0-24-10.8-24-24zm160-14.1v6.1H256V0h6.1c6.4 0 12.5 2.5 17 7l97.9 98c4.5 4.5 7 10.6 7 16.9z',
  yOffset: 0,
  xOffset: 0,
};

export const FileIcon = createIcon(FileIconConfig);

export default FileIcon;