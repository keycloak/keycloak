import {
  ComponentClass,
  MouseEventHandler,
  PropsWithChildren,
  ReactNode,
} from "react";
import {
  EmptyState,
  EmptyStateBody,
  Button,
  ButtonVariant,
  EmptyStateActions,
  EmptyStateFooter,
} from "@patternfly/react-core";
import type { SVGIconProps } from "@patternfly/react-icons/dist/js/createIcon";
import { PlusCircleIcon, SearchIcon } from "@patternfly/react-icons";

export type Action = {
  text: string;
  type?: ButtonVariant;
  onClick: MouseEventHandler<HTMLButtonElement>;
};

export type ListEmptyStateProps = {
  message: string;
  instructions: ReactNode;
  primaryActionText?: string;
  onPrimaryAction?: MouseEventHandler<HTMLButtonElement>;
  hasIcon?: boolean;
  icon?: ComponentClass<SVGIconProps>;
  isSearchVariant?: boolean;
  secondaryActions?: Action[];
  isDisabled?: boolean;
};

export const ListEmptyState = ({
  message,
  instructions,
  onPrimaryAction,
  hasIcon = true,
  isSearchVariant,
  primaryActionText,
  secondaryActions,
  icon,
  isDisabled = false,
  children,
}: PropsWithChildren<ListEmptyStateProps>) => {
  return (
    <EmptyState
      headingLevel="h1"
      titleText={message}
      data-testid="empty-state"
      variant="lg"
      icon={
        hasIcon && isSearchVariant
          ? SearchIcon
          : hasIcon
            ? icon
            : PlusCircleIcon
      }
    >
      <EmptyStateBody>{instructions}</EmptyStateBody>
      <EmptyStateFooter>
        {primaryActionText && (
          <Button
            data-testid={`${message
              .replace(/\W+/g, "-")
              .toLowerCase()}-empty-action`}
            variant="primary"
            onClick={onPrimaryAction}
            isDisabled={isDisabled}
          >
            {primaryActionText}
          </Button>
        )}
        {children}
        {secondaryActions && (
          <EmptyStateActions>
            {secondaryActions.map((action) => (
              <Button
                key={action.text}
                data-testid={`${action.text
                  .replace(/\W+/g, "-")
                  .toLowerCase()}-empty-action`}
                variant={action.type || ButtonVariant.secondary}
                onClick={action.onClick}
                isDisabled={isDisabled}
              >
                {action.text}
              </Button>
            ))}
          </EmptyStateActions>
        )}
      </EmptyStateFooter>
    </EmptyState>
  );
};
