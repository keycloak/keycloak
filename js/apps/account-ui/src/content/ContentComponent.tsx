import { Spinner } from "@patternfly/react-core";
import { Suspense, lazy, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { environment } from "../environment";
import { ContentComponentParams } from "../routes";
import { joinPath } from "../utils/joinPath";
import { usePromise } from "../utils/usePromise";
import fetchContentJson from "./fetchContent";
import { MenuItem } from "../root/PageNav";

function findComponent(
  content: MenuItem[],
  componentId: string,
): string | undefined {
  for (const item of content) {
    if (
      "path" in item &&
      item.path.endsWith(componentId) &&
      "modulePath" in item
    ) {
      return item.modulePath;
    }
    if ("children" in item) {
      return findComponent(item.children, componentId);
    }
  }
  return undefined;
}

const ContentComponent = () => {
  const [content, setContent] = useState<MenuItem[]>();
  const { componentId } = useParams<ContentComponentParams>();

  usePromise((signal) => fetchContentJson({ signal }), setContent);
  const modulePath = useMemo(
    () => findComponent(content || [], componentId!),
    [content, componentId],
  );

  return modulePath && <Component modulePath={modulePath} />;
};

type ComponentProps = {
  modulePath: string;
};

const Component = ({ modulePath }: ComponentProps) => {
  const Element = lazy(
    () => import(joinPath(environment.resourceUrl, modulePath)),
  );
  return (
    <Suspense fallback={<Spinner />}>
      <Element />
    </Suspense>
  );
};

export default ContentComponent;
