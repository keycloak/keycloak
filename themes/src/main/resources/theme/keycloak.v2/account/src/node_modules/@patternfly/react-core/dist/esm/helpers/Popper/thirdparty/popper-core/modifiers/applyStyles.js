import getNodeName from '../dom-utils/getNodeName';
import { isHTMLElement } from '../dom-utils/instanceOf';
// This modifier takes the styles prepared by the `computeStyles` modifier
// and applies them to the HTMLElements such as popper and arrow
/**
 *
 */
function applyStyles({ state }) {
    Object.keys(state.elements).forEach(name => {
        const style = state.styles[name] || {};
        const attributes = state.attributes[name] || {};
        const element = state.elements[name];
        // arrow is optional + virtual elements
        if (!isHTMLElement(element) || !getNodeName(element)) {
            return;
        }
        // Flow doesn't support to extend this property, but it's the most
        // effective way to apply styles to an HTMLElement
        // $FlowFixMe
        Object.assign(element.style, style);
        Object.keys(attributes).forEach(name => {
            const value = attributes[name];
            if (value === false) {
                element.removeAttribute(name);
            }
            else {
                element.setAttribute(name, value === true ? '' : value);
            }
        });
    });
}
/**
 *
 */
function effect({ state }) {
    const initialStyles = {
        popper: {
            position: state.options.strategy,
            left: '0',
            top: '0',
            margin: '0'
        },
        arrow: {
            position: 'absolute'
        },
        reference: {}
    };
    Object.assign(state.elements.popper.style, initialStyles.popper);
    if (state.elements.arrow) {
        Object.assign(state.elements.arrow.style, initialStyles.arrow);
    }
    return () => {
        Object.keys(state.elements).forEach(name => {
            const element = state.elements[name];
            const attributes = state.attributes[name] || {};
            const styleProperties = Object.keys(state.styles.hasOwnProperty(name) ? state.styles[name] : initialStyles[name]);
            // Set all values to an empty string to unset them
            const style = styleProperties.reduce((style, property) => {
                style[property] = '';
                return style;
            }, {});
            // arrow is optional + virtual elements
            if (!isHTMLElement(element) || !getNodeName(element)) {
                return;
            }
            // Flow doesn't support to extend this property, but it's the most
            // effective way to apply styles to an HTMLElement
            // $FlowFixMe
            Object.assign(element.style, style);
            Object.keys(attributes).forEach(attribute => {
                element.removeAttribute(attribute);
            });
        });
    };
}
export default {
    name: 'applyStyles',
    enabled: true,
    phase: 'write',
    fn: applyStyles,
    effect,
    requires: ['computeStyles']
};
//# sourceMappingURL=applyStyles.js.map