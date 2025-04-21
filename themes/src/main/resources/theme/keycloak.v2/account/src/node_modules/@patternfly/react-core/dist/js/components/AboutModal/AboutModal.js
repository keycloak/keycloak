"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AboutModal = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const ReactDOM = tslib_1.__importStar(require("react-dom"));
const react_styles_1 = require("@patternfly/react-styles");
const backdrop_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Backdrop/backdrop"));
const helpers_1 = require("../../helpers");
const constants_1 = require("../../helpers/constants");
const AboutModalContainer_1 = require("./AboutModalContainer");
class AboutModal extends React.Component {
    constructor(props) {
        super(props);
        this.id = AboutModal.currentId++;
        this.ariaLabelledBy = `pf-about-modal-title-${this.id}`;
        this.ariaDescribedBy = `pf-about-modal-content-${this.id}`;
        this.handleEscKeyClick = (event) => {
            if (event.keyCode === constants_1.KEY_CODES.ESCAPE_KEY && this.props.isOpen) {
                this.props.onClose();
            }
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
        this.getElement = (appendTo) => {
            if (typeof appendTo === 'function') {
                return appendTo();
            }
            return appendTo || document.body;
        };
        this.state = {
            container: undefined
        };
        if (props.brandImageSrc && !props.brandImageAlt) {
            // eslint-disable-next-line no-console
            console.error('AboutModal:', 'brandImageAlt is required when a brandImageSrc is specified');
        }
    }
    componentDidMount() {
        const container = document.createElement('div');
        const target = this.getElement(this.props.appendTo);
        this.setState({ container });
        target.appendChild(container);
        target.addEventListener('keydown', this.handleEscKeyClick, false);
        if (this.props.isOpen) {
            target.classList.add(react_styles_1.css(backdrop_1.default.backdropOpen));
        }
        else {
            target.classList.remove(react_styles_1.css(backdrop_1.default.backdropOpen));
        }
    }
    componentDidUpdate() {
        const target = this.getElement(this.props.appendTo);
        if (this.props.isOpen) {
            target.classList.add(react_styles_1.css(backdrop_1.default.backdropOpen));
            this.toggleSiblingsFromScreenReaders(true);
        }
        else {
            target.classList.remove(react_styles_1.css(backdrop_1.default.backdropOpen));
            this.toggleSiblingsFromScreenReaders(false);
        }
    }
    componentWillUnmount() {
        const target = this.getElement(this.props.appendTo);
        if (this.state.container) {
            target.removeChild(this.state.container);
        }
        target.removeEventListener('keydown', this.handleEscKeyClick, false);
        target.classList.remove(react_styles_1.css(backdrop_1.default.backdropOpen));
    }
    render() {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const _a = this.props, { appendTo } = _a, props = tslib_1.__rest(_a, ["appendTo"]);
        const { container } = this.state;
        if (!helpers_1.canUseDOM || !container) {
            return null;
        }
        return ReactDOM.createPortal(React.createElement(AboutModalContainer_1.AboutModalContainer, Object.assign({ aboutModalBoxHeaderId: this.ariaLabelledBy, aboutModalBoxContentId: this.ariaDescribedBy }, props)), container);
    }
}
exports.AboutModal = AboutModal;
AboutModal.displayName = 'AboutModal';
AboutModal.currentId = 0;
AboutModal.defaultProps = {
    className: '',
    isOpen: false,
    onClose: () => undefined,
    productName: '',
    trademark: '',
    backgroundImageSrc: '',
    noAboutModalBoxContentContainer: false,
    appendTo: null
};
//# sourceMappingURL=AboutModal.js.map