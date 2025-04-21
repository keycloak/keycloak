import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';
import { MultipleFileUploadContext } from './MultipleFileUpload';
import { Button } from '../Button';
export const MultipleFileUploadButton = (_a) => {
    var { className, 'aria-label': ariaLabel } = _a, props = __rest(_a, ["className", 'aria-label']);
    const { open } = React.useContext(MultipleFileUploadContext);
    return (React.createElement("div", Object.assign({ className: css(styles.multipleFileUploadUpload, className) }, props),
        React.createElement(Button, { variant: "secondary", "aria-label": ariaLabel, onClick: open }, "Upload")));
};
MultipleFileUploadButton.displayName = 'MultipleFileUploadButton';
//# sourceMappingURL=MultipleFileUploadButton.js.map