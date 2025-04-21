import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AboutModalBox/about-modal-box';
// eslint-disable-next-line camelcase
import c_about_modal_box__hero_sm_BackgroundImage from '@patternfly/react-tokens/dist/esm/c_about_modal_box__hero_sm_BackgroundImage';
export const AboutModalBoxHero = (_a) => {
    var { className, backgroundImageSrc } = _a, props = __rest(_a, ["className", "backgroundImageSrc"]);
    return (React.createElement("div", Object.assign({ style: 
        /* eslint-disable camelcase */
        backgroundImageSrc !== ''
            ? { [c_about_modal_box__hero_sm_BackgroundImage.name]: `url(${backgroundImageSrc})` }
            : {}, className: css(styles.aboutModalBoxHero, className) }, props)));
};
AboutModalBoxHero.displayName = 'AboutModalBoxHero';
//# sourceMappingURL=AboutModalBoxHero.js.map