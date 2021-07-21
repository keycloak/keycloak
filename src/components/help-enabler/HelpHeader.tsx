import {
  Divider,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Split,
  SplitItem,
  Switch,
  TextContent,
} from "@patternfly/react-core";
import { ExternalLinkAltIcon, HelpIcon } from "@patternfly/react-icons";
import React, { createContext, ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";
import useRequiredContext from "../../utils/useRequiredContext";
import "./help-header.css";

type HelpProps = {
  children: ReactNode;
};

type HelpContextProps = {
  enabled: boolean;
  toggleHelp: () => void;
};

export const HelpContext = createContext<HelpContextProps | undefined>(
  undefined
);

export const useHelp = () => useRequiredContext(HelpContext);

export const Help = ({ children }: HelpProps) => {
  const [enabled, setHelp] = useState(true);

  function toggleHelp() {
    setHelp((help) => !help);
  }
  return (
    <HelpContext.Provider value={{ enabled, toggleHelp }}>
      {children}
    </HelpContext.Provider>
  );
};

export const HelpHeader = () => {
  const [open, setOpen] = useState(false);
  const help = useHelp();
  const { t } = useTranslation();

  const dropdownItems = [
    <DropdownItem key="link" id="link">
      <Split>
        <SplitItem isFilled>{t("documentation")}</SplitItem>
        <SplitItem>
          <ExternalLinkAltIcon />
        </SplitItem>
      </Split>
    </DropdownItem>,
    <Divider key="divide" />,
    <DropdownItem key="enable" id="enable">
      <Split>
        <SplitItem isFilled>{t("enableHelpMode")}</SplitItem>
        <SplitItem>
          <Switch
            id="enableHelp"
            aria-label="Help is enabled"
            isChecked={help.enabled}
            label=""
            className="keycloak_help-header-switch"
            onChange={() => help.toggleHelp()}
          />
        </SplitItem>
      </Split>
      <TextContent className="keycloak_help-header-description">
        {t("common-help:helpToggleInfo")}
      </TextContent>
    </DropdownItem>,
  ];
  return (
    <Dropdown
      position="right"
      isPlain
      isOpen={open}
      toggle={
        <DropdownToggle
          toggleIndicator={null}
          onToggle={() => setOpen(!open)}
          aria-label="Help"
          id="help"
        >
          <HelpIcon />
        </DropdownToggle>
      }
      dropdownItems={dropdownItems}
    />
  );
};
