package org.keycloak.services.resources;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.servlet.ServletException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Create a barcode image
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/qrcode")
public class QRCodeResource {

    /**
     * Create a bar code image
     *
     * @param contents
     * @param size
     * @return
     * @throws ServletException
     * @throws IOException
     * @throws WriterException
     */
    @GET
    @Produces("image/png")
    public Response createQrCode(@QueryParam("contents") String contents, @QueryParam("size") String size) throws ServletException, IOException, WriterException {
        int width = 256;
        int height = 256;

        if (size != null) {
            String[] s = size.split("x");
            try {
                width = Integer.parseInt(s[0]);
                height = Integer.parseInt(s[1]);
            } catch (Throwable t) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }

        if (contents == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (width > 1000 || height > 1000 || contents.length() > 1000) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        QRCodeWriter writer = new QRCodeWriter();
        final BitMatrix bitMatrix = writer.encode(contents, BarcodeFormat.QR_CODE, width, height);

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException,
                    WebApplicationException {
                MatrixToImageWriter.writeToStream(bitMatrix, "png", os);
            }
        };

        return Response.ok(stream).build();
    }

}
