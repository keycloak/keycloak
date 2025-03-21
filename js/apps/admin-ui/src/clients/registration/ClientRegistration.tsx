import { Tab, TabTitleText } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import {
  ClientRegistrationTab,
  toClientRegistration,
} from "../routes/ClientRegistration";
import { ClientRegistrationList } from "./ClientRegistrationList";

export const ClientRegistration = () => {
  const { t } = useTranslation();
  const { realm } = useRealm();

  const useTab = (subTab: ClientRegistrationTab) =>
    useRoutableTab(toClientRegistration({ realm, subTab }));

  const anonymousTab = useTab("anonymous");
  const authenticatedTab = useTab("authenticated");

  return (
    <RoutableTabs
      defaultLocation={toClientRegistration({ realm, subTab: "anonymous" })}
      mountOnEnter
      unmountOnExit
    >
      <Tab
        data-testid="anonymous"
        title={
          <TabTitleText>
            {t("anonymousAccessPolicies")}{" "}
            <HelpItem
              fieldLabelId=""
              helpText={t("anonymousAccessPoliciesHelp")}
              noVerticalAlign={false}
              unWrap
            />
          </TabTitleText>
        }
        {...anonymousTab}
      >
        <ClientRegistrationList subType="anonymous" />
      </Tab>
      <Tab
        data-testid="authenticated"
        title={
          <TabTitleText>
            {t("authenticatedAccessPolicies")}{" "}
            <HelpItem
              fieldLabelId=""
              helpText={t("authenticatedAccessPoliciesHelp")}
              noVerticalAlign={false}
              unWrap
            />
          </TabTitleText>
        }
        {...authenticatedTab}
      >
        <ClientRegistrationList subType="authenticated" />
      </Tab>
    </RoutableTabs>
  );
};
