import { Spinner } from "@patternfly/react-core";
import { Suspense, lazy, useState } from "react";
import { useParams } from "react-router-dom";
import { environment } from "../environment";
import { joinPath } from "../utils/joinPath";
import { usePromise } from "../utils/usePromise";
import { fetchContentJson } from "./ContentRenderer";
import { ContentItem, ModulePageDef, isExpansion } from "./content";
import { ContentComponentParams } from "../routes";

function findComponent(content: ContentItem[], componentId: string): string {
  for (const item of content) {
    if ("path" in item && item.path === componentId) {
      return (item as ModulePageDef).modulePath;
    }
    if (isExpansion(item)) {
      return findComponent(item.content, componentId);
    }
  }
  throw Error("Could not find component with id: " + componentId);
}

const ContentComponent = () => {
  const [modulePath, setModulePath] = useState<string>();
  const { componentId } = useParams<ContentComponentParams>();

  usePromise(
    (signal) => fetchContentJson({ signal }),
    (content) => setModulePath(findComponent(content, componentId!)),
  );

  let Element = undefined;
  if (modulePath) {
    Element = lazy(() => import(joinPath(environment.resourceUrl, modulePath)));
  }
  return (
    <Suspense fallback={<Spinner />}>
      {modulePath && Element && <Element />}
    </Suspense>
  );
};

export default ContentComponent;
