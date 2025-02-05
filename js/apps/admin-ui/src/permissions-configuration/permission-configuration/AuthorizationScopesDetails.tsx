import { useTranslation } from "react-i18next";
import { MoreLabel } from "../../clients/authorization/MoreLabel";
import { capitalize } from "lodash-es";
import { Label, Popover } from "@patternfly/react-core";

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

  return (
    <>
      <Popover
        aria-label="Authorization scopes popover"
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
                    {capitalize(row.associatedScopes?.[0]?.name)}
                  </td>
                </tr>
                <tr>
                  <td style={{ paddingRight: "15px", paddingBottom: "10px" }}>
                    <strong>{t("authorizationScopeDetailsDescription")}</strong>
                  </td>
                  <td style={{ paddingBottom: "10px" }}>
                    {t(
                      `authorizationScope.${row.resourceType}.${row.associatedScopes?.[0]?.name}`,
                    )}
                  </td>
                </tr>
              </tbody>
            </table>
          </>
        }
      >
        <Label color="blue">
          {capitalize(row.associatedScopes?.[0]?.name)}{" "}
        </Label>
      </Popover>
      <MoreLabel array={row.associatedScopes} />
    </>
  );
};
