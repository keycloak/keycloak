package org.keycloak.testsuite.excluded;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by michigerber on 21.12.14.
 */
public class PostServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder response = new StringBuilder();
        if(req.getUserPrincipal() != null){
            response.append("Hello "+req.getUserPrincipal().getName()).append(", ");
        }
        response.append("you said: ");
        Scanner scanner = new Scanner(req.getInputStream());
        while(scanner.hasNext()){
            response.append(scanner.next());
        }
        resp.getWriter().write(response.toString());
    }

}
