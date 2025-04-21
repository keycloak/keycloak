import { createIcon } from '../createIcon';

export const QrcodeIconConfig = {
  name: 'QrcodeIcon',
  height: 512,
  width: 448,
  svgPath: 'M0 224h192V32H0v192zM64 96h64v64H64V96zm192-64v192h192V32H256zm128 128h-64V96h64v64zM0 480h192V288H0v192zm64-128h64v64H64v-64zm352-64h32v128h-96v-32h-32v96h-64V288h96v32h64v-32zm0 160h32v32h-32v-32zm-64 0h32v32h-32v-32z',
  yOffset: 0,
  xOffset: 0,
};

export const QrcodeIcon = createIcon(QrcodeIconConfig);

export default QrcodeIcon;