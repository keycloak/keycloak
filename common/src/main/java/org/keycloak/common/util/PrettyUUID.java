package org.keycloak.common.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public class PrettyUUID {

    private final UUID uuid;
    private byte[] signature;

    public static String create() {
        return encode(UUID.randomUUID());
    }

    public static String encode(UUID uuid) {
        return encode(uuid, null);
    }

    public static String encode(UUID uuid, byte[] signature) {
        boolean signed = signature != null;
        ByteBuffer uuidBuffer = ByteBuffer.allocate(signed ? 48 : 16);
        uuidBuffer.putLong(uuid.getMostSignificantBits());
        uuidBuffer.putLong(uuid.getLeastSignificantBits());
        if (signed) {
            uuidBuffer.put(signature);
        }
        return Base62.encodeToString(uuidBuffer.array());
    }

    public static PrettyUUID decode(String encoded) {
        boolean signed = encoded.length() > 26;

        byte[] decode = Base62.decode(encoded);
        ByteBuffer wrap = ByteBuffer.wrap(decode);

        UUID uuid = new UUID(wrap.getLong(), wrap.getLong());
        PrettyUUID prettyUUID = new PrettyUUID(uuid);

        if (signed) {
            prettyUUID.signature = new byte[32];
            wrap.get(prettyUUID.signature);
        }

        return prettyUUID;
    }

    private PrettyUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isSigned() {
        return signature != null;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void sign(byte[] signature) {
        this.signature = signature;
    }

}
