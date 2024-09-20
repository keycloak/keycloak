import {
  Divider,
  InputGroup,
  SearchInput,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { KeyboardEvent, PropsWithChildren, ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";

type TableToolbarProps = {
  toolbarItem?: ReactNode;
  subToolbar?: ReactNode;
  toolbarItemFooter?: ReactNode;
  searchTypeComponent?: ReactNode;
  inputGroupName?: string;
  inputGroupPlaceholder?: string;
  inputGroupOnEnter?: (value: string) => void;
};

export const TableToolbar = ({
  toolbarItem,
  subToolbar,
  toolbarItemFooter,
  children,
  searchTypeComponent,
  inputGroupName,
  inputGroupPlaceholder,
  inputGroupOnEnter,
}: PropsWithChildren<TableToolbarProps>) => {
  const { t } = useTranslation();
  const [searchValue, setSearchValue] = useState<string>("");

  const onSearch = () => {
    if (searchValue !== "") {
      setSearchValue(searchValue);
      inputGroupOnEnter?.(searchValue);
    } else {
      setSearchValue("");
      inputGroupOnEnter?.("");
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      onSearch();
    }
  };

  return (
    <>
      <Toolbar>
        <ToolbarContent>
          {inputGroupName && (
            <ToolbarItem>
              <InputGroup data-testid={inputGroupName}>
                {searchTypeComponent}
                {inputGroupPlaceholder && (
                  <SearchInput
                    data-testid="table-search-input"
                    placeholder={inputGroupPlaceholder}
                    aria-label={t("search")}
                    value={searchValue}
                    onChange={(_, value) => {
                      setSearchValue(value);
                    }}
                    onSearch={onSearch}
                    onKeyDown={handleKeyDown}
                    onClear={() => {
                      setSearchValue("");
                      inputGroupOnEnter?.("");
                    }}
                  />
                )}
              </InputGroup>
            </ToolbarItem>
          )}
          {toolbarItem}
        </ToolbarContent>
      </Toolbar>
      {subToolbar && (
        <Toolbar>
          <ToolbarContent>{subToolbar}</ToolbarContent>
        </Toolbar>
      )}
      <Divider />
      {children}
      <Toolbar>{toolbarItemFooter}</Toolbar>
    </>
  );
};
