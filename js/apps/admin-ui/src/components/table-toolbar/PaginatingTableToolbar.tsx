import {
  Pagination,
  ToggleTemplateProps,
  ToolbarItem,
} from "@patternfly/react-core";
import { PropsWithChildren, ReactNode } from "react";

import { TableToolbar } from "./TableToolbar";

type TableToolbarProps = {
  count: number;
  first: number;
  max: number;
  onNextClick: (page: number) => void;
  onPreviousClick: (page: number) => void;
  onPerPageSelect: (max: number, first: number) => void;
  searchTypeComponent?: ReactNode;
  toolbarItem?: ReactNode;
  subToolbar?: ReactNode;
  inputGroupName?: string;
  inputGroupPlaceholder?: string;
  inputGroupOnEnter?: (value: string) => void;
};

export const PaginatingTableToolbar = ({
  count,
  first,
  max,
  onNextClick,
  onPreviousClick,
  onPerPageSelect,
  searchTypeComponent,
  toolbarItem,
  subToolbar,
  children,
  inputGroupName,
  inputGroupPlaceholder,
  inputGroupOnEnter,
}: PropsWithChildren<TableToolbarProps>) => {
  const page = Math.round(first / max);
  const KeycloakPagination = ({
    variant = "top",
  }: {
    variant?: "top" | "bottom";
  }) => (
    <Pagination
      isCompact
      toggleTemplate={({ firstIndex, lastIndex }: ToggleTemplateProps) => (
        <b>
          {firstIndex} - {lastIndex}
        </b>
      )}
      itemCount={count + page * max}
      page={page + 1}
      perPage={max}
      onNextClick={(_, p) => onNextClick((p - 1) * max)}
      onPreviousClick={(_, p) => onPreviousClick((p - 1) * max)}
      onPerPageSelect={(_, m, f) => onPerPageSelect(f - 1, m)}
      variant={variant}
    />
  );

  return (
    <TableToolbar
      searchTypeComponent={searchTypeComponent}
      toolbarItem={
        <>
          {toolbarItem}
          <ToolbarItem variant="pagination">
            <KeycloakPagination />
          </ToolbarItem>
        </>
      }
      subToolbar={subToolbar}
      toolbarItemFooter={
        count !== 0 ? (
          <ToolbarItem>
            <KeycloakPagination variant="bottom" />
          </ToolbarItem>
        ) : null
      }
      inputGroupName={inputGroupName}
      inputGroupPlaceholder={inputGroupPlaceholder}
      inputGroupOnEnter={inputGroupOnEnter}
    >
      {children}
    </TableToolbar>
  );
};
