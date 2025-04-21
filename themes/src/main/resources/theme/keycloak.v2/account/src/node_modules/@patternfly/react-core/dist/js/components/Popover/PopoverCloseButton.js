"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PopoverCloseButton = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Button_1 = require("../Button");
const times_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-icon'));
const FindRefWrapper_1 = require("../../helpers/Popper/FindRefWrapper");
const PopoverCloseButton = (_a) => {
    var { onClose = () => undefined } = _a, props = tslib_1.__rest(_a, ["onClose"]);
    const [closeButtonElement, setCloseButtonElement] = React.useState(null);
    React.useEffect(() => {
        closeButtonElement && closeButtonElement.addEventListener('click', onClose, false);
        return () => {
            closeButtonElement && closeButtonElement.removeEventListener('click', onClose, false);
        };
    }, [closeButtonElement]);
    return (React.createElement(FindRefWrapper_1.FindRefWrapper, { onFoundRef: (foundRef) => setCloseButtonElement(foundRef) },
        React.createElement(Button_1.Button, Object.assign({ variant: "plain", "aria-label": true }, props, { style: { pointerEvents: 'auto' } }),
            React.createElement(times_icon_1.default, null))));
};
exports.PopoverCloseButton = PopoverCloseButton;
exports.PopoverCloseButton.displayName = 'PopoverCloseButton';
//# sourceMappingURL=PopoverCloseButton.js.map