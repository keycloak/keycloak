package org.keycloak.protocol.oid4vc.issuance.mappers;

import org.keycloak.models.ProtocolMapperModel;

/**
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VPMapperFactory {

    private OID4VPMapperFactory() {
        // prevent instantiation
    }

    public static OID4VPMapper createOID4VCMapper(ProtocolMapperModel mapperModel) {
        return switch (mapperModel.getProtocolMapper()) {
            case OID4VPTargetRoleMapper.MAPPER_ID -> new OID4VPTargetRoleMapper().setMapperModel(mapperModel);
            case OID4VPSubjectIdMapper.MAPPER_ID -> new OID4VPSubjectIdMapper().setMapperModel(mapperModel);
            case OID4VPUserAttributeMapper.MAPPER_ID -> new OID4VPUserAttributeMapper().setMapperModel(mapperModel);
            case OID4VPStaticClaimMapper.MAPPER_ID -> new OID4VPStaticClaimMapper().setMapperModel(mapperModel);
            case OID4VPTypeMapper.MAPPER_ID -> new OID4VPTypeMapper().setMapperModel(mapperModel);
            case OID4VPContextMapper.MAPPER_ID -> new OID4VPContextMapper().setMapperModel(mapperModel);
            default -> throw new OID4VPMapperException(
                    String.format("No mapper with id %s exists.", mapperModel.getProtocolMapper()));
        };
    }
}