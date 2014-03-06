package org.keycloak.example;

import org.json.JSONObject;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.social.utils.SimpleHttp;
import org.keycloak.util.JsonSerialization;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CustomerCli {

    private static String authServerUrl;
    private static String realm;
    private static String clientId;

    private static String accessToken;
    private static BufferedReader br;

    public static void main(String[] args) throws Exception {
        try {
            String f = args.length > 0 ? args[0] : "keycloak.json";
            ApplicationManager.InstallationAdapterConfig config = JsonSerialization.readValue(new FileInputStream(new File(f)), ApplicationManager.InstallationAdapterConfig.class);

            authServerUrl = config.getAuthServerUrl();
            realm = config.getRealm();
            clientId = config.getResource();
        } catch (Throwable t) {
            System.err.println("Failed to load config:");
            t.printStackTrace();
            System.exit(1);
        }

        br = new BufferedReader(new InputStreamReader(System.in));

        printHelp();

        for (String s = br.readLine(); s != null; s = br.readLine()) {
            if (s.equals("login")) {
                login();

            } else if (s.equals("login-desktop")) {
                loginDesktop();
            } else if (s.equals("login-manual")) {
                loginManual();
            } else if (s.equals("profile")) {
                profile();
            } else if (s.equals("token")) {
                token();
            } else if (s.equals("exit")) {
                System.exit(0);
            } else {
                printHelp();
            }
        }
    }

    public static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  login - login with desktop browser if available, otherwise do manual login");
        System.out.println("  login-manual - manual login");
        System.out.println("  login-desktop - desktop login");
        System.out.println("  profile - retrieve user profile");
        System.out.println("  token - show token details");
        System.out.println("  exit - exit");

    }

    public static void login() {
        try {
            if (Desktop.isDesktopSupported()) {
                loginDesktop();
            } else {
                loginManual();
            }

        } catch (Throwable e) {
            System.err.println("Failed to log in user: " + e.getMessage());
        }
    }

    public static void loginDesktop() {
        try {
            CallbackListener callback = new CallbackListener();
            callback.start();

            String redirectUri = URLEncoder.encode("http://localhost:" + callback.getPort(), "utf-8");
            String state = UUID.randomUUID().toString();

            String loginUrl = authServerUrl + "/rest/realms/" + realm + "/tokens/login?" +
                    "client_id=" + clientId +
                    "&redirect_uri=" + redirectUri +
                    "&state=" + state;

            Desktop.getDesktop().browse(new URI(loginUrl));

            callback.join();

            if (!state.equals(callback.getStateParam())) {
                System.err.println("Invalid state received");
                return;
            }

            if (callback.getError() != null) {
                System.err.println("Failed to login: " + callback.getError());
                return;
            }

            System.out.println("User logged in");

            String tokenUrl = authServerUrl + "/rest/realms/" + realm + "/tokens/access/codes";
            JSONObject response = SimpleHttp.doPost(tokenUrl).param("client_id", clientId).param("code", callback.getCode()).asJson();
            accessToken = response.getString("access_token");
            System.out.println("Access token received");

            return;
        } catch (Throwable e) {
            System.err.println("Failed to log in user: " + e.getMessage());
        }
    }

    public static void loginManual() {
        try {
            CallbackListener callback = new CallbackListener();
            callback.start();

            String redirectUri = URLEncoder.encode("urn:ietf:wg:oauth:2.0:oob", "utf-8");

            String loginUrl = authServerUrl + "/rest/realms/" + realm + "/tokens/login?" +
                    "client_id=" + clientId +
                    "&redirect_uri=" + redirectUri;

            System.out.println("Open the following URL in a browser and paste the code back:");
            System.out.println(loginUrl);
            System.out.print("code: ");

            String code = br.readLine().trim();

            String tokenUrl = authServerUrl + "/rest/realms/" + realm + "/tokens/access/codes";
            JSONObject response = SimpleHttp.doPost(tokenUrl).param("client_id", clientId).param("code", code).asJson();
            accessToken = response.getString("access_token");
            System.out.println("Access token received");

            return;
        } catch (Throwable e) {
            System.err.println("Failed to log in user: " + e.getMessage());
        }
    }

    public static void profile() {
        try {
            String profileUrl = authServerUrl + "/rest/realms/" + realm + "/account";
            JSONObject profile = SimpleHttp.doGet(profileUrl).header("Accept", "application/json").header("Authorization", "Bearer " + accessToken).asJson();
            System.out.println(profile.toString(2));
        } catch (Throwable e) {
            System.err.println("Failed: " + e.getMessage());
        }
    }

    public static void token() {
        try {
            JWSInput jws = new JWSInput(accessToken);
            System.out.println(new JSONObject(new String(jws.getContent())).toString(2));
        } catch (Throwable e) {
            System.err.println("Failed to log in user: " + e.getMessage());
        }
    }

    public static class CallbackListener extends Thread {

        private String code;

        private String error;

        private String state;
        private final ServerSocket server;

        public CallbackListener() throws IOException {
            server = new ServerSocket(0);
        }

        public int getPort() {
            return server.getLocalPort();
        }

        @Override
        public void run() {
            try {
                Socket socket = server.accept();

                System.out.println("Request received");

                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String request = br.readLine();

                System.out.println(request);

                String[] params = request.split(" ")[1].substring(2).split("&");
                System.out.println(params.length);

                for (String param : params) {
                    String[] p = param.split("=");
                    System.out.println(p[0]);
                    if (p[0].equals("code")) {
                        code = p[1];
                    } else if (p[0].equals("error")) {
                        error = p[1];
                    } else if (p[0].equals("state")) {
                        state = p[1];
                    }
                }

                PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                pw.println("Please close and return to application");
                pw.flush();

                socket.close();
            } catch (IOException e) {
                error = "Local error: " + e.getMessage();
            }

            try {
                server.close();
            } catch (IOException e) {
            }
        }

        public String getCode() throws InterruptedException {
            return code;
        }

        public String getError() {
            return error;
        }

        public String getStateParam() {
            return state;
        }
    }

}
