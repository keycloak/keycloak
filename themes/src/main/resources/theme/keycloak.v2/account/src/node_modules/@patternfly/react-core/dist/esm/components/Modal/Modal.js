import { __rest } from "tslib";
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { canUseDOM, KEY_CODES } from '../../helpers';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Backdrop/backdrop';
import { ModalContent } from './ModalContent';
import { getDefaultOUIAId } from '../../helpers';
export var ModalVariant;
(function (ModalVariant) {
    ModalVariant["small"] = "small";
    ModalVariant["medium"] = "medium";
    ModalVariant["large"] = "large";
    ModalVariant["default"] = "default";
})(ModalVariant || (ModalVariant = {}));
export class Modal extends React.Component {
    constructor(props) {
        super(props);
        this.boxId = '';
        this.labelId = '';
        this.descriptorId = '';
        this.handleEscKeyClick = (event) => {
            const { onEscapePress } = this.props;
            if (event.keyCode === KEY_CODES.ESCAPE_KEY && this.props.isOpen) {
                onEscapePress ? onEscapePress(event) : this.props.onClose();
            }
        };
        this.getElement = (appendTo) => {
            if (typeof appendTo === 'function') {
                return appendTo();
            }
            return appendTo || document.body;
        };
        this.toggleSiblingsFromScreenReaders = (hide) => {
            const { appendTo } = this.props;
            const target = this.getElement(appendTo);
            const bodyChildren = target.children;
            for (const child of Array.from(bodyChildren)) {
                if (child !== this.state.container) {
                    hide ? child.setAttribute('aria-hidden', '' + hide) : child.removeAttribute('aria-hidden');
                }
            }
        };
        this.isEmpty = (value) => value === null || value === undefined || value === '';
        const boxIdNum = Modal.currentId++;
        const labelIdNum = boxIdNum + 1;
        const descriptorIdNum = boxIdNum + 2;
        this.boxId = props.id || `pf-modal-part-${boxIdNum}`;
        this.labelId = `pf-modal-part-${labelIdNum}`;
        this.descriptorId = `pf-modal-part-${descriptorIdNum}`;
        this.state = {
            container: undefined,
            ouiaStateId: getDefaultOUIAId(Modal.displayName, props.variant)
        };
    }
    componentDidMount() {
        const { appendTo, title, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledby, hasNoBodyWrapper, header } = this.props;
        const target = this.getElement(appendTo);
        const container = document.createElement('div');
        this.setState({ container });
        target.appendChild(container);
        target.addEventListener('keydown', this.handleEscKeyClick, false);
        if (this.props.isOpen) {
            target.classList.add(css(styles.backdropOpen));
        }
        else {
            target.classList.remove(css(styles.backdropOpen));
        }
        if (this.isEmpty(title) && this.isEmpty(ariaLabel) && this.isEmpty(ariaLabelledby)) {
            // eslint-disable-next-line no-console
            console.error('Modal: Specify at least one of: title, aria-label, aria-labelledby.');
        }
        if (this.isEmpty(ariaLabel) && this.isEmpty(ariaLabelledby) && (hasNoBodyWrapper || header)) {
            // eslint-disable-next-line no-console
            console.error('Modal: When using hasNoBodyWrapper or setting a custom header, ensure you assign an accessible name to the the modal container with aria-label or aria-labelledby.');
        }
    }
    componentDidUpdate() {
        const { appendTo } = this.props;
        const target = this.getElement(appendTo);
        if (this.props.isOpen) {
            target.classList.add(css(styles.backdropOpen));
            this.toggleSiblingsFromScreenReaders(true);
        }
        else {
            target.classList.remove(css(styles.backdropOpen));
            this.toggleSiblingsFromScreenReaders(false);
        }
    }
    componentWillUnmount() {
        const { appendTo } = this.props;
        const target = this.getElement(appendTo);
        if (this.state.container) {
            target.removeChild(this.state.container);
        }
        target.removeEventListener('keydown', this.handleEscKeyClick, false);
        target.classList.remove(css(styles.backdropOpen));
    }
    render() {
        const _a = this.props, { 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        appendTo, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onEscapePress, 'aria-labelledby': ariaLabelledby, 'aria-label': ariaLabel, 'aria-describedby': ariaDescribedby, bodyAriaLabel, bodyAriaRole, title, titleIconVariant, titleLabel, ouiaId, ouiaSafe } = _a, props = __rest(_a, ["appendTo", "onEscapePress", 'aria-labelledby', 'aria-label', 'aria-describedby', "bodyAriaLabel", "bodyAriaRole", "title", "titleIconVariant", "titleLabel", "ouiaId", "ouiaSafe"]);
        const { container } = this.state;
        if (!canUseDOM || !container) {
            return null;
        }
        return ReactDOM.createPortal(React.createElement(ModalContent, Object.assign({}, props, { boxId: this.boxId, labelId: this.labelId, descriptorId: this.descriptorId, title: title, titleIconVariant: titleIconVariant, titleLabel: titleLabel, "aria-label": ariaLabel, "aria-describedby": ariaDescribedby, "aria-labelledby": ariaLabelledby, bodyAriaLabel: bodyAriaLabel, bodyAriaRole: bodyAriaRole, ouiaId: ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe: ouiaSafe })), container);
    }
}
Modal.displayName = 'Modal';
Modal.currentId = 0;
Modal.defaultProps = {
    className: '',
    isOpen: false,
    title: '',
    titleIconVariant: null,
    titleLabel: '',
    'aria-label': '',
    showClose: true,
    'aria-describedby': '',
    'aria-labelledby': '',
    id: undefined,
    actions: [],
    onClose: () => undefined,
    variant: 'default',
    hasNoBodyWrapper: false,
    appendTo: () => document.body,
    ouiaSafe: true
};
//# sourceMappingURL=Modal.js.map