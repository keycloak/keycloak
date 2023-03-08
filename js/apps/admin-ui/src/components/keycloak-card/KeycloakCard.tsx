import {
  CardActions,
  CardBody,
  CardFooter,
  CardHeader,
  CardTitle,
  Dropdown,
  Flex,
  FlexItem,
  KebabToggle,
  Label,
} from "@patternfly/react-core";
import { ReactElement, useState } from "react";
import { To, useNavigate } from "react-router-dom";
import { ClickableCard } from "./ClickableCard";

import "./keycloak-card.css";

export type KeycloakCardProps = {
  title: string;
  dropdownItems?: ReactElement[];
  labelText?: string;
  labelColor?: any;
  footerText?: string;
  to: To;
};

export const KeycloakCard = ({
  title,
  dropdownItems,
  labelText,
  labelColor,
  footerText,
  to,
}: KeycloakCardProps) => {
  const navigate = useNavigate();
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  const onDropdownToggle = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  const handleCardMenuClick = (e: any) => {
    e.stopPropagation();
  };

  return (
    <ClickableCard isSelectable onClick={() => navigate(to)}>
      <CardHeader>
        <CardActions>
          {dropdownItems && (
            <Dropdown
              data-testid={`${title}-dropdown`}
              isPlain
              position={"right"}
              toggle={<KebabToggle onToggle={onDropdownToggle} />}
              onClick={(e) => handleCardMenuClick(e)}
              isOpen={isDropdownOpen}
              dropdownItems={dropdownItems}
            />
          )}
        </CardActions>
        <CardTitle data-testid="keycloak-card-title">{title}</CardTitle>
      </CardHeader>
      <CardBody />
      <CardFooter>
        <Flex>
          <FlexItem className="keycloak--keycloak-card__footer">
            {footerText && footerText}
          </FlexItem>
          <FlexItem>
            {labelText && (
              <Label color={labelColor || "gray"}>{labelText}</Label>
            )}
          </FlexItem>
        </Flex>
      </CardFooter>
    </ClickableCard>
  );
};
