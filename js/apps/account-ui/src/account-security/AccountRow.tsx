import {
  Button,
  DataListAction,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Icon,
  Label,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { LinkIcon, UnlinkIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import {
  IconMapper,
  useAlerts,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import { linkAccount, unLinkAccount } from "../api/methods";
import { LinkedAccountRepresentation } from "../api/representations";

type AccountRowProps = {
  account: LinkedAccountRepresentation;
  isLinked?: boolean;
  refresh: () => void;
};

export const AccountRow = ({
  account,
  isLinked = false,
  refresh,
}: AccountRowProps) => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAlerts();

  const unLink = async (account: LinkedAccountRepresentation) => {
    try {
      await unLinkAccount(context, account);
      addAlert(t("unLinkSuccess"));
      refresh();
    } catch (error) {
      addError(t("unLinkError", { error }).toString());
    }
  };

  const link = async (account: LinkedAccountRepresentation) => {
    try {
      const { accountLinkUri } = await linkAccount(context, account);
      location.href = accountLinkUri;
    } catch (error) {
      addError(t("linkError", { error }).toString());
    }
  };

  return (
    <DataListItem
      id={`${account.providerAlias}-idp`}
      key={account.providerName}
      aria-label={t("linkedAccounts")}
    >
      <DataListItemRow
        key={account.providerName}
        data-testid={`linked-accounts/${account.providerName}`}
      >
        <DataListItemCells
          dataListCells={[
            <DataListCell key="idp">
              <Split>
                <SplitItem className="pf-v5-u-mr-sm">
                  <IconMapper icon={account.providerName} />
                </SplitItem>
                <SplitItem className="pf-v5-u-my-xs" isFilled>
                  <span id={`${account.providerAlias}-idp-name`}>
                    {account.displayName}
                  </span>
                </SplitItem>
              </Split>
            </DataListCell>,
            <DataListCell key="label">
              <Split>
                <SplitItem className="pf-v5-u-my-xs" isFilled>
                  <span id={`${account.providerAlias}-idp-label`}>
                    <Label color={account.social ? "blue" : "green"}>
                      {t(account.social ? "socialLogin" : "systemDefined")}
                    </Label>
                  </span>
                </SplitItem>
              </Split>
            </DataListCell>,
            <DataListCell key="username" width={5}>
              <Split>
                <SplitItem className="pf-v5-u-my-xs" isFilled>
                  <span id={`${account.providerAlias}-idp-username`}>
                    {account.linkedUsername}
                  </span>
                </SplitItem>
              </Split>
            </DataListCell>,
          ]}
        />
        <DataListAction
          aria-labelledby={t("link")}
          aria-label={t("unLink")}
          id="setPasswordAction"
        >
          {isLinked && (
            <Button
              id={`${account.providerAlias}-idp-unlink`}
              variant="link"
              onClick={() => unLink(account)}
            >
              <Icon size="sm">
                <UnlinkIcon />
              </Icon>{" "}
              {t("unLink")}
            </Button>
          )}
          {!isLinked && (
            <Button
              id={`${account.providerAlias}-idp-link`}
              variant="link"
              onClick={() => link(account)}
            >
              <Icon size="sm">
                <LinkIcon />
              </Icon>{" "}
              {t("link")}
            </Button>
          )}
        </DataListAction>
      </DataListItemRow>
    </DataListItem>
  );
};
