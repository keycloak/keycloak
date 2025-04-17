import {
  Badge,
  Button,
  Divider,
  Dropdown,
  DropdownList,
  Level,
  LevelItem,
  MenuToggle,
  PageSection,
  Switch,
  Content,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  ReactElement,
  ReactNode,
  useState,
  isValidElement,
  Fragment,
} from "react";
import { useTranslation } from "react-i18next";
import { FormattedLink } from "../external-link/FormattedLink";
import { useHelp, HelpItem } from "@keycloak/keycloak-ui-shared";
import "../../help-urls";

export type ViewHeaderProps = {
  titleKey: string;
  className?: string;
  badges?: ViewHeaderBadge[];
  isDropdownDisabled?: boolean;
  subKey?: string | ReactNode;
  actionsDropdownId?: string;
  helpUrl?: string | undefined;
  dropdownItems?: ReactElement[];
  lowerDropdownItems?: any;
  lowerDropdownMenuTitle?: any;
  lowerButton?: any;
  isEnabled?: boolean;
  onToggle?: (value: boolean) => void;
  divider?: boolean;
  helpTextKey?: string;
  isReadOnly?: boolean;
};

export type ViewHeaderBadge = {
  id?: string;
  text?: string | ReactNode;
  readonly?: boolean;
};

export const ViewHeader = ({
  actionsDropdownId,
  className,
  titleKey,
  badges,
  isDropdownDisabled,
  subKey,
  helpUrl,
  dropdownItems,
  lowerDropdownMenuTitle,
  lowerDropdownItems,
  lowerButton,
  isEnabled = true,
  onToggle,
  divider = true,
  helpTextKey,
  isReadOnly = false,
}: ViewHeaderProps) => {
  const { t, i18n } = useTranslation();
  const { enabled } = useHelp();
  const [isDropdownOpen, setDropdownOpen] = useState(false);
  const [isLowerDropdownOpen, setIsLowerDropdownOpen] = useState(false);

  const onDropdownToggle = () => {
    setDropdownOpen(!isDropdownOpen);
  };

  const onLowerDropdownToggle = () => {
    setIsLowerDropdownOpen(!isLowerDropdownOpen);
  };

  const toKey = (value: string) => value.replace(/\s/g, "-");

  return (
    <>
      <PageSection hasBodyWrapper={false}>
        <Level hasGutter>
          <LevelItem>
            <Level>
              <LevelItem>
                <Content className="pf-v5-u-mr-sm">
                  <Content
                    className={className}
                    component="h1"
                    data-testid="view-header"
                  >
                    {i18n.exists(titleKey) ? t(titleKey) : titleKey}
                  </Content>
                </Content>
              </LevelItem>
              {badges && (
                <LevelItem>
                  {badges.map((badge, index) => (
                    <Fragment key={index}>
                      {!isValidElement(badge.text) && (
                        <Fragment key={badge.text as string}>
                          <Badge data-testid={badge.id} isRead={badge.readonly}>
                            {badge.text}
                          </Badge>{" "}
                        </Fragment>
                      )}
                      {isValidElement(badge.text) && badge.text}{" "}
                    </Fragment>
                  ))}
                </LevelItem>
              )}
            </Level>
          </LevelItem>
          <LevelItem>
            <Toolbar className="pf-v5-u-p-0">
              <ToolbarContent>
                {onToggle && (
                  <ToolbarItem alignSelf="center">
                    <Switch
                      id={`${toKey(titleKey)}-switch`}
                      data-testid={`${titleKey}-switch`}
                      label={t("enabled")}
                      className="pf-v5-u-mr-lg"
                      isDisabled={isReadOnly}
                      isChecked={isEnabled}
                      aria-label={t("enabled")}
                      onChange={(_event, value) => {
                        onToggle(value);
                      }}
                    />
                    {helpTextKey && (
                      <HelpItem
                        helpText={t(helpTextKey)}
                        fieldLabelId={`${toKey(titleKey)}-switch`}
                      />
                    )}
                  </ToolbarItem>
                )}
                {dropdownItems && (
                  <ToolbarItem>
                    <Dropdown
                      popperProps={{
                        position: "right",
                      }}
                      onOpenChange={onDropdownToggle}
                      toggle={(ref) => (
                        <MenuToggle
                          ref={ref}
                          isDisabled={isDropdownDisabled}
                          id={actionsDropdownId}
                          onClick={onDropdownToggle}
                          data-testid="action-dropdown"
                        >
                          {t("action")}
                        </MenuToggle>
                      )}
                      isOpen={isDropdownOpen}
                    >
                      <DropdownList>{dropdownItems}</DropdownList>
                    </Dropdown>
                  </ToolbarItem>
                )}
              </ToolbarContent>
            </Toolbar>
          </LevelItem>
        </Level>
        {enabled && (
          <Content id="view-header-subkey">
            <Content component="p">
              {isValidElement(subKey)
                ? subKey
                : subKey
                  ? t(subKey as string)
                  : ""}
              {helpUrl && (
                <FormattedLink
                  title={t("learnMore")}
                  href={helpUrl}
                  isInline
                  className="pf-v5-u-ml-md"
                />
              )}
            </Content>
          </Content>
        )}
        {lowerDropdownItems && (
          <Dropdown
            className="keycloak__user-federation__dropdown"
            onOpenChange={onLowerDropdownToggle}
            toggle={(ref) => (
              <MenuToggle
                ref={ref}
                onClick={onLowerDropdownToggle}
                variant="primary"
                id="ufToggleId"
              >
                {t(lowerDropdownMenuTitle)}
              </MenuToggle>
            )}
            isOpen={isLowerDropdownOpen}
          >
            <DropdownList>{lowerDropdownItems}</DropdownList>
          </Dropdown>
        )}
        {lowerButton && (
          <Button
            variant={lowerButton.variant}
            onClick={lowerButton.onClick}
            data-testid="viewHeader-lower-btn"
          >
            {lowerButton.lowerButtonTitle}
          </Button>
        )}
      </PageSection>
      {divider && <Divider component="div" />}
    </>
  );
};
