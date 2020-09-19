import React, { MouseEventHandler } from "react";
import {
  EmptyState,
  EmptyStateIcon,
  EmptyStateBody,
  Title,
  Button,
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";

export type ListEmptyStateProps = {
  message: string;
  instructions: string;
  primaryActionText: string;
  onPrimaryAction: MouseEventHandler<HTMLButtonElement>;
};

export const ListEmptyState = ({
  message,
  instructions,
  primaryActionText,
  onPrimaryAction,
}: ListEmptyStateProps) => {
  return (
    <>
      <EmptyState variant="large">
        <EmptyStateIcon icon={PlusCircleIcon} />
        <Title headingLevel="h4" size="lg">
          {message}
        </Title>
        <EmptyStateBody>{instructions}</EmptyStateBody>
        <Button variant="primary" onClick={onPrimaryAction}>
          {primaryActionText}
        </Button>
      </EmptyState>
    </>
  );
};
