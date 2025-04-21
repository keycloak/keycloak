"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultipleFileUploadStatusItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const multiple_file_upload_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload"));
const react_styles_1 = require("@patternfly/react-styles");
const Progress_1 = require("../Progress");
const Button_1 = require("../Button");
const file_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/file-icon'));
const times_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-circle-icon'));
const MultipleFileUploadStatusItem = (_a) => {
    var { className, file, fileIcon, onReadStarted = () => { }, onReadFinished = () => { }, onReadSuccess = () => { }, onReadFail = () => { }, onClearClick = () => { }, customFileHandler, fileName, fileSize, progressValue, progressVariant, progressAriaLabel, progressAriaLabelledBy, progressId, buttonAriaLabel = 'Remove from list' } = _a, props = tslib_1.__rest(_a, ["className", "file", "fileIcon", "onReadStarted", "onReadFinished", "onReadSuccess", "onReadFail", "onClearClick", "customFileHandler", "fileName", "fileSize", "progressValue", "progressVariant", "progressAriaLabel", "progressAriaLabelledBy", "progressId", "buttonAriaLabel"]);
    const [loadPercentage, setLoadPercentage] = React.useState(0);
    const [loadResult, setLoadResult] = React.useState();
    function readFile(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = () => reject(reader.error);
            reader.onprogress = data => {
                if (data.lengthComputable) {
                    setLoadPercentage((data.loaded / data.total) * 100);
                }
            };
            reader.readAsDataURL(file);
        });
    }
    React.useEffect(() => {
        if (customFileHandler) {
            customFileHandler(file);
        }
        else {
            onReadStarted(file);
            readFile(file)
                .then(data => {
                setLoadResult('success');
                setLoadPercentage(100);
                onReadFinished(file);
                onReadSuccess(data, file);
            })
                .catch((error) => {
                onReadFinished(file);
                onReadFail(error, file);
                setLoadResult('danger');
            });
        }
    }, []);
    const getHumanReadableFileSize = (size) => {
        const prefixes = ['', 'K', 'M', 'G', 'T'];
        let prefixUnit = 0;
        while (size >= 1000) {
            prefixUnit += 1;
            size = size / 1000;
        }
        if (prefixUnit >= prefixes.length) {
            return 'File size too large';
        }
        return `${Math.round(size)}${prefixes[prefixUnit]}B`;
    };
    const title = (React.createElement("span", { className: multiple_file_upload_1.default.multipleFileUploadStatusItemProgress },
        React.createElement("span", { className: multiple_file_upload_1.default.multipleFileUploadStatusItemProgressText }, fileName || (file === null || file === void 0 ? void 0 : file.name) || ''),
        React.createElement("span", { className: multiple_file_upload_1.default.multipleFileUploadStatusItemProgressSize }, fileSize || getHumanReadableFileSize((file === null || file === void 0 ? void 0 : file.size) || 0))));
    return (React.createElement("li", Object.assign({ className: react_styles_1.css(multiple_file_upload_1.default.multipleFileUploadStatusItem, className) }, props),
        React.createElement("div", { className: multiple_file_upload_1.default.multipleFileUploadStatusItemIcon }, fileIcon || React.createElement(file_icon_1.default, null)),
        React.createElement("div", { className: multiple_file_upload_1.default.multipleFileUploadStatusItemMain },
            React.createElement(Progress_1.Progress, { title: title, value: progressValue || loadPercentage, variant: progressVariant || loadResult, "aria-label": progressAriaLabel, "aria-labelledby": progressAriaLabelledBy, id: progressId })),
        React.createElement("div", { className: multiple_file_upload_1.default.multipleFileUploadStatusItemClose },
            React.createElement(Button_1.Button, { variant: "plain", "aria-label": buttonAriaLabel, onClick: onClearClick },
                React.createElement(times_circle_icon_1.default, null)))));
};
exports.MultipleFileUploadStatusItem = MultipleFileUploadStatusItem;
exports.MultipleFileUploadStatusItem.displayName = 'MultipleFileUploadStatusItem';
//# sourceMappingURL=MultipleFileUploadStatusItem.js.map