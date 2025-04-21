import { KeyboardEvent, useId } from "react";
import { Card, CardHeader, CardProps } from "@patternfly/react-core";

type ClickableCardProps = Omit<CardProps, "onClick"> & {
  onClick: () => void;
};

export const ClickableCard = ({
  onClick,
  children,
  ...rest
}: ClickableCardProps) => {
  const id = useId();
  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === " " || e.key === "Enter" || e.key === "Spacebar") {
      onClick();
    }
  };
  return (
    <Card id={id} isClickable onKeyDown={onKeyDown} onClick={onClick} {...rest}>
      <CardHeader
        selectableActions={{
          onClickAction: onClick,
          selectableActionId: `input-${id}`,
          selectableActionAriaLabelledby: id,
        }}
      >
        {children}
      </CardHeader>
    </Card>
  );
};
