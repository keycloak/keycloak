import React, { MouseEventHandler, ReactNode } from "react";
import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  InputGroup,
  TextInput,
  Button,
  ButtonVariant,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

type TableToolbarProps = {
  toolbarItem?: ReactNode;
  toolbarItemFooter?: ReactNode;
  children: React.ReactNode;
  inputGroupName?: string;
  inputGroupPlaceholder?: string;
  inputGroupOnChange?: (
    newInput: string,
    event: React.FormEvent<HTMLInputElement>
  ) => void;
  inputGroupOnClick?: MouseEventHandler;
};

export const TableToolbar = ({
  toolbarItem,
  toolbarItemFooter,
  children,
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
          <React.Fragment>
            {inputGroupName && (
              <ToolbarItem>
                <InputGroup>
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
          </React.Fragment>
          {toolbarItem}
        </ToolbarContent>
      </Toolbar>
      {children}
      <Toolbar>{toolbarItemFooter}</Toolbar>
    </>
  );
};
