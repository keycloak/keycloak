import { useTranslation } from "react-i18next";
import { capitalize } from "lodash-es";
import {
  Label,
  LabelGroup,
  Popover,
  Text,
  TextContent,
  TextList,
  TextListItem,
  TextListItemVariants,
  TextListVariants,
} from "@patternfly/react-core";

type AuthorizationScopesDetailsProps = {
  row: {
    resourceType: string;
    associatedScopes?: { name: string }[];
  };
};

export const AuthorizationScopesDetails = ({
  row,
}: AuthorizationScopesDetailsProps) => {
  const { t } = useTranslation();

  const associatedScopes = row.associatedScopes || [];

  return (
    <LabelGroup>
      {associatedScopes.map((scope, index) => (
        <Popover
          key={index}
          aria-label={`Authorization scope popover for ${scope.name}`}
          position="top"
          hasAutoWidth
          bodyContent={
            <TextContent>
              <Text className="pf-v5-u-font-size-md pf-v5-u-font-weight-bold">
                {t("authorizationScopeDetailsTitle")}
              </Text>
              <Text className="pf-v5-u-font-size-sm">
                {t("authorizationScopeDetailsSubtitle")}
              </Text>
              <TextList
                component={TextListVariants.dl}
                className="pf-v5-u-font-size-sm"
              >
                <TextListItem component={TextListItemVariants.dt}>
                  {t("authorizationScopeDetailsName")}
                </TextListItem>
                <TextListItem component={TextListItemVariants.dd}>
                  {capitalize(scope.name)}
                </TextListItem>
                <TextListItem component={TextListItemVariants.dt}>
                  {t("authorizationScopeDetailsDescription")}
                </TextListItem>
                <TextListItem component={TextListItemVariants.dd}>
                  {" "}
                  {t(`authorizationScope.${row.resourceType}.${scope.name}`)}
                </TextListItem>
              </TextList>
            </TextContent>
          }
        >
          <Label color="blue">{capitalize(scope.name)}</Label>
        </Popover>
      ))}
    </LabelGroup>
  );
};
