import React from "react";
import {
  ToggleTemplateProps,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  InputGroup,
  TextInput,
  Button,
  ButtonVariant,
  Pagination,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

type TableToolbarProps = {
  count: number;
  first: number;
  max: number;
  onNextClick: (page: number) => void;
  onPreviousClick: (page: number) => void;
  onPerPageSelect: (max: number, first: number) => void;
  toolbarItem?: React.ReactNode;
  children: React.ReactNode;
  inputGroupName?: string;
  inputGroupPlaceholder?: string;
  inputGroupOnChange?: (
    newInput: string,
    event: React.FormEvent<HTMLInputElement>
  ) => void;
};

export const TableToolbar = ({
  count,
  first,
  max,
  onNextClick,
  onPreviousClick,
  onPerPageSelect,
  toolbarItem,
  children,
  inputGroupName,
  inputGroupPlaceholder,
  inputGroupOnChange,
}: TableToolbarProps) => {
  const { t } = useTranslation("groups");
  const page = first / max;
  const pagination = (variant: "top" | "bottom" = "top") => (
    <Pagination
      isCompact
      toggleTemplate={({ firstIndex, lastIndex }: ToggleTemplateProps) => (
        <b>
          {firstIndex} - {lastIndex}
        </b>
      )}
      itemCount={count + page * max + (count <= max ? 1 : 0)}
      page={page + 1}
      perPage={max}
      onNextClick={(_, p) => onNextClick((p - 1) * max)}
      onPreviousClick={(_, p) => onPreviousClick((p - 1) * max)}
      onPerPageSelect={(_, m, f) => onPerPageSelect(f, m)}
      variant={variant}
    />
  );

  return (
    <>
      <Toolbar>
        <ToolbarContent>
          <React.Fragment>
            {inputGroupName && (
              <ToolbarItem>
                <InputGroup>
                  <TextInput
                    name={inputGroupName}
                    id={inputGroupName}
                    type="search"
                    aria-label={t("Search")}
                    placeholder={inputGroupPlaceholder}
                    onChange={inputGroupOnChange}
                  />
                  <Button
                    variant={ButtonVariant.control}
                    aria-label={t("Search")}
                  >
                    <SearchIcon />
                  </Button>
                </InputGroup>
              </ToolbarItem>
            )}
          </React.Fragment>
          {toolbarItem}
          <ToolbarItem variant="pagination">{pagination()}</ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      {children}
      <Toolbar>
        <ToolbarItem>{pagination("bottom")}</ToolbarItem>
      </Toolbar>
    </>
  );
};
