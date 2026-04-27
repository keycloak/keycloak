import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Pagination,
  SearchInput,
  PaginationToggleTemplateProps,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";

type LinkedAccountsToolbarProps = {
  onFilter: (nameFilter: string) => void;
  count: number;
  first: number;
  max: number;
  onNextClick: (page: number) => void;
  onPreviousClick: (page: number) => void;
  onPerPageSelect: (max: number, first: number) => void;
  hasNext: boolean;
};

export const LinkedAccountsToolbar = ({
  count,
  first,
  max,
  onNextClick,
  onPreviousClick,
  onPerPageSelect,
  onFilter,
  hasNext,
}: LinkedAccountsToolbarProps) => {
  const { t } = useTranslation();
  const [nameFilter, setNameFilter] = useState("");

  const page = Math.round(first / max) + 1;
  return (
    <Toolbar>
      <ToolbarContent>
        <ToolbarItem>
          <SearchInput
            placeholder={t("filterByName")}
            aria-label={t("filterByName")}
            value={nameFilter}
            onChange={(_, value) => {
              setNameFilter(value);
            }}
            onSearch={() => onFilter(nameFilter)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                onFilter(nameFilter);
              }
            }}
            onClear={() => {
              setNameFilter("");
              onFilter("");
            }}
          />
        </ToolbarItem>
        <ToolbarItem variant="pagination">
          <Pagination
            isCompact
            perPageOptions={[
              { title: "5", value: 6 },
              { title: "10", value: 11 },
              { title: "20", value: 21 },
            ]}
            toggleTemplate={({
              firstIndex,
              lastIndex,
            }: PaginationToggleTemplateProps) => (
              <b>
                {firstIndex && firstIndex > 1 ? firstIndex - 1 : firstIndex} -{" "}
                {lastIndex && lastIndex > 1 ? lastIndex - 1 : lastIndex}
              </b>
            )}
            itemCount={count + (page - 1) * max + (hasNext ? 1 : 0)}
            page={page}
            perPage={max}
            onNextClick={(_, p) => onNextClick((p - 1) * max)}
            onPreviousClick={(_, p) => onPreviousClick((p - 1) * max)}
            onPerPageSelect={(_, m, f) => onPerPageSelect(f - 1, m)}
          />
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );
};
