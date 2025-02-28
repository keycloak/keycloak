import { useTranslation } from "react-i18next";
import { capitalize } from "lodash-es";
import { Label, LabelGroup, Popover } from "@patternfly/react-core";

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
            <>
              <p style={{ fontSize: "16px" }}>
                <strong>{t("authorizationScopeDetailsTitle")}</strong>
              </p>
              <br />
              <p>{t("authorizationScopeDetailsSubtitle")}</p>
              <br />
              <table>
                <tbody>
                  <tr>
                    <td style={{ paddingRight: "15px", paddingBottom: "10px" }}>
                      <strong>{t("authorizationScopeDetailsName")}</strong>
                    </td>
                    <td style={{ paddingBottom: "10px" }}>
                      {capitalize(scope.name)}
                    </td>
                  </tr>
                  <tr>
                    <td style={{ paddingRight: "15px", paddingBottom: "10px" }}>
                      <strong>
                        {t("authorizationScopeDetailsDescription")}
                      </strong>
                    </td>
                    <td style={{ paddingBottom: "10px" }}>
                      {t(
                        `authorizationScope.${row.resourceType}.${scope.name}`,
                      )}
                    </td>
                  </tr>
                </tbody>
              </table>
            </>
          }
        >
          <Label color="blue">{capitalize(scope.name)}</Label>
        </Popover>
      ))}
    </LabelGroup>
  );
};
