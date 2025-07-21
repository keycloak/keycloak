package org.keycloak.quarkus.runtime.oas;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;

import java.util.Map;
import java.util.stream.Collectors;


public class OASModelFilter implements OASFilter {

  @Override
  public void filterOpenAPI(OpenAPI openAPI) {
    Map<String, PathItem> newPaths = openAPI.getPaths().getPathItems().entrySet().stream()
        .filter(entry -> entry.getKey().contains("/v2/"))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> sortOperationsByMethod(entry.getValue())
        ));


    openAPI.getPaths().setPathItems(newPaths);
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


}
