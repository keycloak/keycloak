"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultipleFileUploadTitle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const multiple_file_upload_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload"));
const react_styles_1 = require("@patternfly/react-styles");
const MultipleFileUploadTitleIcon_1 = require("./MultipleFileUploadTitleIcon");
const MultipleFileUploadTitleText_1 = require("./MultipleFileUploadTitleText");
const MultipleFileUploadTitleTextSeparator_1 = require("./MultipleFileUploadTitleTextSeparator");
const MultipleFileUploadTitle = (_a) => {
    var { className, icon, text = '', textSeparator = '' } = _a, props = tslib_1.__rest(_a, ["className", "icon", "text", "textSeparator"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(multiple_file_upload_1.default.multipleFileUploadTitle, className) }, props),
        icon && React.createElement(MultipleFileUploadTitleIcon_1.MultipleFileUploadTitleIcon, null, icon),
        text && (React.createElement(MultipleFileUploadTitleText_1.MultipleFileUploadTitleText, null,
            `${text} `,
            textSeparator && React.createElement(MultipleFileUploadTitleTextSeparator_1.MultipleFileUploadTitleTextSeparator, null, textSeparator)))));
};
exports.MultipleFileUploadTitle = MultipleFileUploadTitle;
exports.MultipleFileUploadTitle.displayName = 'MultipleFileUploadTitle';
//# sourceMappingURL=MultipleFileUploadTitle.js.map