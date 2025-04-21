import { __rest } from "tslib";
import * as React from 'react';
import Dropzone from 'react-dropzone';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';
export const MultipleFileUploadContext = React.createContext({
    open: () => { }
});
export const MultipleFileUpload = (_a) => {
    var { className, children, dropzoneProps = {}, isHorizontal, onFileDrop = () => { } } = _a, props = __rest(_a, ["className", "children", "dropzoneProps", "isHorizontal", "onFileDrop"]);
    const onDropAccepted = (acceptedFiles, event) => {
        onFileDrop(acceptedFiles);
        // allow users to set a custom drop accepted handler rather than using on data change
        dropzoneProps.onDropAccepted && dropzoneProps.onDropAccepted(acceptedFiles, event);
    };
    const onDropRejected = (rejectedFiles, event) => {
        dropzoneProps.onDropRejected && (dropzoneProps === null || dropzoneProps === void 0 ? void 0 : dropzoneProps.onDropRejected(rejectedFiles, event));
    };
    return (React.createElement(Dropzone, Object.assign({ multiple: true }, dropzoneProps, { onDropAccepted: onDropAccepted, onDropRejected: onDropRejected }), ({ getRootProps, getInputProps, isDragActive, open }) => {
        const rootProps = getRootProps(Object.assign(Object.assign({}, props), { onClick: event => event.preventDefault() // Prevents clicking TextArea from opening file dialog
         }));
        const inputProps = getInputProps();
        return (React.createElement(MultipleFileUploadContext.Provider, { value: { open } },
            React.createElement("div", Object.assign({ className: css(styles.multipleFileUpload, isDragActive && styles.modifiers.dragOver, isHorizontal && styles.modifiers.horizontal, className) }, rootProps, props),
                React.createElement("input", Object.assign({}, inputProps)),
                children)));
    }));
};
MultipleFileUpload.displayName = 'MultipleFileUpload';
//# sourceMappingURL=MultipleFileUpload.js.map