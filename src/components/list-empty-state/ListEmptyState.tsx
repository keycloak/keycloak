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
  id?: string,
  message: string;
  instructions: string;
  primaryActionText?: string;
  onPrimaryAction?: MouseEventHandler<HTMLButtonElement>;
  hasIcon?: boolean;
  isSearchVariant?: boolean;
  secondaryActions?: Action[];
};

export const ListEmptyState = ({
  id,
  message,
  instructions,
  onPrimaryAction,
  hasIcon = true,
  isSearchVariant,
  primaryActionText,
  secondaryActions,
}: ListEmptyStateProps) => {
  return (
    <>
      <EmptyState id={id} variant="large">
        {hasIcon && isSearchVariant ? (
          <EmptyStateIcon icon={SearchIcon} />
        ) : (
          hasIcon && <EmptyStateIcon icon={PlusCircleIcon} />
        )}
        <Title headingLevel="h4" size="lg">
          {message}
        </Title>
        <EmptyStateBody>{instructions}</EmptyStateBody>
        {primaryActionText && (
          <Button
            data-testid="empty-primary-action"
            variant="primary"
            onClick={onPrimaryAction}
          >
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
