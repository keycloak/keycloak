import { KeyboardEvent } from "react";
import { Card, CardProps } from "@patternfly/react-core";

type ClickableCardProps = Omit<CardProps, "onClick"> & {
  onClick: () => void;
};

export const ClickableCard = ({
  children,
  onClick,
  ...rest
}: ClickableCardProps) => {
  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === " " || e.key === "Enter" || e.key === "Spacebar") {
      onClick();
    }
  };
  return (
    <Card
      className="keycloak-empty-state-card"
      role="button"
      aria-pressed="false"
      tabIndex={0}
      isSelectable
      onKeyDown={onKeyDown}
      onClick={onClick}
      {...rest}
    >
      {children}
    </Card>
  );
};
