import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/InputGroup/input-group';
import { css } from '@patternfly/react-styles';
import { FormSelect } from '../FormSelect';
import { TextArea } from '../TextArea';
import { TextInput } from '../TextInput';

export interface InputGroupProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the input group. */
  className?: string;
  /** Content rendered inside the input group. */
  children: React.ReactNode;
  /** @hide A reference object to attach to the input box */
  innerRef?: React.RefObject<any>;
}

export const InputGroup: React.FunctionComponent<InputGroupProps> = ({
  className = '',
  children,
  innerRef,
  ...props
}: InputGroupProps) => {
  const formCtrls = [FormSelect, TextArea, TextInput].map(comp => comp.displayName);
  const idItem = React.Children.toArray(children).find(
    (child: any) => !formCtrls.includes(child.type.displayName) && child.props.id
  ) as React.ReactElement<{ id: string }>;

  const inputGroupRef = innerRef || React.useRef(null);

  return (
    <div ref={inputGroupRef} className={css(styles.inputGroup, className)} {...props}>
      {idItem
        ? React.Children.map(children, (child: any) =>
            formCtrls.includes(child.type.displayName)
              ? React.cloneElement(child, { 'aria-describedby': idItem.props.id })
              : child
          )
        : children}
    </div>
  );
};
InputGroup.displayName = 'InputGroup';
