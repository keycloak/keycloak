package org.keycloak.quarkus.runtime.oas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

@OpenApiFilter(OpenApiFilter.RunStage.BUILD)
public class OASModelFilter implements OASFilter {

  @Override
  public void filterOpenAPI(OpenAPI openAPI) {
    // Filter Paths that have the '/admin/api/v2' prefix
    Map<String, PathItem> newPaths = openAPI.getPaths().getPathItems().entrySet().stream()
        .filter(entry -> entry.getKey().startsWith("/admin/api/v2"))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> sortOperationsByMethod(entry.getValue())
        ));

    // Replace ALL Paths with filtered Paths
    var paths = OASFactory.createPaths();
    newPaths.forEach(paths::addPathItem);
    openAPI.setPaths(paths);

    // Compute tags that are actually used by remaining operations
    Set<String> usedTags = newPaths.values().stream()
        .flatMap(pi -> operationsOf(pi).stream())
        .flatMap(op -> Optional.ofNullable(op.getTags()).orElseGet(List::of).stream())
        .collect(Collectors.toSet());

    // Drop top-level tags not used anywhere
    if (openAPI.getTags() != null) {
      var filteredTags = openAPI.getTags().stream()
          .filter(t -> t.getName() != null && usedTags.contains(t.getName()))
          .collect(Collectors.toList());
      openAPI.setTags(filteredTags.isEmpty() ? null : filteredTags);
    }
  }

  private PathItem sortOperationsByMethod(PathItem pathItem) {
    PathItem sortedPathItem = OASFactory.createPathItem();

    // Add operations order: GET -> POST -> PUT -> PATCH -> DELETE -> HEAD -> OPTIONS -> TRACE
    if (pathItem.getGET() != null) {
      sortedPathItem.setGET(pathItem.getGET());
    }
    if (pathItem.getPOST() != null) {
      sortedPathItem.setPOST(pathItem.getPOST());
    }
    if (pathItem.getPUT() != null) {
      sortedPathItem.setPUT(pathItem.getPUT());
    }
    if (pathItem.getPATCH() != null) {
      sortedPathItem.setPATCH(pathItem.getPATCH());
    }
    if (pathItem.getDELETE() != null) {
      sortedPathItem.setDELETE(pathItem.getDELETE());
    }
    if (pathItem.getHEAD() != null) {
      sortedPathItem.setHEAD(pathItem.getHEAD());
    }
    if (pathItem.getOPTIONS() != null) {
      sortedPathItem.setOPTIONS(pathItem.getOPTIONS());
    }
    if (pathItem.getTRACE() != null) {
      sortedPathItem.setTRACE(pathItem.getTRACE());
    }

    sortedPathItem.setSummary(pathItem.getSummary());
    sortedPathItem.setDescription(pathItem.getDescription());
    sortedPathItem.setServers(pathItem.getServers());
    sortedPathItem.setParameters(pathItem.getParameters());

    return sortedPathItem;
  }

  private List<Operation> operationsOf(PathItem pi) {
    List<Operation> ops = new ArrayList<>(8);
    if (pi.getGET() != null) ops.add(pi.getGET());
    if (pi.getPOST() != null) ops.add(pi.getPOST());
    if (pi.getPUT() != null) ops.add(pi.getPUT());
    if (pi.getPATCH() != null) ops.add(pi.getPATCH());
    if (pi.getDELETE() != null) ops.add(pi.getDELETE());
    if (pi.getHEAD() != null) ops.add(pi.getHEAD());
    if (pi.getOPTIONS() != null) ops.add(pi.getOPTIONS());
    if (pi.getTRACE() != null) ops.add(pi.getTRACE());
    return ops;
  }
}
