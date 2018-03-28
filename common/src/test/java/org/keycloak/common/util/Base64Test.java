package org.keycloak.common.util;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class Base64Test {

	public static final String WIERD_STRING = "eyJqdGkiOiJiYjE2NTI3Zi0yNWNhLTQzNmMtYThiOS04ZDZmNjgzODE2YjciLCJleHAiOjE1MjIyNjMxMDAsIm5iZiI6MCwiaWF0IjoxNTIyMjYzMDQwLCJpc3MiOiJodHRwczovL3NlY3VyZS10bXBzc28tZGVtby5wYWFzLmxvY2FsL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6Im9pZGMtdGVzdCIsInN1YiI6IjE4ZjUwMjk0LTQyNWItNDY3My05NDNkLTFiMzVkMWE3M2I2YiIsInR5cCI6IkJlYXJlciIsImF6cCI6Im9pZGMtdGVzdCIsImF1dGhfdGltZSI6MTUyMjI2MzA0MCwic2Vzc2lvbl9zdGF0ZSI6Ijk5NGIyODhmLTNmZWMtNGQ3MC05YzFlLTkxMTk2NTYxNDIzMyIsImFjciI6IjEiLCJjbGllbnRfc2Vzc2lvbiI6IjViNjZkZTkzLTA3NDktNGYxYS1iMDU5LTk5ZDk1ZmI1ZWM1NSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhdXRoZW50aWNhdGVkIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwibmFtZSI6InVzZXIgb25lIiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlcjEiLCJnaXZlbl9uYW1lIjoidXNlciIsImZhbWlseV9uYW1lIjoib25lIiwiZW1haWwiOiJ1c2VyMUByZWRoYXQuY29tIn0";
	public static final String WIERD_STRING_DECODED = "{\"jti\":\"bb16527f-25ca-436c-a8b9-8d6f683816b7\",\"exp\":1522263100,\"nbf\":0,\"iat\":1522263040,\"iss\":\"https://secure-tmpsso-demo.paas.local/auth/realms/master\",\"aud\":\"oidc-test\",\"sub\":\"18f50294-425b-4673-943d-1b35d1a73b6b\",\"typ\":\"Bearer\",\"azp\":\"oidc-test\",\"auth_time\":1522263040,\"session_state\":\"994b288f-3fec-4d70-9c1e-911965614233\",\"acr\":\"1\",\"client_session\":\"5b66de93-0749-4f1a-b059-99d95fb5ec55\",\"allowed-origins\":[],\"realm_access\":{\"roles\":[\"authenticated\",\"uma_authorization\"]},\"resource_access\":{\"account\":{\"roles\":[\"manage-account\",\"view-profile\"]}},\"name\":\"user one\",\"preferred_username\":\"user1\",\"given_name\":\"user\",\"family_name\":\"one\",\"email\":\"user1@redhat.com\"}";

	@Test
	public void keycloakDecoderShouldDecodeWierdString() throws Exception {
		final String decoded = new String(Base64.decode(WIERD_STRING));
		System.out.println(decoded);
		assertThat(decoded, equalTo(WIERD_STRING_DECODED));
	}

	@Test
	public void javaDecoderShouldDecodeWierdString() throws Exception {
		final String decoded = new String(java.util.Base64.getDecoder().decode(WIERD_STRING));
		System.out.println(decoded);
		assertThat(decoded, equalTo(WIERD_STRING_DECODED));
	}
}
