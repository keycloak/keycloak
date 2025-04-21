import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DualListSelector/dual-list-selector';
import { css } from '@patternfly/react-styles';
import { handleArrows } from '../../helpers';
export const DualListSelectorControlsWrapperBase = (_a) => {
    var { innerRef, children = null, className, 'aria-label': ariaLabel = 'Controls for moving options between lists' } = _a, props = __rest(_a, ["innerRef", "children", "className", 'aria-label']);
    const wrapperRef = innerRef || React.useRef(null);
    // Adds keyboard navigation to the dynamically built dual list selector controls. Works when controls are dynamically built
    // as well as when they are passed in via children.
    const handleKeys = (event) => {
        if (!wrapperRef.current ||
            (wrapperRef.current !== event.target.closest('.pf-c-dual-list-selector__controls') &&
                !Array.from(wrapperRef.current.getElementsByClassName('pf-c-dual-list-selector__controls')).includes(event.target.closest('.pf-c-dual-list-selector__controls')))) {
            return;
        }
        event.stopImmediatePropagation();
        const controls = Array.from(wrapperRef.current.getElementsByTagName('BUTTON')).filter(el => !el.classList.contains('pf-m-disabled'));
        const activeElement = document.activeElement;
        handleArrows(event, controls, (element) => activeElement.contains(element), (element) => element, undefined, undefined, true, false);
    };
    React.useEffect(() => {
        window.addEventListener('keydown', handleKeys);
        return () => {
            window.removeEventListener('keydown', handleKeys);
        };
    }, [wrapperRef.current]);
    return (React.createElement("div", Object.assign({ className: css(styles.dualListSelectorControls, className), tabIndex: 0, ref: wrapperRef, "aria-label": ariaLabel }, props), children));
};
DualListSelectorControlsWrapperBase.displayName = 'DualListSelectorControlsWrapperBase';
export const DualListSelectorControlsWrapper = React.forwardRef((props, ref) => (React.createElement(DualListSelectorControlsWrapperBase, Object.assign({ innerRef: ref }, props))));
DualListSelectorControlsWrapper.displayName = 'DualListSelectorControlsWrapper';
//# sourceMappingURL=DualListSelectorControlsWrapper.js.map