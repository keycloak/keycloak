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
import { FunctionComponent, useState } from "react";
import { useTranslation } from "react-i18next";
import useLocalStorage from "react-use-localstorage";

import { createNamedContext } from "../../utils/createNamedContext";
import useRequiredContext from "../../utils/useRequiredContext";
import helpUrls from "../../help-urls";

import "./help-header.css";

type HelpContextProps = {
  enabled: boolean;
  toggleHelp: () => void;
};

export const HelpContext = createNamedContext<HelpContextProps | undefined>(
  "HelpContext",
  undefined
);

export const useHelp = () => useRequiredContext(HelpContext);

export const Help: FunctionComponent = ({ children }) => {
  const [enabled, setHelp] = useLocalStorage("helpEnabled", "true");

  function toggleHelp() {
    setHelp(enabled === "true" ? "false" : "true");
  }
  return (
    <HelpContext.Provider value={{ enabled: enabled === "true", toggleHelp }}>
      {children}
    </HelpContext.Provider>
  );
};

export const HelpHeader = () => {
  const [open, setOpen] = useState(false);
  const help = useHelp();
  const { t } = useTranslation();

  const dropdownItems = [
    <DropdownItem
      key="link"
      id="link"
      href={helpUrls.documentationUrl}
      target="_blank"
    >
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
            aria-label={t("common:enableHelp")}
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
