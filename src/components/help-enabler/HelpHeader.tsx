import React, { useState, useContext, ReactNode, createContext } from "react";
import {
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Split,
  SplitItem,
  Switch,
  Text,
  TextVariants,
} from "@patternfly/react-core";
import { Trans, useTranslation } from "react-i18next";
import { HelpIcon, ExternalLinkAltIcon } from "@patternfly/react-icons";

type HelpProps = {
  children: ReactNode;
};

type HelpContextProps = {
  enabled: boolean;
  toggleHelp: () => void;
};

export const HelpContext = createContext<HelpContextProps>({
  enabled: true,
  toggleHelp: () => {},
});

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
  const help = useContext(HelpContext);
  const { t } = useTranslation();

  const dropdownItems = [
    <DropdownItem key="link" id="link">
      <Split>
        <SplitItem isFilled>{t("Documentation")}</SplitItem>
        <SplitItem>
          <ExternalLinkAltIcon />
        </SplitItem>
      </Split>
    </DropdownItem>,
    <DropdownItem
      key="enable"
      id="enable"
      description={
        <Trans>
          This toggle will enable / disable part of the help info in the
          console. Includes any help text, links and popovers.
        </Trans>
      }
    >
      <Split>
        <SplitItem isFilled>{t("Enable help mode")}</SplitItem>
        <SplitItem>
          <Switch
            id="enableHelp"
            aria-label="Help is enabled"
            isChecked={help.enabled}
            onChange={() => help.toggleHelp()}
          />
        </SplitItem>
      </Split>
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
