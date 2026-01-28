package org.keycloak.operator;

import io.fabric8.crdv2.generator.CRDPostProcessor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;

public class MultiVersionCRDPostProcessor implements CRDPostProcessor {

    @Override
    public HasMetadata process(HasMetadata crd, String crdSpecVersion) {
        CustomResourceDefinition v1crd = (CustomResourceDefinition) crd;
        var mainVersion = v1crd.getSpec().getVersions().get(0);
        v1crd.getSpec().getVersions()
                .add(mainVersion.edit().withDeprecated()
                        .withDeprecationWarning("Please migrate to " + Constants.CRDS_VERSION)
                        .withName(Constants.CRDS_VERSION_ALPHA).withStorage(false).build());
        return v1crd;
    }

}
