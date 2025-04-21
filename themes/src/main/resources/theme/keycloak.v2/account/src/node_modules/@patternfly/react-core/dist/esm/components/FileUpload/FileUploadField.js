import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/FileUpload/file-upload';
import { css } from '@patternfly/react-styles';
import { InputGroup } from '../InputGroup';
import { TextInput } from '../TextInput';
import { Button, ButtonVariant } from '../Button';
import { TextArea, TextAreResizeOrientation } from '../TextArea';
import { Spinner, spinnerSize } from '../Spinner';
import { fileReaderType } from '../../helpers/fileUtils';
export const FileUploadField = (_a) => {
    var { id, type, value = '', filename = '', onChange = () => { }, onBrowseButtonClick = () => { }, onClearButtonClick = () => { }, onTextAreaClick, onTextChange, onTextAreaBlur, textAreaPlaceholder = '', className = '', isDisabled = false, isReadOnly = false, isLoading = false, spinnerAriaValueText, isRequired = false, isDragActive = false, validated = 'default', 'aria-label': ariaLabel = 'File upload', filenamePlaceholder = 'Drag a file here or browse to upload', filenameAriaLabel = filename ? 'Read only filename' : filenamePlaceholder, browseButtonText = 'Browse...', clearButtonText = 'Clear', isClearButtonDisabled = !filename && !value, containerRef = null, allowEditingUploadedText = false, hideDefaultPreview = false, children = null } = _a, props = __rest(_a, ["id", "type", "value", "filename", "onChange", "onBrowseButtonClick", "onClearButtonClick", "onTextAreaClick", "onTextChange", "onTextAreaBlur", "textAreaPlaceholder", "className", "isDisabled", "isReadOnly", "isLoading", "spinnerAriaValueText", "isRequired", "isDragActive", "validated", 'aria-label', "filenamePlaceholder", "filenameAriaLabel", "browseButtonText", "clearButtonText", "isClearButtonDisabled", "containerRef", "allowEditingUploadedText", "hideDefaultPreview", "children"]);
    const onTextAreaChange = (newValue, event) => {
        onChange(newValue, filename, event);
        onTextChange === null || onTextChange === void 0 ? void 0 : onTextChange(newValue);
    };
    return (React.createElement("div", Object.assign({ className: css(styles.fileUpload, isDragActive && styles.modifiers.dragHover, isLoading && styles.modifiers.loading, className), ref: containerRef }, props),
        React.createElement("div", { className: styles.fileUploadFileSelect },
            React.createElement(InputGroup, null,
                React.createElement(TextInput, { isReadOnly // Always read-only regardless of isReadOnly prop (which is just for the TextArea)
                    : true, isDisabled: isDisabled, id: `${id}-filename`, name: `${id}-filename`, "aria-label": filenameAriaLabel, placeholder: filenamePlaceholder, "aria-describedby": `${id}-browse-button`, value: filename }),
                React.createElement(Button, { id: `${id}-browse-button`, variant: ButtonVariant.control, onClick: onBrowseButtonClick, isDisabled: isDisabled }, browseButtonText),
                React.createElement(Button, { variant: ButtonVariant.control, isDisabled: isDisabled || isClearButtonDisabled, onClick: onClearButtonClick }, clearButtonText))),
        React.createElement("div", { className: styles.fileUploadFileDetails },
            !hideDefaultPreview && type === fileReaderType.text && (React.createElement(TextArea, { readOnly: isReadOnly || (!!filename && !allowEditingUploadedText), disabled: isDisabled, isRequired: isRequired, resizeOrientation: TextAreResizeOrientation.vertical, validated: validated, id: id, name: id, "aria-label": ariaLabel, value: value, onChange: onTextAreaChange, onClick: onTextAreaClick, onBlur: onTextAreaBlur, placeholder: textAreaPlaceholder })),
            isLoading && (React.createElement("div", { className: styles.fileUploadFileDetailsSpinner },
                React.createElement(Spinner, { size: spinnerSize.lg, "aria-valuetext": spinnerAriaValueText })))),
        children));
};
FileUploadField.displayName = 'FileUploadField';
//# sourceMappingURL=FileUploadField.js.map