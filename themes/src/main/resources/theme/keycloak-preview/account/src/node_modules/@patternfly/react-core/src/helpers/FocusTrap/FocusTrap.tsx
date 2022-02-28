import * as React from 'react';
import createFocusTrap from 'focus-trap';
import { Options as FocusTrapOptions, FocusTrap as IFocusTrap } from 'focus-trap';

interface FocusTrapProps {
  children: React.ReactNode;
  className?: string;
  active?: boolean;
  paused?: boolean;
  focusTrapOptions?: FocusTrapOptions;
}

export class FocusTrap extends React.Component<FocusTrapProps> {
  previouslyFocusedElement: HTMLElement;
  focusTrap: IFocusTrap;
  divRef = React.createRef<HTMLDivElement>();

  static defaultProps = {
    active: true,
    paused: false,
    focusTrapOptions: {}
  };

  constructor(props: FocusTrapProps) {
    super(props);

    if (typeof document !== 'undefined') {
      this.previouslyFocusedElement = document.activeElement as HTMLElement;
    }
  }

  componentDidMount() {
    // We need to hijack the returnFocusOnDeactivate option,
    // because React can move focus into the element before we arrived at
    // this lifecycle hook (e.g. with autoFocus inputs). So the component
    // captures the previouslyFocusedElement in componentWillMount,
    // then (optionally) returns focus to it in componentWillUnmount.
    this.focusTrap = createFocusTrap(this.divRef.current, {
      ...this.props.focusTrapOptions,
      returnFocusOnDeactivate: false
    });
    if (this.props.active) {
      this.focusTrap.activate();
    }
    if (this.props.paused) {
      this.focusTrap.pause();
    }
  }

  componentDidUpdate(prevProps: FocusTrapProps) {
    if (prevProps.active && !this.props.active) {
      const { returnFocusOnDeactivate } = this.props.focusTrapOptions;
      const returnFocus = returnFocusOnDeactivate || false;
      const config = { returnFocus };
      this.focusTrap.deactivate(config);
    } else if (!prevProps.active && this.props.active) {
      this.focusTrap.activate();
    }

    if (prevProps.paused && !this.props.paused) {
      this.focusTrap.unpause();
    } else if (!prevProps.paused && this.props.paused) {
      this.focusTrap.pause();
    }
  }

  componentWillUnmount() {
    this.focusTrap.deactivate();
    if (
      this.props.focusTrapOptions.returnFocusOnDeactivate !== false &&
      this.previouslyFocusedElement &&
      this.previouslyFocusedElement.focus
    ) {
      this.previouslyFocusedElement.focus();
    }
  }

  render() {
    return (
      <div ref={this.divRef} className={this.props.className}>
        {this.props.children}
      </div>
    );
  }
}
