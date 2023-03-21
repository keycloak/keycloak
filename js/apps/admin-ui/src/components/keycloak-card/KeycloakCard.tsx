import {
  Card,
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
import { Link, To } from "react-router-dom";

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
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  const onDropdownToggle = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  return (
    <Card isSelectable>
      <CardHeader>
        <CardActions>
          {dropdownItems && (
            <Dropdown
              data-testid={`${title}-dropdown`}
              isPlain
              position={"right"}
              toggle={<KebabToggle onToggle={onDropdownToggle} />}
              isOpen={isDropdownOpen}
              dropdownItems={dropdownItems}
            />
          )}
        </CardActions>
        <CardTitle data-testid="keycloak-card-title">
          <Link to={to}>{title}</Link>
        </CardTitle>
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
    </Card>
  );
};
