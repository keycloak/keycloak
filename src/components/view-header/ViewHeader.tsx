import React, { ReactElement, useContext, useState } from "react";
import {
  Text,
  PageSection,
  TextContent,
  Divider,
  Level,
  LevelItem,
  Switch,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Badge,
  Select,
} from "@patternfly/react-core";
import { HelpContext } from "../help-enabler/HelpHeader";
import { useTranslation } from "react-i18next";

export type ViewHeaderProps = {
  titleKey: string;
  badge?: string;
  subKey: string;
  selectItems?: ReactElement[];
  isEnabled?: boolean;
  onSelect?: (value: string) => void;
  onToggle?: (value: boolean) => void;
};

export const ViewHeader = ({
  titleKey,
  badge,
  subKey,
  selectItems,
  isEnabled,
  onSelect,
  onToggle,
}: ViewHeaderProps) => {
  const { t } = useTranslation();
  const { enabled } = useContext(HelpContext);
  const [open, setOpen] = useState(false);
  const [checked, setChecked] = useState(isEnabled);
  return (
    <>
      <PageSection variant="light">
        <Level hasGutter>
          <LevelItem>
            <Level>
              <LevelItem>
                <TextContent className="pf-u-mr-sm">
                  <Text component="h1">{t(titleKey)}</Text>
                </TextContent>
              </LevelItem>
              {badge && (
                <LevelItem>
                  <Badge>{badge}</Badge>
                </LevelItem>
              )}
            </Level>
          </LevelItem>
          <LevelItem></LevelItem>
          {selectItems && (
            <LevelItem>
              <Toolbar>
                <ToolbarContent>
                  <ToolbarItem>
                    <Switch
                      id={`${titleKey}-switch`}
                      label={t("common:enabled")}
                      labelOff={t("common:disabled")}
                      className="pf-u-mr-lg"
                      isChecked={checked}
                      onChange={(value) => {
                        if (onToggle) {
                          onToggle(value);
                        }
                        setChecked(value);
                      }}
                    />
                  </ToolbarItem>
                  <ToolbarItem>
                    <Select
                      isOpen={open}
                      onToggle={() => setOpen(!open)}
                      onSelect={(_, value) => {
                        if (onSelect) {
                          onSelect(value as string);
                        }
                        setOpen(false);
                      }}
                    >
                      {selectItems}
                    </Select>
                  </ToolbarItem>
                </ToolbarContent>
              </Toolbar>
            </LevelItem>
          )}
        </Level>
        {enabled && (
          <TextContent>
            <Text>{t(subKey)}</Text>
          </TextContent>
        )}
      </PageSection>
      <Divider />
    </>
  );
};
