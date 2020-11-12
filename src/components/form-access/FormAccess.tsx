import React, {
  Children,
  cloneElement,
  isValidElement,
  ReactElement,
} from "react";
import { Controller } from "react-hook-form";
import {
  ActionGroup,
  Form,
  FormGroup,
  FormProps,
  Grid,
  GridItem,
  TextArea,
} from "@patternfly/react-core";
import { AccessType } from "keycloak-admin/lib/defs/whoAmIRepresentation";

import { useAccess } from "../../context/access/Access";

export type FormAccessProps = FormProps & {
  /**
   * One of the AccessType's that the user needs to have to view this form. Also see {@link useAccess}.
   * @type {AccessType}
   */
  role: AccessType;

  /**
   * An override property if fine grained access has been setup for this form.
   * @type {boolean}
   */
  fineGrainedAccess?: boolean;

  /**
   * Set unWrap when you don't want this component to wrap your "children" in a {@link Form} component.
   * @type {boolean}
   */
  unWrap?: boolean;
  children: ReactElement[];
};

/**
 * Use this in place of a patternfly Form component and add the `role` and `fineGrainedAccess` properties.
 * @param {FormAccessProps} param0 - all properties of Form + role and fineGrainedAccess
 */
export const FormAccess = ({
  children,
  role,
  fineGrainedAccess = false,
  unWrap = false,
  ...rest
}: FormAccessProps) => {
  const { hasAccess } = useAccess();

  const recursiveCloneChildren = (
    children: ReactElement[],
    newProps: any
  ): ReactElement[] => {
    return Children.map(children, (child) => {
      if (!isValidElement(child)) {
        return child;
      }

      if (child.props) {
        const element = child as ReactElement;
        if (child.type === Controller) {
          return cloneElement(child, {
            ...element.props,
            render: (props: any) => {
              const renderElement = element.props.render(props);
              return cloneElement(renderElement, {
                value: props.value,
                onChange: props.onChange,
                ...newProps,
              });
            },
          });
        }
        const children = recursiveCloneChildren(
          element.props.children,
          newProps
        );
        if (child.type === TextArea) {
          return cloneElement(child, {
            readOnly: newProps.isDisabled,
            children,
          } as any);
        }
        return cloneElement(
          child,
          child.type === FormGroup ||
            child.type === GridItem ||
            child.type === Grid ||
            child.type === ActionGroup
            ? { children }
            : { ...newProps, children }
        );
      }
      return child;
    });
  };
  return (
    <>
      {!unWrap && (
        <Form {...rest}>
          {recursiveCloneChildren(children, {
            isDisabled: !hasAccess(role) && !fineGrainedAccess,
          })}
        </Form>
      )}
      {unWrap &&
        recursiveCloneChildren(children, {
          isDisabled: !hasAccess(role) && !fineGrainedAccess,
        })}
    </>
  );
};
