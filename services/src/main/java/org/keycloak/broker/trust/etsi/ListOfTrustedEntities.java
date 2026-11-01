package org.keycloak.broker.trust.etsi;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The parts of an ETSI TS 119 602 V1.1.1 List of Trusted Entities in its JSON binding that trust
 * decisions depend on.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ListOfTrustedEntities(@JsonProperty("ListAndSchemeInformation") SchemeInformation schemeInformation,
                             @JsonProperty("TrustedEntitiesList") List<TrustedEntity> trustedEntities) {

    Stream<ServiceInformation> services() {
        return (trustedEntities == null ? Stream.<TrustedEntity>empty() : trustedEntities.stream())
                .flatMap(TrustedEntity::servicesInformation);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Document(@JsonProperty("LoTE") ListOfTrustedEntities list) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SchemeInformation(@JsonProperty("LoTEType") String type,
                             @JsonProperty("NextUpdate") String nextUpdate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrustedEntity(@JsonProperty("TrustedEntityServices") List<TrustedEntityService> services) {

        Stream<ServiceInformation> servicesInformation() {
            return services == null ? Stream.empty() : services.stream().map(TrustedEntityService::information);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrustedEntityService(@JsonProperty("ServiceInformation") ServiceInformation information) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ServiceInformation(@JsonProperty("ServiceTypeIdentifier") String type,
                              @JsonProperty("ServiceStatus") String status,
                              @JsonProperty("ServiceDigitalIdentity") DigitalIdentity digitalIdentity) {

        Stream<String> encodedCertificates() {
            return digitalIdentity == null || digitalIdentity.certificates() == null ? Stream.empty()
                    : digitalIdentity.certificates().stream().map(X509CertificateEntry::value);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DigitalIdentity(@JsonProperty("X509Certificates") List<X509CertificateEntry> certificates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record X509CertificateEntry(@JsonProperty("val") String value) {
    }
}
