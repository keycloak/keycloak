import { TextArea, TextAreaProps } from "@patternfly/react-core";
import { ComponentProps, forwardRef, HTMLProps } from "react";

// PatternFly changes the signature of the 'onChange' handler for textarea elements.
// This causes issues with React Hook Form as it expects the default signature for a textarea element.
// So we have to create this wrapper component that takes care of converting these signatures for us.

export type KeycloakTextAreaProps = Omit<
  ComponentProps<typeof TextArea>,
  "onChange"
> &
  Pick<HTMLProps<HTMLTextAreaElement>, "onChange">;

export const KeycloakTextArea = forwardRef<
  HTMLTextAreaElement,
  KeycloakTextAreaProps
>(({ onChange, ...props }, ref) => {
  const onChangeForward: TextAreaProps["onChange"] = (_, event) =>
    onChange?.(event);

  return <TextArea {...props} ref={ref} onChange={onChangeForward} />;
});

// We need to fake the displayName to match what PatternFly expects.
// This is because PatternFly uses it to filter children in certain aspects.
// This is a stupid approach, but it's not like we can change that.
KeycloakTextArea.displayName = "TextArea";
