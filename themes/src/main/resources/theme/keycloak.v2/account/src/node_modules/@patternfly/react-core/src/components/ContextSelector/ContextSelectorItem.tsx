import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';
import { ContextSelectorContext } from './contextSelectorConstants';

export interface ContextSelectorItemProps {
  /** Anything which can be rendered as Context Selector item */
  children?: React.ReactNode;
  /** Classes applied to root element of the Context Selector item */
  className?: string;
  /** Render Context  Selector item as disabled */
  isDisabled?: boolean;
  /** Callback for click event */
  onClick: (event: React.MouseEvent) => void;
  /** @hide internal index of the item */
  index: number;
  /** Internal callback for ref tracking */
  sendRef: (index: number, current: any) => void;
  /** Link href, indicates item should render as anchor tag */
  href?: string;
}

export class ContextSelectorItem extends React.Component<ContextSelectorItemProps> {
  static displayName = 'ContextSelectorItem';
  static defaultProps: ContextSelectorItemProps = {
    children: null as React.ReactNode,
    className: '',
    isDisabled: false,
    onClick: (): any => undefined,
    index: undefined as number,
    sendRef: () => {},
    href: null as string
  };

  ref: React.RefObject<HTMLButtonElement & HTMLAnchorElement> = React.createRef();

  componentDidMount() {
    /* eslint-disable-next-line */
    this.props.sendRef(this.props.index, this.ref.current);
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { className, children, onClick, isDisabled, index, sendRef, href, ...props } = this.props;
    const Component = href ? 'a' : 'button';
    const isDisabledLink = href && isDisabled;
    return (
      <ContextSelectorContext.Consumer>
        {({ onSelect }) => (
          <li role="none">
            <Component
              className={css(
                styles.contextSelectorMenuListItem,
                isDisabledLink && styles.modifiers.disabled,
                className
              )}
              ref={this.ref}
              onClick={event => {
                if (!isDisabled) {
                  onClick(event);
                  onSelect(event, children);
                }
              }}
              disabled={isDisabled && !href}
              href={href}
              {...(isDisabledLink && { 'aria-disabled': true, tabIndex: -1 })}
              {...props}
            >
              {children}
            </Component>
          </li>
        )}
      </ContextSelectorContext.Consumer>
    );
  }
}
