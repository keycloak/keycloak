"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultipleFileUploadTitleIcon = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const multiple_file_upload_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload"));
const react_styles_1 = require("@patternfly/react-styles");
const MultipleFileUploadTitleIcon = (_a) => {
    var { children, className } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(multiple_file_upload_1.default.multipleFileUploadTitleIcon, className) }, props), children));
};
exports.MultipleFileUploadTitleIcon = MultipleFileUploadTitleIcon;
exports.MultipleFileUploadTitleIcon.displayName = 'MultipleFileUploadTitleIcon';
//# sourceMappingURL=MultipleFileUploadTitleIcon.js.map