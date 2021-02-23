import React, {
  FormEvent,
  Fragment,
  MouseEventHandler,
  ReactNode,
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
  filterToolbarDropdown?: ReactNode;
  toolbarItem?: ReactNode;
  toolbarItemFooter?: ReactNode;
  children: ReactNode;
  searchTypeComponent?: ReactNode;
  inputGroupName?: string;
  inputGroupPlaceholder?: string;
  inputGroupOnChange?: (
    newInput: string,
    event: FormEvent<HTMLInputElement>
  ) => void;
  inputGroupOnClick?: MouseEventHandler;
};

export const TableToolbar = ({
  filterToolbarDropdown,
  toolbarItem,
  toolbarItemFooter,
  children,
  searchTypeComponent,
  inputGroupName,
  inputGroupPlaceholder,
  inputGroupOnChange,
  inputGroupOnClick,
}: TableToolbarProps) => {
  const { t } = useTranslation();
  return (
    <>
      <Toolbar>
        <ToolbarContent>
          <Fragment>
            {inputGroupName && (
              <ToolbarItem>
                <InputGroup>
                  {filterToolbarDropdown}
                  {searchTypeComponent}
                  <TextInput
                    name={inputGroupName}
                    id={inputGroupName}
                    type="search"
                    aria-label={t("search")}
                    placeholder={inputGroupPlaceholder}
                    onChange={inputGroupOnChange}
                  />
                  <Button
                    variant={ButtonVariant.control}
                    aria-label={t("search")}
                    onClick={inputGroupOnClick}
                  >
                    <SearchIcon />
                  </Button>
                </InputGroup>
              </ToolbarItem>
            )}
          </Fragment>
          {toolbarItem}
        </ToolbarContent>
      </Toolbar>
      <Divider />
      {children}
      <Toolbar>{toolbarItemFooter}</Toolbar>
    </>
  );
};
