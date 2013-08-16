package org.keycloak.forms;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.resteasy.logging.Logger;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@WebServlet(urlPatterns = "/forms/qrcode")
public class QRServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(QRServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] size = req.getParameter("size").split("x");
        int width = Integer.parseInt(size[0]);
        int height = Integer.parseInt(size[1]);

        String contents = req.getParameter("contents");

        try {
            QRCodeWriter writer = new QRCodeWriter();

            BitMatrix bitMatrix = writer.encode(contents, BarcodeFormat.QR_CODE, width, height);

            MatrixToImageWriter.writeToStream(bitMatrix, "png", resp.getOutputStream());
            resp.setContentType("image/png");
        } catch (Exception e) {
            log.warn("Failed to generate qr code", e);
            resp.sendError(500);
        }
    }

}
