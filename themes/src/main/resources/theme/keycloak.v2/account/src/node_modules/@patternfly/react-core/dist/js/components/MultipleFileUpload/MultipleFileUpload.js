"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultipleFileUpload = exports.MultipleFileUploadContext = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_dropzone_1 = tslib_1.__importDefault(require("react-dropzone"));
const multiple_file_upload_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload"));
const react_styles_1 = require("@patternfly/react-styles");
exports.MultipleFileUploadContext = React.createContext({
    open: () => { }
});
const MultipleFileUpload = (_a) => {
    var { className, children, dropzoneProps = {}, isHorizontal, onFileDrop = () => { } } = _a, props = tslib_1.__rest(_a, ["className", "children", "dropzoneProps", "isHorizontal", "onFileDrop"]);
    const onDropAccepted = (acceptedFiles, event) => {
        onFileDrop(acceptedFiles);
        // allow users to set a custom drop accepted handler rather than using on data change
        dropzoneProps.onDropAccepted && dropzoneProps.onDropAccepted(acceptedFiles, event);
    };
    const onDropRejected = (rejectedFiles, event) => {
        dropzoneProps.onDropRejected && (dropzoneProps === null || dropzoneProps === void 0 ? void 0 : dropzoneProps.onDropRejected(rejectedFiles, event));
    };
    return (React.createElement(react_dropzone_1.default, Object.assign({ multiple: true }, dropzoneProps, { onDropAccepted: onDropAccepted, onDropRejected: onDropRejected }), ({ getRootProps, getInputProps, isDragActive, open }) => {
        const rootProps = getRootProps(Object.assign(Object.assign({}, props), { onClick: event => event.preventDefault() // Prevents clicking TextArea from opening file dialog
         }));
        const inputProps = getInputProps();
        return (React.createElement(exports.MultipleFileUploadContext.Provider, { value: { open } },
            React.createElement("div", Object.assign({ className: react_styles_1.css(multiple_file_upload_1.default.multipleFileUpload, isDragActive && multiple_file_upload_1.default.modifiers.dragOver, isHorizontal && multiple_file_upload_1.default.modifiers.horizontal, className) }, rootProps, props),
                React.createElement("input", Object.assign({}, inputProps)),
                children)));
    }));
};
exports.MultipleFileUpload = MultipleFileUpload;
exports.MultipleFileUpload.displayName = 'MultipleFileUpload';
//# sourceMappingURL=MultipleFileUpload.js.map