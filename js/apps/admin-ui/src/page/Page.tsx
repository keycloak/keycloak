import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { PageHandler } from "./PageHandler";
import { PAGE_PROVIDER } from "./PageList";
import { PageParams } from "./routes";

export default function Page() {
  const { t } = useTranslation();
  const { componentTypes } = useServerInfo();
  const pages = componentTypes?.[PAGE_PROVIDER];
  const { id, providerId } = useParams<PageParams>();

  const page = pages?.find((p) => p.id === providerId);
  if (!page) {
    throw new Error(t("notFound"));
  }

  return (
    <>
      <ViewHeader titleKey={id || t("createItem")} />
      <PageHandler providerType={PAGE_PROVIDER} id={id} page={page} />
    </>
  );
}
