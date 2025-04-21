import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
export const FormFieldGroupHeader = (_a) => {
    var { className, titleText, titleDescription, actions } = _a, props = __rest(_a, ["className", "titleText", "titleDescription", "actions"]);
    return (React.createElement("div", Object.assign({ className: css(styles.formFieldGroupHeader, className) }, props),
        React.createElement("div", { className: css(styles.formFieldGroupHeaderMain) },
            titleText && (React.createElement("div", { className: css(styles.formFieldGroupHeaderTitle) },
                React.createElement("div", { className: css(styles.formFieldGroupHeaderTitleText), id: titleText.id }, titleText.text))),
            titleDescription && React.createElement("div", { className: css(styles.formFieldGroupHeaderDescription) }, titleDescription)),
        React.createElement("div", { className: css(styles.formFieldGroupHeaderActions) }, actions && actions)));
};
FormFieldGroupHeader.displayName = 'FormFieldGroupHeader';
//# sourceMappingURL=FormFieldGroupHeader.js.map