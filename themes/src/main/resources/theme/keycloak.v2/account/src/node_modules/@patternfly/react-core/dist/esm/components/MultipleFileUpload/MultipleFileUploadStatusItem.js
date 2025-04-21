import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';
import { Progress } from '../Progress';
import { Button } from '../Button';
import FileIcon from '@patternfly/react-icons/dist/esm/icons/file-icon';
import TimesCircleIcon from '@patternfly/react-icons/dist/esm/icons/times-circle-icon';
export const MultipleFileUploadStatusItem = (_a) => {
    var { className, file, fileIcon, onReadStarted = () => { }, onReadFinished = () => { }, onReadSuccess = () => { }, onReadFail = () => { }, onClearClick = () => { }, customFileHandler, fileName, fileSize, progressValue, progressVariant, progressAriaLabel, progressAriaLabelledBy, progressId, buttonAriaLabel = 'Remove from list' } = _a, props = __rest(_a, ["className", "file", "fileIcon", "onReadStarted", "onReadFinished", "onReadSuccess", "onReadFail", "onClearClick", "customFileHandler", "fileName", "fileSize", "progressValue", "progressVariant", "progressAriaLabel", "progressAriaLabelledBy", "progressId", "buttonAriaLabel"]);
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
    const title = (React.createElement("span", { className: styles.multipleFileUploadStatusItemProgress },
        React.createElement("span", { className: styles.multipleFileUploadStatusItemProgressText }, fileName || (file === null || file === void 0 ? void 0 : file.name) || ''),
        React.createElement("span", { className: styles.multipleFileUploadStatusItemProgressSize }, fileSize || getHumanReadableFileSize((file === null || file === void 0 ? void 0 : file.size) || 0))));
    return (React.createElement("li", Object.assign({ className: css(styles.multipleFileUploadStatusItem, className) }, props),
        React.createElement("div", { className: styles.multipleFileUploadStatusItemIcon }, fileIcon || React.createElement(FileIcon, null)),
        React.createElement("div", { className: styles.multipleFileUploadStatusItemMain },
            React.createElement(Progress, { title: title, value: progressValue || loadPercentage, variant: progressVariant || loadResult, "aria-label": progressAriaLabel, "aria-labelledby": progressAriaLabelledBy, id: progressId })),
        React.createElement("div", { className: styles.multipleFileUploadStatusItemClose },
            React.createElement(Button, { variant: "plain", "aria-label": buttonAriaLabel, onClick: onClearClick },
                React.createElement(TimesCircleIcon, null)))));
};
MultipleFileUploadStatusItem.displayName = 'MultipleFileUploadStatusItem';
//# sourceMappingURL=MultipleFileUploadStatusItem.js.map