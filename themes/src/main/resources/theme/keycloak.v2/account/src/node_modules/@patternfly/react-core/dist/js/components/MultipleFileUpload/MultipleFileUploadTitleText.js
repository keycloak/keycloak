"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultipleFileUploadTitleText = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const multiple_file_upload_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload"));
const react_styles_1 = require("@patternfly/react-styles");
const MultipleFileUploadTitleText = (_a) => {
    var { className, children } = _a, props = tslib_1.__rest(_a, ["className", "children"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(multiple_file_upload_1.default.multipleFileUploadTitleText, className) }, props), children));
};
exports.MultipleFileUploadTitleText = MultipleFileUploadTitleText;
exports.MultipleFileUploadTitleText.displayName = 'MultipleFileUploadTitleText';
//# sourceMappingURL=MultipleFileUploadTitleText.js.map