import { useTranslation } from "react-i18next";
import { capitalize } from "lodash-es";
import {
  Label,
  LabelGroup,
  Popover,
  Content,
  ContentVariants,
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
            <Content>
              <Content
                component="p"
                className="pf-v6-u-font-size-md pf-v6-u-font-weight-bold"
              >
                {t("authorizationScopeDetailsTitle")}
              </Content>
              <Content component="p" className="pf-v6-u-font-size-sm">
                {t("authorizationScopeDetailsSubtitle")}
              </Content>
              <Content
                component={ContentVariants.dl}
                className="pf-v6-u-font-size-sm"
              >
                <Content component={ContentVariants.dt}>
                  {t("authorizationScopeDetailsName")}
                </Content>
                <Content component={ContentVariants.dd}>
                  {capitalize(scope.name)}
                </Content>
                <Content component={ContentVariants.dt}>
                  {t("authorizationScopeDetailsDescription")}
                </Content>
                <Content component={ContentVariants.dd}>
                  {" "}
                  {t(`authorizationScope.${row.resourceType}.${scope.name}`)}
                </Content>
              </Content>
            </Content>
          }
        >
          <Label color="blue">{capitalize(scope.name)}</Label>
        </Popover>
      ))}
    </LabelGroup>
  );
};
