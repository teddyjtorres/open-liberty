/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.web;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.TraceConstants;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

/**
 *
 */
public class WebSSOUtils {

    public static final TraceComponent tc = Tr.register(WebSSOUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public String getRequestUrlWithEncodedQueryString(HttpServletRequest req) {
        StringBuffer reqURL = req.getRequestURL();
        if (req.getQueryString() != null) {
            reqURL.append("?");
            reqURL.append(getUrlEncodedQueryString(req));
        }
        return reqURL.toString();
    }

    public String getUrlEncodedQueryString(HttpServletRequest req) {
        StringBuilder qs = new StringBuilder();
        Map<String, String[]> params = req.getParameterMap();
        if (!params.isEmpty()) {
            qs.append(getUrlEncodedQueryStringFromParameterMap(params));
        }
        return qs.toString();
    }

    public String getUrlEncodedQueryStringFromParameterMap(Map<String, String[]> params) {
        StringBuilder qs = new StringBuilder();
        Iterator<Entry<String, String[]>> iter = params.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String[]> param = iter.next();
            qs.append(getUrlEncodedParameterAndValues(param.getKey(), param.getValue()));

            if (iter.hasNext() && qs.charAt(qs.length() - 1) != '&') {
                // Append a '&' character if there are more parameters to encode
                qs.append("&");
            }
        }
        return qs.toString();
    }

    public String getUrlEncodedParameterAndValues(String key, String[] values) {
        StringBuilder paramAndValue = new StringBuilder();
        // The parameter may or may not have values, but go ahead and add it to the string since at least the key is present
        paramAndValue.append(WebUtils.urlEncode(key));

        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                paramAndValue.append("=" + WebUtils.urlEncode(value));
                if (i < values.length - 1) {
                    // Append a '&' character if there are more parameter values to encode
                    paramAndValue.append("&" + WebUtils.urlEncode(key));
                }
            }
        }
        return paramAndValue.toString();
    }

    public ReferrerURLCookieHandler getCookieHandler() {
        WebAppSecurityConfig config = getWebAppSecurityConfig();
        if (config != null) {
            return config.createReferrerURLCookieHandler();
        }
        return new ReferrerURLCookieHandler(config);
    }

    public Cookie createCookie(String cookieName, @Sensitive String cookieValue, HttpServletRequest req) {
        return createCookie(cookieName, cookieValue, -1, req);
    }

    public Cookie createCookie(String cookieName, @Sensitive String cookieValue, int maxAge, HttpServletRequest req) {
        Cookie cookie = getCookieHandler().createCookie(cookieName, cookieValue, req);
        String domainName = getSsoDomain(req);
        if (domainName != null && !domainName.isEmpty()) {
            cookie.setDomain(domainName);
        }
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    public String getSsoDomain(HttpServletRequest req) {
        WebAppSecurityConfig webAppSecConfig = getWebAppSecurityConfig();
        SSOCookieHelper ssoCookieHelper = webAppSecConfig.createSSOCookieHelper();
        return ssoCookieHelper.getSSODomainName(req, webAppSecConfig.getSSODomainList(), webAppSecConfig.getSSOUseDomainFromURL());
    }

    WebAppSecurityConfig getWebAppSecurityConfig() {
        return WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
    }
    
//    public void savePostParameters(HttpServletRequest request) {
//        PostParameterHelper.savePostParams((SRTServletRequest) request);
//    }
//
//    public void restorePostParameters(HttpServletRequest request) {
//        PostParameterHelper.restorePostParams((SRTServletRequest) request);
//    }

}
