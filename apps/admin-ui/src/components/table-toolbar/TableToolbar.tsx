import { FunctionComponent, ReactNode, useState, KeyboardEvent } from "react";
import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  InputGroup,
  Divider,
  SearchInput,
} from "@patternfly/react-core";
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

export const TableToolbar: FunctionComponent<TableToolbarProps> = ({
  toolbarItem,
  subToolbar,
  toolbarItemFooter,
  children,
  searchTypeComponent,
  inputGroupName,
  inputGroupPlaceholder,
  inputGroupOnEnter,
}) => {
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
                    placeholder={inputGroupPlaceholder}
                    aria-label={t("search")}
                    value={searchValue}
                    onChange={setSearchValue}
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
