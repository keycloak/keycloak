"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FileUploadField = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const file_upload_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/FileUpload/file-upload"));
const react_styles_1 = require("@patternfly/react-styles");
const InputGroup_1 = require("../InputGroup");
const TextInput_1 = require("../TextInput");
const Button_1 = require("../Button");
const TextArea_1 = require("../TextArea");
const Spinner_1 = require("../Spinner");
const fileUtils_1 = require("../../helpers/fileUtils");
const FileUploadField = (_a) => {
    var { id, type, value = '', filename = '', onChange = () => { }, onBrowseButtonClick = () => { }, onClearButtonClick = () => { }, onTextAreaClick, onTextChange, onTextAreaBlur, textAreaPlaceholder = '', className = '', isDisabled = false, isReadOnly = false, isLoading = false, spinnerAriaValueText, isRequired = false, isDragActive = false, validated = 'default', 'aria-label': ariaLabel = 'File upload', filenamePlaceholder = 'Drag a file here or browse to upload', filenameAriaLabel = filename ? 'Read only filename' : filenamePlaceholder, browseButtonText = 'Browse...', clearButtonText = 'Clear', isClearButtonDisabled = !filename && !value, containerRef = null, allowEditingUploadedText = false, hideDefaultPreview = false, children = null } = _a, props = tslib_1.__rest(_a, ["id", "type", "value", "filename", "onChange", "onBrowseButtonClick", "onClearButtonClick", "onTextAreaClick", "onTextChange", "onTextAreaBlur", "textAreaPlaceholder", "className", "isDisabled", "isReadOnly", "isLoading", "spinnerAriaValueText", "isRequired", "isDragActive", "validated", 'aria-label', "filenamePlaceholder", "filenameAriaLabel", "browseButtonText", "clearButtonText", "isClearButtonDisabled", "containerRef", "allowEditingUploadedText", "hideDefaultPreview", "children"]);
    const onTextAreaChange = (newValue, event) => {
        onChange(newValue, filename, event);
        onTextChange === null || onTextChange === void 0 ? void 0 : onTextChange(newValue);
    };
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(file_upload_1.default.fileUpload, isDragActive && file_upload_1.default.modifiers.dragHover, isLoading && file_upload_1.default.modifiers.loading, className), ref: containerRef }, props),
        React.createElement("div", { className: file_upload_1.default.fileUploadFileSelect },
            React.createElement(InputGroup_1.InputGroup, null,
                React.createElement(TextInput_1.TextInput, { isReadOnly // Always read-only regardless of isReadOnly prop (which is just for the TextArea)
                    : true, isDisabled: isDisabled, id: `${id}-filename`, name: `${id}-filename`, "aria-label": filenameAriaLabel, placeholder: filenamePlaceholder, "aria-describedby": `${id}-browse-button`, value: filename }),
                React.createElement(Button_1.Button, { id: `${id}-browse-button`, variant: Button_1.ButtonVariant.control, onClick: onBrowseButtonClick, isDisabled: isDisabled }, browseButtonText),
                React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.control, isDisabled: isDisabled || isClearButtonDisabled, onClick: onClearButtonClick }, clearButtonText))),
        React.createElement("div", { className: file_upload_1.default.fileUploadFileDetails },
            !hideDefaultPreview && type === fileUtils_1.fileReaderType.text && (React.createElement(TextArea_1.TextArea, { readOnly: isReadOnly || (!!filename && !allowEditingUploadedText), disabled: isDisabled, isRequired: isRequired, resizeOrientation: TextArea_1.TextAreResizeOrientation.vertical, validated: validated, id: id, name: id, "aria-label": ariaLabel, value: value, onChange: onTextAreaChange, onClick: onTextAreaClick, onBlur: onTextAreaBlur, placeholder: textAreaPlaceholder })),
            isLoading && (React.createElement("div", { className: file_upload_1.default.fileUploadFileDetailsSpinner },
                React.createElement(Spinner_1.Spinner, { size: Spinner_1.spinnerSize.lg, "aria-valuetext": spinnerAriaValueText })))),
        children));
};
exports.FileUploadField = FileUploadField;
exports.FileUploadField.displayName = 'FileUploadField';
//# sourceMappingURL=FileUploadField.js.map