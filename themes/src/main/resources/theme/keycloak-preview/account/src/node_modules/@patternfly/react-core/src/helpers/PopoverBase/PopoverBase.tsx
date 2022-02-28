import * as React from 'react';
import * as ReactDOM from 'react-dom';
import tippy, { Instance, Props, Content } from 'tippy.js';

// eslint-disable-next-line @typescript-eslint/interface-name-prefix
export interface ITippyProps {
  /** Props for tippy */
  [_: string]: any;
}

interface PopoverBaseProps extends ITippyProps {
  children: React.ReactNode;
  content: React.ReactNode;
  isEnabled?: boolean;
  isVisible?: boolean;
  onCreate?: (tippy: Instance<Props>) => void;
  trigger?: string;
}

// These props are not native to `tippy.js` and are specific to React only.
const REACT_ONLY_PROPS = ['children', 'onCreate', 'isVisible', 'isEnabled'];
/** Avoid Babel's large '_objectWithoutProperties' helper function.
 *
 * @param {object} props - Props object
 */
function getNativeTippyProps(props: ITippyProps) {
  return Object.keys(props)
    .filter(prop => !REACT_ONLY_PROPS.includes(prop))
    .reduce(
      (acc, key) => {
        acc[key] = props[key];
        return acc;
      },
      {} as ITippyProps
    );
}

interface PopoverBaseState {
  isMounted: boolean;
}

class PopoverBase extends React.Component<PopoverBaseProps, PopoverBaseState> {
  state = { isMounted: false };
  container = typeof document !== 'undefined' && document.createElement('div');
  tip: Instance<Props>;

  static defaultProps = {
    trigger: 'mouseenter focus'
  };

  get isReactElementContent() {
    return React.isValidElement(this.props.content);
  }

  get options() {
    return {
      ...getNativeTippyProps(this.props),
      content: this.isReactElementContent ? this.container : (this.props.content as Content)
    };
  }

  get isManualTrigger() {
    return this.props.trigger === 'manual';
  }

  componentDidMount() {
    this.setState({ isMounted: true });

    /* eslint-disable-next-line */
    this.tip = tippy(ReactDOM.findDOMNode(this) as HTMLElement, this.options);

    const { onCreate, isEnabled, isVisible } = this.props;

    if (onCreate) {
      onCreate(this.tip);
    }

    if (isEnabled === false) {
      this.tip.disable();
    }

    if (this.isManualTrigger && isVisible === true) {
      this.tip.show();
    }
  }

  componentDidUpdate() {
    this.tip.setProps(this.options);

    const { isEnabled, isVisible } = this.props;

    if (isEnabled === true) {
      this.tip.enable();
    }
    if (isEnabled === false) {
      this.tip.disable();
    }

    if (this.isManualTrigger) {
      if (isVisible === true) {
        this.tip.show();
      }
      if (isVisible === false) {
        this.tip.hide();
      }
    }
  }

  componentWillUnmount() {
    this.tip.destroy();
    this.tip = null;
  }

  render() {
    return (
      <React.Fragment>
        {this.props.children}
        {this.isReactElementContent &&
          this.state.isMounted &&
          ReactDOM.createPortal(this.props.content, this.container)}
      </React.Fragment>
    );
  }
}

export default PopoverBase;
