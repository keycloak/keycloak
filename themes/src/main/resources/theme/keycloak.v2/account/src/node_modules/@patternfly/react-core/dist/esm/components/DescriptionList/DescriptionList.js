import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DescriptionList/description-list';
import { formatBreakpointMods } from '../../helpers';
const setBreakpointModifiers = (prefix, modifiers) => {
    const mods = modifiers;
    return Object.keys(mods || {}).reduce((acc, curr) => curr === 'default' ? Object.assign(Object.assign({}, acc), { [prefix]: mods[curr] }) : Object.assign(Object.assign({}, acc), { [`${prefix}-on-${curr}`]: mods[curr] }), {});
};
export const DescriptionList = (_a) => {
    var { className = '', children = null, isHorizontal = false, isAutoColumnWidths, isAutoFit, isInlineGrid, isCompact, isFluid, isFillColumns, columnModifier, autoFitMinModifier, horizontalTermWidthModifier, orientation, style } = _a, props = __rest(_a, ["className", "children", "isHorizontal", "isAutoColumnWidths", "isAutoFit", "isInlineGrid", "isCompact", "isFluid", "isFillColumns", "columnModifier", "autoFitMinModifier", "horizontalTermWidthModifier", "orientation", "style"]);
    if (isAutoFit && autoFitMinModifier) {
        style = Object.assign(Object.assign({}, style), setBreakpointModifiers('--pf-c-description-list--GridTemplateColumns--min', autoFitMinModifier));
    }
    if (isHorizontal && horizontalTermWidthModifier) {
        style = Object.assign(Object.assign({}, style), setBreakpointModifiers('--pf-c-description-list--m-horizontal__term--width', horizontalTermWidthModifier));
    }
    return (React.createElement("dl", Object.assign({ className: css(styles.descriptionList, (isHorizontal || isFluid) && styles.modifiers.horizontal, isAutoColumnWidths && styles.modifiers.autoColumnWidths, isAutoFit && styles.modifiers.autoFit, formatBreakpointMods(columnModifier, styles), formatBreakpointMods(orientation, styles), isInlineGrid && styles.modifiers.inlineGrid, isCompact && styles.modifiers.compact, isFluid && styles.modifiers.fluid, isFillColumns && styles.modifiers.fillColumns, className), style: style }, props), children));
};
DescriptionList.displayName = 'DescriptionList';
//# sourceMappingURL=DescriptionList.js.map