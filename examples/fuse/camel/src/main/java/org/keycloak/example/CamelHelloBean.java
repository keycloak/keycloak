package org.keycloak.example;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CamelHelloBean {

    public String hello() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "Hello admin! It's " + sdf.format(new Date());
    }
}
