/*
 * Copyright (c) 2018 - 2023, Entgra (Pvt) Ltd. (http://www.entgra.io) All Rights Reserved.
 *
 * Entgra (Pvt) Ltd. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.entgra.device.mgt.core.ui.request.interceptor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.entgra.device.mgt.core.ui.request.interceptor.beans.AuthData;
import io.entgra.device.mgt.core.ui.request.interceptor.beans.ProxyResponse;
import io.entgra.device.mgt.core.ui.request.interceptor.util.HandlerConstants;
import io.entgra.device.mgt.core.ui.request.interceptor.util.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

@MultipartConfig
@WebServlet("/default-oauth2-credentials")
public class DefaultOauth2TokenHandler extends HttpServlet {
    private static final Log log = LogFactory.getLog(DefaultTokenHandler.class);
    private static final long serialVersionUID = 2254408216447549205L;


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            HttpSession httpSession = req.getSession(false);

            if (httpSession != null) {
                AuthData authData = (AuthData) httpSession.getAttribute(HandlerConstants.SESSION_AUTH_DATA_KEY);
                if (authData == null) {
                    HandlerUtil.sendUnAuthorizeResponse(resp);
                    return;
                }

                AuthData defaultAuthData = (AuthData) httpSession
                        .getAttribute(HandlerConstants.SESSION_DEFAULT_AUTH_DATA_KEY);
                if (defaultAuthData != null) {
                    HandlerUtil.handleSuccess(resp, constructSuccessProxyResponse(defaultAuthData.getAccessToken()));
                    return;
                }

                String clientId = authData.getClientId();
                String clientSecret = authData.getClientSecret();

                String queryString = req.getQueryString();
                String scopeString = "";
                if (StringUtils.isNotEmpty(queryString)) {
                    scopeString = req.getParameter("scopes");
                    if (scopeString != null) {
                        scopeString = "?scopes=" + scopeString;
                    }
                }

                ClassicHttpRequest defaultTokenRequest =
                        ClassicRequestBuilder.get(req.getScheme() + HandlerConstants.SCHEME_SEPARATOR
                                        + System.getProperty(HandlerConstants.IOT_GW_HOST_ENV_VAR)
                                        + HandlerConstants.COLON + HandlerUtil.getGatewayPort(req.getScheme())
                                        + "/api/device-mgt/v1.0/devices/" + clientId + HandlerConstants.URI_SEPARATOR
                                        + clientSecret + "/default-token" + scopeString)
                                .setHeader(org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE, org.apache.hc.core5.http.ContentType.APPLICATION_FORM_URLENCODED.toString())
                                .setHeader(org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION, HandlerConstants.BEARER + authData.getAccessToken())
                                .build();

                ProxyResponse tokenResultResponse = HandlerUtil.execute(defaultTokenRequest);

                if (tokenResultResponse.getExecutorResponse().contains(HandlerConstants.EXECUTOR_EXCEPTION_PREFIX)) {
                    log.error("Error occurred while invoking the API to get default token data.");
                    HandlerUtil.handleError(resp, tokenResultResponse);
                    return;
                }
                JsonNode tokenResult = tokenResultResponse.getData();
                if (tokenResult == null) {
                    log.error("Invalid default token response is received.");
                    HandlerUtil.handleError(resp, tokenResultResponse);
                    return;
                }

                AuthData newDefaultAuthData = new AuthData();
                newDefaultAuthData.setClientId(clientId);
                newDefaultAuthData.setClientSecret(clientSecret);

                String defaultToken = tokenResult.get("accessToken").asText();
                newDefaultAuthData.setAccessToken(defaultToken);
                newDefaultAuthData.setRefreshToken(tokenResult.get("refreshToken").asText());
                newDefaultAuthData.setScope(tokenResult.get("scopes"));
                httpSession.setAttribute(HandlerConstants.SESSION_DEFAULT_AUTH_DATA_KEY, newDefaultAuthData);

                HandlerUtil.handleSuccess(resp, constructSuccessProxyResponse(defaultToken));
            } else {
                HandlerUtil.sendUnAuthorizeResponse(resp);
            }
        } catch (IOException e) {
            log.error("Error occurred when processing GET request to get default token.", e);
        }
    }

    /**
     * Get Success Proxy Response
     * @param defaultAccessToken Access token which has default scope
     * @return {@link ProxyResponse}
     */
    private ProxyResponse constructSuccessProxyResponse (String defaultAccessToken) {

        URIBuilder ub = new URIBuilder();
        ub.setScheme(HandlerConstants.WSS_PROTOCOL);
        ub.setHost(System.getProperty(HandlerConstants.IOT_REMOTE_SESSION_HOST_ENV_VAR));
        ub.setPort(Integer.parseInt(System.getProperty(HandlerConstants.IOT_REMOTE_SESSION_HTTPS_PORT_ENV_VAR)));
        ub.setPath(HandlerConstants.REMOTE_SESSION_CONTEXT);

        URIBuilder ub2 = new URIBuilder();
        ub2.setScheme(HandlerConstants.WSS_PROTOCOL);
        ub2.setHost(System.getProperty(HandlerConstants.IOT_GW_HOST_ENV_VAR));
        ub2.setPort(Integer.parseInt(System.getProperty(HandlerConstants.IOT_GATEWAY_WEBSOCKET_WSS_PORT_ENV_VAR)));

        URIBuilder ub3 = new URIBuilder();
        ub3.setScheme(HandlerConstants.WS_PROTOCOL);
        ub3.setHost(System.getProperty(HandlerConstants.IOT_GW_HOST_ENV_VAR));
        ub3.setPort(Integer.parseInt(System.getProperty(HandlerConstants.IOT_GATEWAY_WEBSOCKET_WS_PORT_ENV_VAR)));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        Map<String, Object> nodeMap = mapper.convertValue(node, new TypeReference<>() {
        });
        nodeMap.put("default-access-token", defaultAccessToken);
        nodeMap.put("remote-session-base-url", ub.toString());
        nodeMap.put("secured-websocket-gateway-url", ub2.toString());
        nodeMap.put("unsecured-websocket-gateway-url", ub3.toString());

        ProxyResponse proxyResponse = new ProxyResponse();
        proxyResponse.setCode(HttpStatus.SC_OK);
        proxyResponse.setStatus(ProxyResponse.Status.SUCCESS);
        proxyResponse.setData(mapper.convertValue(nodeMap, JsonNode.class));
        return proxyResponse;
    }
}
