package org.keycloak.testsuite.broker;

class BrokerTestConstants {

    final static String REALM_PROV_NAME = "provider";
    final static String REALM_CONS_NAME = "consumer";

    final static String IDP_OIDC_ALIAS = "kc-oidc-idp";
    final static String IDP_OIDC_PROVIDER_ID = "keycloak-oidc";

    final static String IDP_SAML_ALIAS = "kc-saml-idp";
    final static String IDP_SAML_PROVIDER_ID = "saml";

    final static String CLIENT_ID = "brokerapp";
    final static String CLIENT_SECRET = "secret";
    final static String VAULT_CLIENT_SECRET = "${vault.oidc_idp}";

    final static String USER_LOGIN = "testuser";
    final static String USER_EMAIL = "user@localhost.com";
    final static String USER_PASSWORD = "password";

    final static String IDP_SAML_SIGN_KEY = "MIICWwIBAAKBgQDVG8a7xGN6ZIkDbeecySygc" +
            "DfsypjUMNPE4QJjis8B316CvsZQ0hcTTLUyiRpHlHZys2k3xEhHBHymFC1AONcvzZzpb4" +
            "0tAhLHO1qtAnut00khjAdjR3muLVdGkM/zMC7G5s9iIwBVhwOQhy+VsGnCH91EzkjZ4SV" +
            "Er55KJoyQJQIDAQABAoGADaTtoG/+foOZUiLjRWKL/OmyavK9vjgyFtThNkZY4qHOh0h3" +
            "og0RdSbgIxAsIpEa1FUwU2W5yvI6mNeJ3ibFgCgcxqPk6GkAC7DWfQfdQ8cS+dCuaFTs8" +
            "ObIQEvU50YzeNPiiFxRA+MnauCUXaKm/PnDfjd4tPgru7XZvlGh0wECQQDsBbN2cKkBKp" +
            "r/b5oJiBcBaSZtWiMNuYBDn9x8uORj+Gy/49BUIMHF2EWyxOWz6ocP5YiynNRkPe21Zus" +
            "7PEr1AkEA5yWQOkxUTIg43s4pxNSeHtL+Ebqcg54lY2xOQK0yufxUVZI8ODctAKmVBMiC" +
            "KpU3mZQquOaQicuGtocpgxlScQI/YM31zZ5nsxLGf/5GL6KhzPJT0IYn2nk7IoFu7bjn9" +
            "BjwgcPurpLA52TNMYWQsTqAKwT6DEhG1NaRqNWNpb4VAkBehObAYBwMm5udyHIeEc+CzU" +
            "alm0iLLa0eRdiN7AUVNpCJ2V2Uo0NcxPux1AgeP5xXydXafDXYkwhINWcNO9qRAkEA58c" +
            "kAC5loUGwU5dLaugsGH/a2Q8Ac8bmPglwfCstYDpl8Gp/eimb1eKyvDEELOhyImAv4/uZ" +
            "V9wN85V0xZXWsw==";

    final static String IDP_SAML_SIGN_CERT = "MIIDdzCCAl+gAwIBAgIEbySuqTANBgkqhkiG" +
            "9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDV" +
            "QQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDg" +
            "YDVQQDEwdVbmtub3duMB4XDTE1MDEyODIyMTYyMFoXDTE3MTAyNDIyMTYyMFowbDEQMA4" +
            "GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQ" +
            "MA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93b" +
            "jCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAII/K9NNvXi9IySl7+l2zY/kKr" +
            "GTtuR4WdCI0xLW/Jn4dLY7v1/HOnV4CC4ecFOzhdNFPtJkmEhP/q62CpmOYOKApXk3tfm" +
            "m2rwEz9bWprVxgFGKnbrWlz61Z/cjLAlhD3IUj2ZRBquYgSXQPsYfXo1JmSWF5pZ9uh1F" +
            "Vqu9f4wvRqY20ZhUN+39F+1iaBsoqsrbXypCn1HgZkW1/9D9GZug1c3vB4wg1TwZZWRNG" +
            "txwoEhdK6dPrNcZ+6PdanVilWrbQFbBjY4wz8/7IMBzssoQ7Usmo8F1Piv0FGfaVeJqBr" +
            "cAvbiBMpk8pT+27u6p8VyIX6LhGvnxIwM07NByeSUCAwEAAaMhMB8wHQYDVR0OBBYEFFl" +
            "cNuTYwI9W0tQ224K1gFJlMam0MA0GCSqGSIb3DQEBCwUAA4IBAQB5snl1KWOJALtAjLqD" +
            "0mLPg1iElmZP82Lq1htLBt3XagwzU9CaeVeCQ7lTp+DXWzPa9nCLhsC3QyrV3/+oqNli8" +
            "C6NpeqI8FqN2yQW/QMWN1m5jWDbmrWwtQzRUn/rh5KEb5m3zPB+tOC6e/2bV3QeQebxeW" +
            "7lVMD0tSCviUg1MQf1l2gzuXQo60411YwqrXwk6GMkDOhFDQKDlMchO3oRbQkGbcP8Uei" +
            "KAXjMeHfzbiBr+cWz8NYZEtxUEDYDjTpKrYCSMJBXpmgVJCZ00BswbksxJwaGqGMPpUKm" +
            "CV671pf3m8nq3xyiHMDGuGwtbU+GE8kVx85menmp8+964nin";

    final static String REALM_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwg" +
            "gSkAgEAAoIBAQCCPyvTTb14vSMkpe/pds2P5Cqxk7bkeFnQiNMS1vyZ+HS2O79fxzp1eA" +
            "guHnBTs4XTRT7SZJhIT/6utgqZjmDigKV5N7X5ptq8BM/W1qa1cYBRip261pc+tWf3Iyw" +
            "JYQ9yFI9mUQarmIEl0D7GH16NSZklheaWfbodRVarvX+ML0amNtGYVDft/RftYmgbKKrK" +
            "218qQp9R4GZFtf/Q/RmboNXN7weMINU8GWVkTRrccKBIXSunT6zXGfuj3Wp1YpVq20BWw" +
            "Y2OMM/P+yDAc7LKEO1LJqPBdT4r9BRn2lXiaga3AL24gTKZPKU/tu7uqfFciF+i4Rr58S" +
            "MDNOzQcnklAgMBAAECggEAc0eibJYEO5d8QXW1kPgcHV2gBChv2mxDYnWYDLbIQSdNdfY" +
            "P/qABt/MTmm5KkWr16fcCEYoD1w0mqFBrtVn1msSusUmEAYGTXJMNumOmjjX1kzaTQMmq" +
            "eFBrwqwYz/xehWR5P+A7fSmwNV3KEeW19GvN5w5K96w0TLAQdFV3TQVPSytusDunwuR1y" +
            "ltMe1voaEDZ9z0Pi08YiEk2f6xhj5CMkoiw3mNImzfruphHullxU4FD05fH6tDeJ38152" +
            "7ILpAzDsgYZh4aFLKjUHem96bX4EL7FIzBJ6okgN78AZnUC/EaVfgFTw0qfhoWvZV4ruV" +
            "XXiMhCg4CMMRDq/k9iQKBgQDBNWsJMT84OnnWmQoJmZogkFV+tsGrSK6Re+aJxLWpishh" +
            "7dwAnT2OcagZvVdUb0FwNWu1D0B9/SKDDMRnnHBhOGDpH57m/eQdRU0oX1BD27xvffk0l" +
            "LcfD4BTxnR5e9jss8K4twc9jf0P1rxC/loGJ2NtCH0BrPHgz54Ea+96ewKBgQCsk3JDaa" +
            "PnFwzVYm2BXlhxOxLPsF4wvD2rIRAswZV4C5xebjand8nwiMmVpNd0PRLkEnkI+waURGv" +
            "2EY/P3JsssoiY8Xqe8f/1G+SQKre7lbqOas8rFoALepC0BYDiZDFy0Z9ZnRAFzRI5sgIt" +
            "7jpoMRD4xDNlmiV8X+yBxc3Y3wKBgQChDQsU1YUyNKQ8+sLAL9anEEkD4Ald4q8JPHN2I" +
            "Y+gLLxNzT0XEfsu0pTiJ8805axxgUYv3e/PVYNAJBNPnrqaf6lgiegl+jr9Hzhqz9CTUA" +
            "YqFaL2boSakoxQyNtsLI0s+cb1vDN/3uy0GDZDzcty18BsMagqDmRtFgNNAj/UIwKBgQC" +
            "ahbeFBv0cOPZjxisY8Bou4N8aGehsqNBq/0LVYExuXa8YmoTTdJ3bgw9Er4G/ccQNdUDs" +
            "uqAMeCtW/CiRzQ0ge4d1sprB4Rv3I4+HSsiS7SFKzfZLtWzXWlpg5qCdlWr1TR7qhYjIO" +
            "PO9t1beO3YOvwhcRoliyyAPenBxTmTfbwKBgDtm2WJ5VlQgNpIdOs1CCiqd0DFmWOmvBP" +
            "spPC1kySiy+Ndr9jNohRZkR7pEjgqA5E8rdzc88LirUN7bY5HFHRWN9KXrs5/o3O1K3GF" +
            "Cp64N6nvnPEYZ2zSJalcMC2fjSsJg26z8Dg1H+gfTIDUMoGiEAAnJXuqk+WayPU+fZMLn";

    final static String REALM_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgK" +
            "CAQEAgj8r0029eL0jJKXv6XbNj+QqsZO25HhZ0IjTEtb8mfh0tju/X8c6dXgILh5wU7OF0" +
            "0U+0mSYSE/+rrYKmY5g4oCleTe1+abavATP1tamtXGAUYqdutaXPrVn9yMsCWEPchSPZlE" +
            "Gq5iBJdA+xh9ejUmZJYXmln26HUVWq71/jC9GpjbRmFQ37f0X7WJoGyiqyttfKkKfUeBmR" +
            "bX/0P0Zm6DVze8HjCDVPBllZE0a3HCgSF0rp0+s1xn7o91qdWKVattAVsGNjjDPz/sgwHO" +
            "yyhDtSyajwXU+K/QUZ9pV4moGtwC9uIEymTylP7bu7qnxXIhfouEa+fEjAzTs0HJ5JQIDAQAB";
}
