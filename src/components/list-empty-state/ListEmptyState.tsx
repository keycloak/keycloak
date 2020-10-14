import React, { MouseEventHandler } from "react";
import {
  EmptyState,
  EmptyStateIcon,
  EmptyStateBody,
  Title,
  Button,
  ButtonVariant,
  EmptyStateSecondaryActions,
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { SearchIcon } from "@patternfly/react-icons";

export type Action = {
  text: string;
  type?: ButtonVariant;
  onClick: MouseEventHandler<HTMLButtonElement>;
};

export type ListEmptyStateProps = {
  message: string;
  instructions: string;
  primaryActionText?: string;
  onPrimaryAction?: MouseEventHandler<HTMLButtonElement>;
  hasIcon?: boolean;
  isSearchVariant?: boolean;
  secondaryActions?: Action[];
};

export const ListEmptyState = ({
  message,
  instructions,
  onPrimaryAction,
  hasIcon,
  isSearchVariant,
  primaryActionText,
  secondaryActions,
}: ListEmptyStateProps) => {
  return (
    <>
      <EmptyState variant="large">
        {hasIcon && isSearchVariant ? (
          <EmptyStateIcon icon={SearchIcon} />
        ) : (
          <EmptyStateIcon icon={PlusCircleIcon} />
        )}
        <Title headingLevel="h4" size="lg">
          {message}
        </Title>
        <EmptyStateBody>{instructions}</EmptyStateBody>
        {primaryActionText && (
          <Button variant="primary" onClick={onPrimaryAction}>
            {primaryActionText}
          </Button>
        )}
        {secondaryActions && (
          <EmptyStateSecondaryActions>
            {secondaryActions.map((action) => (
              <Button
                key={action.text}
                variant={action.type || ButtonVariant.secondary}
                onClick={action.onClick}
              >
                {action.text}
              </Button>
            ))}
          </EmptyStateSecondaryActions>
        )}
      </EmptyState>
    </>
  );
};
