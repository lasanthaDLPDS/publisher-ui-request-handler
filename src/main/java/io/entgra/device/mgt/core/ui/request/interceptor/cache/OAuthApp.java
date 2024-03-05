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

package io.entgra.device.mgt.core.ui.request.interceptor.cache;

/**
 * The data object used for Login Cache
 */
public class OAuthApp {

    private String appName;
    private String appOwner;
    private String clientId;
    private String clientSecret;
    private String encodedClientApp;

    public OAuthApp(String appName, String appOwner, String clientId, String clientSecret, String encodedClientApp) {
        this.appName = appName;
        this.appOwner = appOwner;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.encodedClientApp = encodedClientApp;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppOwner() {
        return appOwner;
    }

    public void setAppOwner(String appOwner) {
        this.appOwner = appOwner;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getEncodedClientApp() {
        return encodedClientApp;
    }

    public void setEncodedClientApp(String encodedClientApp) {
        this.encodedClientApp = encodedClientApp;
    }
}
