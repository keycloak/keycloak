import React, {
  FormEvent,
  Fragment,
  FunctionComponent,
  ReactNode,
  useState,
} from "react";
import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  InputGroup,
  TextInput,
  Button,
  ButtonVariant,
  Divider,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

type TableToolbarProps = {
  toolbarItem?: ReactNode;
  subToolbar?: ReactNode;
  toolbarItemFooter?: ReactNode;
  searchTypeComponent?: ReactNode;
  inputGroupName?: string;
  inputGroupPlaceholder?: string;
  inputGroupOnChange?: (
    newInput: string,
    event: FormEvent<HTMLInputElement>
  ) => void;
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
  inputGroupOnChange,
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

  const handleKeyDown = (e: any) => {
    if (e.key === "Enter") {
      onSearch();
    }
  };

  const handleInputChange = (
    value: string,
    event: FormEvent<HTMLInputElement>
  ) => {
    inputGroupOnChange?.(value, event);
    setSearchValue(value);
  };

  return (
    <>
      <Toolbar>
        <ToolbarContent>
          <Fragment>
            {inputGroupName && (
              <ToolbarItem>
                <InputGroup>
                  {searchTypeComponent}
                  {inputGroupPlaceholder && (
                    <>
                      <TextInput
                        name={inputGroupName}
                        id={inputGroupName}
                        type="search"
                        aria-label={t("search")}
                        placeholder={inputGroupPlaceholder}
                        onChange={handleInputChange}
                        onKeyDown={handleKeyDown}
                      />
                      <Button
                        variant={ButtonVariant.control}
                        aria-label={t("search")}
                        onClick={onSearch}
                      >
                        <SearchIcon />
                      </Button>
                    </>
                  )}
                </InputGroup>
              </ToolbarItem>
            )}
          </Fragment>
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
