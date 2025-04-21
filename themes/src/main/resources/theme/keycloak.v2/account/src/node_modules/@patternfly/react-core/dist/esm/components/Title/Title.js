import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Title/title';
import { useOUIAProps } from '../../helpers';
export var TitleSizes;
(function (TitleSizes) {
    TitleSizes["md"] = "md";
    TitleSizes["lg"] = "lg";
    TitleSizes["xl"] = "xl";
    TitleSizes["2xl"] = "2xl";
    TitleSizes["3xl"] = "3xl";
    TitleSizes["4xl"] = "4xl";
})(TitleSizes || (TitleSizes = {}));
var headingLevelSizeMap;
(function (headingLevelSizeMap) {
    headingLevelSizeMap["h1"] = "2xl";
    headingLevelSizeMap["h2"] = "xl";
    headingLevelSizeMap["h3"] = "lg";
    headingLevelSizeMap["h4"] = "md";
    headingLevelSizeMap["h5"] = "md";
    headingLevelSizeMap["h6"] = "md";
})(headingLevelSizeMap || (headingLevelSizeMap = {}));
export const Title = (_a) => {
    var { className = '', children = '', headingLevel: HeadingLevel, size = headingLevelSizeMap[HeadingLevel], ouiaId, ouiaSafe = true } = _a, props = __rest(_a, ["className", "children", "headingLevel", "size", "ouiaId", "ouiaSafe"]);
    const ouiaProps = useOUIAProps(Title.displayName, ouiaId, ouiaSafe);
    return (React.createElement(HeadingLevel, Object.assign({}, ouiaProps, props, { className: css(styles.title, size && styles.modifiers[size], className) }), children));
};
Title.displayName = 'Title';
//# sourceMappingURL=Title.js.map