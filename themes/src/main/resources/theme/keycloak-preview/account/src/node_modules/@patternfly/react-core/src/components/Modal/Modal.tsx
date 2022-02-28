import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { canUseDOM } from '../../helpers';

import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Backdrop/backdrop';

import { KEY_CODES } from '../../helpers/constants';
import { ModalContent } from './ModalContent';
import { PickOptional } from '../../helpers/typeUtils';

export interface ModalProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the Modal. */
  children: React.ReactNode;
  /** Additional classes added to the Modal */
  className?: string;
  /** Flag to show the modal */
  isOpen?: boolean;
  /** Complex header (more than just text), supersedes title for header content */
  header?: React.ReactNode;
  /** Simple text content of the Modal Header, also used for aria-label on the body */
  title: string;
  /** Flag to hide the title */
  hideTitle?: boolean;
  /** Flag to show the close button in the header area of the modal */
  showClose?: boolean;
  /** Id to use for Modal Box description */
  ariaDescribedById?: string;
  /** Custom footer */
  footer?: React.ReactNode;
  /** Action buttons to add to the standard Modal Footer, ignored if `footer` is given */
  actions?: any;
  /** Flag to indicate that the Footer content is left aligned */
  isFooterLeftAligned?: boolean;
  /** A callback for when the close button is clicked */
  onClose?: () => void;
  /** Default width of the Modal. */
  width?: number | string;
  /** Creates a large version of the Modal */
  isLarge?: boolean;
  /** Creates a small version of the Modal */
  isSmall?: boolean;
  /** The parent container to append the modal to. Defaults to document.body */
  appendTo?: HTMLElement | (() => HTMLElement);
  /** Flag to disable focus trap */
  disableFocusTrap?: boolean;
  /** Description of the modal */
  description?: React.ReactNode;
}

interface ModalState {
  container: HTMLElement;
}

export class Modal extends React.Component<ModalProps, ModalState> {
  static currentId = 0;
  id = '';

  static defaultProps: PickOptional<ModalProps> = {
    className: '',
    isOpen: false,
    hideTitle: false,
    showClose: true,
    ariaDescribedById: '',
    actions: [] as any[],
    isFooterLeftAligned: false,
    onClose: () => undefined as any,
    isLarge: false,
    isSmall: false,
    appendTo: (typeof document !== 'undefined' && document.body) || null
  };

  constructor(props: ModalProps) {
    super(props);
    const newId = Modal.currentId++;
    this.id = `pf-modal-${newId}`;

    this.state = {
      container: undefined
    };
  }

  handleEscKeyClick = (event: KeyboardEvent): void => {
    if (event.keyCode === KEY_CODES.ESCAPE_KEY && this.props.isOpen) {
      this.props.onClose();
    }
  };

  getElement = (appendTo: HTMLElement | (() => HTMLElement)) => {
    let target: HTMLElement;
    if (typeof appendTo === 'function') {
      target = appendTo();
    } else {
      target = appendTo;
    }
    return target;
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

  componentDidMount() {
    const { appendTo } = this.props;
    const target: HTMLElement = this.getElement(appendTo);
    const container = document.createElement('div');
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
    const { appendTo } = this.props;
    const target: HTMLElement = this.getElement(appendTo);
    if (this.props.isOpen) {
      target.classList.add(css(styles.backdropOpen));
      this.toggleSiblingsFromScreenReaders(true);
    } else {
      target.classList.remove(css(styles.backdropOpen));
      this.toggleSiblingsFromScreenReaders(false);
    }
  }

  componentWillUnmount() {
    const { appendTo } = this.props;
    const target: HTMLElement = this.getElement(appendTo);
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
      <ModalContent
        {...props}
        title={this.props.title}
        id={this.id}
        ariaDescribedById={this.props.ariaDescribedById}
      />,
      container
    );
  }
}
