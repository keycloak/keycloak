import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Backdrop/backdrop';
import { canUseDOM } from '../../helpers';
import { KEY_CODES } from '../../helpers/constants';
import { AboutModalContainer } from './AboutModalContainer';
import { PickOptional } from '../../helpers/typeUtils';

export interface AboutModalProps {
  /** Content rendered inside the about modal */
  children: React.ReactNode;
  /** Additional classes added to the about modal */
  className?: string;
  /** Flag to show the about modal  */
  isOpen?: boolean;
  /** A callback for when the close button is clicked  */
  onClose?: () => void;
  /** Product name  */
  productName?: string;
  /** Trademark information  */
  trademark?: string;
  /** The URL of the image for the brand  */
  brandImageSrc: string;
  /** The alternate text of the brand image  */
  brandImageAlt: string;
  /** The URL of the image for the background  */
  backgroundImageSrc?: string;
  /** Prevents the about modal from rendering content inside a container; allows for more flexible layouts  */
  noAboutModalBoxContentContainer?: boolean;
  /** The parent container to append the modal to. Defaults to document.body */
  appendTo?: HTMLElement | (() => HTMLElement);
  /** Set aria label to the close button */
  closeButtonAriaLabel?: string;
  /** Flag to disable focus trap */
  disableFocusTrap?: boolean;
}

interface ModalState {
  container: HTMLElement;
}

export class AboutModal extends React.Component<AboutModalProps, ModalState> {
  static displayName = 'AboutModal';
  private static currentId = 0;
  private id = AboutModal.currentId++;
  ariaLabelledBy = `pf-about-modal-title-${this.id}`;
  ariaDescribedBy = `pf-about-modal-content-${this.id}`;

  static defaultProps: PickOptional<AboutModalProps> = {
    className: '',
    isOpen: false,
    onClose: (): any => undefined,
    productName: '',
    trademark: '',
    backgroundImageSrc: '',
    noAboutModalBoxContentContainer: false,
    appendTo: null as HTMLElement
  };

  constructor(props: AboutModalProps) {
    super(props);

    this.state = {
      container: undefined
    };
    if (props.brandImageSrc && !props.brandImageAlt) {
      // eslint-disable-next-line no-console
      console.error('AboutModal:', 'brandImageAlt is required when a brandImageSrc is specified');
    }
  }

  handleEscKeyClick = (event: KeyboardEvent) => {
    if (event.keyCode === KEY_CODES.ESCAPE_KEY && this.props.isOpen) {
      this.props.onClose();
    }
  };

  toggleSiblingsFromScreenReaders = (hide: boolean) => {
    const { appendTo } = this.props;
    const target: HTMLElement = this.getElement(appendTo);
    const bodyChildren = target.children;
    for (const child of Array.from(bodyChildren)) {
      if (child !== this.state.container) {
        hide ? child.setAttribute('aria-hidden', '' + hide) : child.removeAttribute('aria-hidden');
      }
    }
  };

  getElement = (appendTo: HTMLElement | (() => HTMLElement)) => {
    if (typeof appendTo === 'function') {
      return appendTo();
    }
    return appendTo || document.body;
  };

  componentDidMount() {
    const container = document.createElement('div');
    const target: HTMLElement = this.getElement(this.props.appendTo);
    this.setState({ container });
    target.appendChild(container);
    target.addEventListener('keydown', this.handleEscKeyClick, false);

    if (this.props.isOpen) {
      target.classList.add(css(styles.backdropOpen));
    } else {
      target.classList.remove(css(styles.backdropOpen));
    }
  }

  componentDidUpdate() {
    const target: HTMLElement = this.getElement(this.props.appendTo);
    if (this.props.isOpen) {
      target.classList.add(css(styles.backdropOpen));
      this.toggleSiblingsFromScreenReaders(true);
    } else {
      target.classList.remove(css(styles.backdropOpen));
      this.toggleSiblingsFromScreenReaders(false);
    }
  }

  componentWillUnmount() {
    const target: HTMLElement = this.getElement(this.props.appendTo);
    if (this.state.container) {
      target.removeChild(this.state.container);
    }
    target.removeEventListener('keydown', this.handleEscKeyClick, false);
    target.classList.remove(css(styles.backdropOpen));
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { appendTo, ...props } = this.props;
    const { container } = this.state;

    if (!canUseDOM || !container) {
      return null;
    }

    return ReactDOM.createPortal(
      <AboutModalContainer
        aboutModalBoxHeaderId={this.ariaLabelledBy}
        aboutModalBoxContentId={this.ariaDescribedBy}
        {...props}
      />,
      container
    ) as React.ReactElement;
  }
}
