import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/SimpleList/simple-list';
import { SimpleListContext } from './SimpleList';

export interface SimpleListItemProps {
  /** Content rendered inside the SimpleList item */
  children?: React.ReactNode;
  /** Additional classes added to the SimpleList <li> */
  className?: string;
  /** Component type of the SimpleList item */
  component?: 'button' | 'a';
  /** Additional classes added to the SimpleList <a> or <button> */
  componentClassName?: string;
  /** Additional props added to the SimpleList <a> or <button> */
  componentProps?: any;
  /** Indicates if the link is current/highlighted */
  isCurrent?: boolean;
  /** OnClick callback for the SimpleList item */
  onClick?: (event: React.MouseEvent | React.ChangeEvent) => void;
  /** Type of button SimpleList item */
  type?: 'button' | 'submit' | 'reset';
  /** Default hyperlink location */
  href?: string;
}

export class SimpleListItem extends React.Component<SimpleListItemProps> {
  ref = React.createRef<any>();
  static defaultProps: SimpleListItemProps = {
    children: null,
    className: '',
    isCurrent: false,
    component: 'button',
    componentClassName: '',
    type: 'button',
    href: '',
    onClick: () => {}
  };

  render() {
    const {
      children,
      isCurrent,
      className,
      component: Component,
      componentClassName,
      componentProps,
      onClick,
      type,
      href,
      ...props
    } = this.props;

    return (
      <SimpleListContext.Consumer>
        {({ currentRef, updateCurrentRef }) => {
          const isButton = Component === 'button';
          const isCurrentItem = this.ref && currentRef ? currentRef.current === this.ref.current : isCurrent;

          const additionalComponentProps = isButton
            ? {
                type
              }
            : {
                tabIndex: 0,
                href
              };

          return (
            <li className={css(className)} {...props}>
              <Component
                className={css(
                  styles.simpleListItemLink,
                  isCurrentItem && styles.modifiers.current,
                  componentClassName
                )}
                onClick={(evt: React.MouseEvent) => {
                  onClick(evt);
                  updateCurrentRef(this.ref, this.props);
                }}
                ref={this.ref}
                {...componentProps}
                {...additionalComponentProps}
              >
                {children}
              </Component>
            </li>
          );
        }}
      </SimpleListContext.Consumer>
    );
  }
}
