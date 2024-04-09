import { KeyboardEvent } from "react";
import { Card, CardHeader, CardProps } from "@patternfly/react-core";

type ClickableCardProps = Omit<CardProps, "onClick"> & {
  id: string;
  onClick: () => void;
};

export const ClickableCard = ({
  id,
  onClick,
  children,
  ...rest
}: ClickableCardProps) => {
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
