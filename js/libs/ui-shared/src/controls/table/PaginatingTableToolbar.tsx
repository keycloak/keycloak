import {
  Pagination,
  PaginationToggleTemplateProps,
  ToolbarItem,
} from "@patternfly/react-core";
import { PropsWithChildren, ReactNode } from "react";
import { useTranslation } from "react-i18next";

import { TableToolbar } from "./TableToolbar";

type KeycloakPaginationProps = {
  id?: string;
  count: number;
  totalCount?: number;
  first: number;
  max: number;
  onNextClick: (page: number) => void;
  onPreviousClick: (page: number) => void;
  onPerPageSelect: (max: number, first: number) => void;
  variant?: "top" | "bottom";
};

type TableToolbarProps = KeycloakPaginationProps & {
  searchTypeComponent?: ReactNode;
  toolbarItem?: ReactNode;
  subToolbar?: ReactNode;
  inputGroupName?: string;
  inputGroupPlaceholder?: string;
  inputGroupOnEnter?: (value: string) => void;
};

const KeycloakPagination = ({
  id,
  variant = "top",
  count,
  totalCount,
  first,
  max,
  onNextClick,
  onPreviousClick,
  onPerPageSelect,
}: KeycloakPaginationProps) => {
  const { t } = useTranslation();
  const page = Math.round(first / max);
  const hasTotal = typeof totalCount === "number";
  return (
    <Pagination
      widgetId={id}
      titles={{
        paginationAriaLabel: `${t("pagination")} ${variant} `,
      }}
      isCompact
      toggleTemplate={
        hasTotal
          ? undefined
          : ({ firstIndex, lastIndex }: PaginationToggleTemplateProps) => (
              <b>
                {firstIndex} - {lastIndex}
              </b>
            )
      }
      itemCount={hasTotal ? totalCount : count + page * max}
      page={page + 1}
      perPage={max}
      onNextClick={(_, p) => onNextClick((p - 1) * max)}
      onPreviousClick={(_, p) => onPreviousClick((p - 1) * max)}
      onPerPageSelect={(_, m, f) => onPerPageSelect(f - 1, m)}
      variant={variant}
    />
  );
};

export const PaginatingTableToolbar = ({
  count,
  totalCount,
  searchTypeComponent,
  toolbarItem,
  subToolbar,
  children,
  inputGroupName,
  inputGroupPlaceholder,
  inputGroupOnEnter,
  ...rest
}: PropsWithChildren<TableToolbarProps>) => {
  return (
    <TableToolbar
      searchTypeComponent={searchTypeComponent}
      toolbarItem={
        <>
          {toolbarItem}
          <ToolbarItem variant="pagination">
            <KeycloakPagination
              count={count}
              totalCount={totalCount}
              {...rest}
            />
          </ToolbarItem>
        </>
      }
      subToolbar={subToolbar}
      toolbarItemFooter={
        count !== 0 ? (
          <ToolbarItem variant="pagination">
            <KeycloakPagination
              count={count}
              totalCount={totalCount}
              variant="bottom"
              {...rest}
            />
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
