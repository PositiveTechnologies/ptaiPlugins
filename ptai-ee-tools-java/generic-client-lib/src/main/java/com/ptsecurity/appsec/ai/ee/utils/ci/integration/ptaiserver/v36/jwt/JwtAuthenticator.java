package com.ptsecurity.appsec.ai.ee.utils.ci.integration.ptaiserver.v36.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.base.Base;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.ptaiserver.v36.BaseClient;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.ptaiserver.v36.utils.ApiClientHelper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Class implements JWT authentication for generic XxxApi instance. As XxxApi classes
 * have no common ancestor we need to pass Object type to constructor and use
 * ApiClientHelper to call methods.
 */
@Log
@RequiredArgsConstructor
public class JwtAuthenticator extends Base implements Authenticator {
    private static final Pattern INVALID_TOKEN_ERROR = Pattern.compile("\\s*error\\s*=\\s*\"?invalid_token\"?");
    private static final Pattern UNAUTHORIZED_ERROR = Pattern.compile("\\s*error\\s*=\\s*\"?unauthorized\"?");

    @NonNull
    protected final BaseClient client;

    @NonNull
    protected final ApiClientHelper helper;

    private boolean fetchingToken = false;

    /**
     * Authenticate failed API request using JWT scheme
     * @param route
     * @param response Response from server
     * @return Modified API request with JWT access token in Authorization header
     * @throws IOException
     */
    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {

        // Any authentication problem while getting JWT treated as a critical failure
        if (fetchingToken) return null;
        fetchingToken = true;

        Request res = null;
        do {
            String authResponse = response.header("WWW-Authenticate");
            if (StringUtils.isEmpty(authResponse) || !authResponse.startsWith("Bearer")) {
                log.severe("Unauthorized, but invalid WWW-Authenticate response header");
                break;
            }
            Response jwtResponse;
            if (UNAUTHORIZED_ERROR.matcher(authResponse).find() || (null == client.getJWT())) {
                // Need to acquire new JWT using client id/secret and username/password credentials
                Request request = response.request().newBuilder()
                        .url(client.getUrl() + "/api/Auth/signin?scopeType=AccessToken")
                        .header("Access-Token", client.getToken())
                        .get()
                        .build();
                jwtResponse = helper.getHttpClient().newBuilder().build().newCall(request).execute();
            } else if (INVALID_TOKEN_ERROR.matcher(authResponse).find()) {
                // Need to acquire new JWT using client id/secret and refresh_token
                Request request = response.request().newBuilder()
                        .url(client.getUrl() + "/api/Auth/refreshToken")
                        .header("Authorization", "Bearer " + client.getJWT().getRefreshToken())
                        .get()
                        .build();
                jwtResponse = helper.getHttpClient().newBuilder().build().newCall(request).execute();
                if (HttpStatus.SC_OK != jwtResponse.code()) {
                    // JWT refresh failed. May be refrest token expored, let's re-authenticate using full set of credentials
                    log.log(Level.SEVERE, "JWT refresh failed. Code is {0}", jwtResponse.code());
                    client.setJWT(null);
                    request = response.request().newBuilder()
                            .url(client.getUrl() + "/api/Auth/signin?scopeType=AccessToken")
                            .header("Access-Token", client.getToken())
                            .get()
                            .build();
                    jwtResponse = helper.getHttpClient().newBuilder().build().newCall(request).execute();
                }
            } else break;
            if (HttpStatus.SC_OK != jwtResponse.code()) {
                log.log(Level.SEVERE, "Authorization failed. Code is {0}", jwtResponse.code());
                break;
            }
            // Save shared JWT in the client
            client.setJWT(new ObjectMapper().readValue(jwtResponse.body().string(), JwtResponse.class));
            fetchingToken = false;
            // Tell OkHTTP to resend failed request with new JWT
            res = response.request().newBuilder()
                    .header("Authorization", "Bearer " + client.getJWT().getAccessToken())
                    .build();
        } while (false);
        fetchingToken = false;
        return res;
    }
}