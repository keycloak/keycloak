import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
export const FormSection = (_a) => {
    var { className = '', children, title = '', titleElement: TitleElement = 'div' } = _a, props = __rest(_a, ["className", "children", "title", "titleElement"]);
    return (React.createElement(GenerateId, { prefix: "pf-form-section-title" }, sectionId => (React.createElement("section", Object.assign({ className: css(styles.formSection, className), role: "group" }, (title && { 'aria-labelledby': sectionId }), props),
        title && (React.createElement(TitleElement, { id: sectionId, className: css(styles.formSectionTitle, className) }, title)),
        children))));
};
FormSection.displayName = 'FormSection';
//# sourceMappingURL=FormSection.js.map