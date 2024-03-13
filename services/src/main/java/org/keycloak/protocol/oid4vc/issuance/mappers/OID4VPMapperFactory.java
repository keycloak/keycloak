/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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