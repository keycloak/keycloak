import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';
import { MultipleFileUploadTitleIcon } from './MultipleFileUploadTitleIcon';
import { MultipleFileUploadTitleText } from './MultipleFileUploadTitleText';
import { MultipleFileUploadTitleTextSeparator } from './MultipleFileUploadTitleTextSeparator';
export const MultipleFileUploadTitle = (_a) => {
    var { className, icon, text = '', textSeparator = '' } = _a, props = __rest(_a, ["className", "icon", "text", "textSeparator"]);
    return (React.createElement("div", Object.assign({ className: css(styles.multipleFileUploadTitle, className) }, props),
        icon && React.createElement(MultipleFileUploadTitleIcon, null, icon),
        text && (React.createElement(MultipleFileUploadTitleText, null,
            `${text} `,
            textSeparator && React.createElement(MultipleFileUploadTitleTextSeparator, null, textSeparator)))));
};
MultipleFileUploadTitle.displayName = 'MultipleFileUploadTitle';
//# sourceMappingURL=MultipleFileUploadTitle.js.map