package org.keycloak.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeUtils {

    /**
     * Encode specified String as a QR code in PNG format
     *
     * @param contentToEncode content to encode
     * @param width width of the resulting QR code
     * @param height height of the resulting QR code
     * @return bytes with encoded QR code
     */
    public static byte[] encodeAsQRBytes(String contentToEncode, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(contentToEncode, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
        bos.close();
        return bos.toByteArray();
    }

    /**
     * Encode specified String as a QR code in PNG format
     *
     * @param contentToEncode content to encode
     * @param width width of the resulting QR code
     * @param height height of the resulting QR code
     * @return Encoded QR code returned in the Base64 encoded string
     */
    public static String encodeAsQRString(String contentToEncode, int width, int height) throws WriterException, IOException {
        byte[] bos = encodeAsQRBytes(contentToEncode, width, height);
        return Base64.getEncoder().encodeToString(bos);
    }

}
