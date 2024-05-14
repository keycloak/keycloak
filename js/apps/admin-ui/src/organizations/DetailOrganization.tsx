import { PageSection, Tab, TabTitleText } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { useRealm } from "../context/realm-context/RealmContext";
import { useFetch } from "../utils/useFetch";
import { useParams } from "../utils/useParams";
import { DetailOrganizationHeader } from "./DetailOraganzationHeader";
import { OrganizationForm } from "./OrganizationForm";
import {
  EditOrganizationParams,
  OrganizationTab,
  toEditOrganization,
} from "./routes/EditOrganization";
import { AttributesForm } from "../components/key-value-form/AttributeForm";
import { Members } from "./Members";

export default function DetailOrganization() {
  // const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { id } = useParams<EditOrganizationParams>();
  const { t } = useTranslation();

  const form = useForm();

  const save = () => {
    console.log("save");
  };

  useFetch(
    () => Promise.resolve({ id: "12", name: "redhat", enabled: false }), //adminClient.organisations.findOne({ id }),
    (org) => {
      if (!org) {
        throw new Error(t("notFound"));
      }
      form.reset({ ...org });
    },
    [id],
  );

  const useTab = (tab: OrganizationTab) =>
    useRoutableTab(
      toEditOrganization({
        realm,
        id,
        tab,
      }),
    );

  const settingsTab = useTab("settings");
  const attributesTab = useTab("attributes");
  const membersTab = useTab("members");

  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
      <FormProvider {...form}>
        <DetailOrganizationHeader save={save} />
        <RoutableTabs
          data-testid="organization-tabs"
          aria-label={t("organization")}
          isBox
          mountOnEnter
        >
          <Tab
            id="settings"
            data-testid="settingsTab"
            title={<TabTitleText>{t("settings")}</TabTitleText>}
            {...settingsTab}
          >
            <PageSection>
              <OrganizationForm save={save} />
            </PageSection>
          </Tab>
          <Tab
            id="attributes"
            data-testid="attributeTab"
            title={<TabTitleText>{t("attributes")}</TabTitleText>}
            {...attributesTab}
          >
            <PageSection variant="light">
              <AttributesForm
                form={form}
                save={save}
                reset={() =>
                  form.reset({
                    ...form.getValues(),
                  })
                }
                name="attributes"
              />
            </PageSection>
          </Tab>
          <Tab
            id="members"
            data-testid="membersTab"
            title={<TabTitleText>{t("members")}</TabTitleText>}
            {...membersTab}
          >
            <PageSection variant="light">
              <Members />
            </PageSection>
          </Tab>
        </RoutableTabs>
      </FormProvider>
    </PageSection>
  );
}
