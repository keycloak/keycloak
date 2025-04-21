"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultipleFileUploadMain = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const multiple_file_upload_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload"));
const react_styles_1 = require("@patternfly/react-styles");
const MultipleFileUploadTitle_1 = require("./MultipleFileUploadTitle");
const MultipleFileUploadButton_1 = require("./MultipleFileUploadButton");
const MultipleFileUploadInfo_1 = require("./MultipleFileUploadInfo");
const MultipleFileUploadMain = (_a) => {
    var { className, titleIcon, titleText, titleTextSeparator, infoText, isUploadButtonHidden } = _a, props = tslib_1.__rest(_a, ["className", "titleIcon", "titleText", "titleTextSeparator", "infoText", "isUploadButtonHidden"]);
    const showTitle = !!titleIcon || !!titleText || !!titleTextSeparator;
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(multiple_file_upload_1.default.multipleFileUploadMain, className) }, props),
        showTitle && React.createElement(MultipleFileUploadTitle_1.MultipleFileUploadTitle, { icon: titleIcon, text: titleText, textSeparator: titleTextSeparator }),
        isUploadButtonHidden || React.createElement(MultipleFileUploadButton_1.MultipleFileUploadButton, null),
        !!infoText && React.createElement(MultipleFileUploadInfo_1.MultipleFileUploadInfo, null, infoText)));
};
exports.MultipleFileUploadMain = MultipleFileUploadMain;
exports.MultipleFileUploadMain.displayName = 'MultipleFileUploadMain';
//# sourceMappingURL=MultipleFileUploadMain.js.map