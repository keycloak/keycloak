import org.picketlink.common.util.Base32;
import org.picketlink.idm.credential.util.TimeBasedOTP;

public class TotpUtil {

    public static void main(String[] args) {
        String google = "PJBX GURY NZIT C2JX I44T S3D2 JBKD G6SB";
        google = google.replace(" ", "");
        google = new String(Base32.decode(google));
        TimeBasedOTP otp = new TimeBasedOTP();
        System.out.println(otp.generate(google));
    }

}
