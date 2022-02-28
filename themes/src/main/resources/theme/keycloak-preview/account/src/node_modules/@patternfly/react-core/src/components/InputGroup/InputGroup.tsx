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
}

export const InputGroup: React.FunctionComponent<InputGroupProps> = ({
  className = '',
  children,
  ...props
}: InputGroupProps) => {
  const formCtrls = [FormSelect, TextArea, TextInput].map(comp => comp.toString());
  const idItem = React.Children.toArray(children).find(
    (child: any) => !formCtrls.includes(child.type.toString()) && child.props.id
  ) as React.ReactElement<{ id: string }>;
  return (
    <div className={css(styles.inputGroup, className)} {...props}>
      {idItem
        ? React.Children.map(children, (child: any) =>
            formCtrls.includes(child.type.toString())
              ? React.cloneElement(child, { 'aria-describedby': idItem.props.id })
              : child
          )
        : children}
    </div>
  );
};
