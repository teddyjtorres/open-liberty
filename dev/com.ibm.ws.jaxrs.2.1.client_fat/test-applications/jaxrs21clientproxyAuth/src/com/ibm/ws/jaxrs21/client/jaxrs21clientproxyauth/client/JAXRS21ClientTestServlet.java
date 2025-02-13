/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.client.jaxrs21clientproxyauth.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configurable;

/**
 *
 */
@WebServlet("/JAXRS21ClientTestServlet")
public class JAXRS21ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646596L;

    public static enum ConfigurableToTest {
        CLIENT_BUILDER, CLIENT, WEB_TARGET, BUILDER
    }

    private static final String moduleName = "jaxrs21clientproxyAuth";

    private static void log(String method, String msg) {
        System.out.println("JAXRS21ClientTestServlet." + method + ": " + msg);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        log("doGet", "<entry>");
        PrintWriter pw = resp.getWriter();

        String testMethod = req.getParameter("test");
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }

        runTest(testMethod, pw, req, resp);
        log("doGet", "<exit>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        this.doGet(req, resp);
    }

    private void runTest(String testMethod, PrintWriter pw, HttpServletRequest req, HttpServletResponse resp) {
        try {
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator<String> itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
                if (key.indexOf("@") == 0) {
                    if ("@proxypassword".equals(key)) {
                        m.put(key.substring(1), "myPa$$word");
                    } else {
                        m.put(key.substring(1), req.getParameter(key));
                    }
                }
            }

            m.put("serverIP", req.getLocalAddr());
            m.put("serverPort", String.valueOf(req.getLocalPort()));

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(pw);
        }
    }

    public void testProxyToHTTP_ClientBuilder(Map<String, String> param, StringBuilder ret) {
        testProxy("http", param, ret, ConfigurableToTest.CLIENT_BUILDER);
    }

    public void testProxyToHTTPS_ClientBuilder(Map<String, String> param, StringBuilder ret) {
        testProxy("https", param, ret, ConfigurableToTest.CLIENT_BUILDER);
    }

    public void testProxyToHTTPTimeout_ClientBuilder(Map<String, String> param, StringBuilder ret) {
        testProxyTimeout("http", param, ret, ConfigurableToTest.CLIENT_BUILDER);
    }

    public void testProxyToHTTP_Client(Map<String, String> param, StringBuilder ret) {
        testProxy("http", param, ret, ConfigurableToTest.CLIENT);
    }

    public void testProxyToHTTPS_Client(Map<String, String> param, StringBuilder ret) {
        testProxy("https", param, ret, ConfigurableToTest.CLIENT);
    }

    public void testProxyToHTTPTimeout_Client(Map<String, String> param, StringBuilder ret) {
        testProxyTimeout("http", param, ret, ConfigurableToTest.CLIENT);
    }

    public void testProxyToHTTP_WebTarget(Map<String, String> param, StringBuilder ret) {
        testProxy("http", param, ret, ConfigurableToTest.WEB_TARGET);
    }

    public void testProxyToHTTPS_WebTarget(Map<String, String> param, StringBuilder ret) {
        testProxy("https", param, ret, ConfigurableToTest.WEB_TARGET);
    }

    public void testProxyToHTTPTimeout_WebTarget(Map<String, String> param, StringBuilder ret) {
        testProxyTimeout("http", param, ret, ConfigurableToTest.WEB_TARGET);
    }

    public void testProxyToHTTP_Builder(Map<String, String> param, StringBuilder ret) {
        testProxy("http", param, ret, ConfigurableToTest.BUILDER);
    }

    public void testProxyToHTTPS_Builder(Map<String, String> param, StringBuilder ret) {
        testProxy("https", param, ret, ConfigurableToTest.BUILDER);
    }

    public void testProxyToHTTPTimeout_Builder(Map<String, String> param, StringBuilder ret) {
        testProxyTimeout("http", param, ret, ConfigurableToTest.BUILDER);
    }

    private void testProxy(String protocol, Map<String, String> param, StringBuilder ret, ConfigurableToTest configurable) {

        log("testProxy", "<entry>");
        String serverIP = param.get("serverIP");

        String serverPort = "https".equals(protocol) ? param.get("secPort") : param.get("serverPort");

        String host = param.get("proxyhost");
        String port = param.get("proxyport");
        String username = param.get("proxyusername");
        String password = param.get("proxypassword");
        String type = param.get("proxytype");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testProxy: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        if (ConfigurableToTest.CLIENT_BUILDER.equals(configurable)) {
            configProperties(cb, null, host, port, type, username, password);
        }

        cb.hostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        Client c = cb.build();
        if (ConfigurableToTest.CLIENT.equals(configurable)) {
            configProperties(c, null, host, port, type, username, password);
        }
        String res = null;
        try {
            String targetURL = protocol + "://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ProxyAuthClientTest/BasicResource";
            log("testProxy", "Attempting to target: " + targetURL);
            WebTarget target = c.target(targetURL);
            if (ConfigurableToTest.WEB_TARGET.equals(configurable)) {
                configProperties(target, null, host, port, type, username, password);
            }
            Builder builder = target.path("echo").path(param.get("param")).request();
            // yuck - Invocation.Builder has a property method, but doesn't implement Configurable...
            if (ConfigurableToTest.BUILDER.equals(configurable)) {
                builder.property("com.ibm.ws.jaxrs.client.proxy.host", host);
                builder.property("com.ibm.ws.jaxrs.client.proxy.port", port);
                builder.property("com.ibm.ws.jaxrs.client.proxy.type", type);
                builder.property("com.ibm.ws.jaxrs.client.proxy.username", username);
                builder.property("com.ibm.ws.jaxrs.client.proxy.password", password);
            }
            res = builder.rx().get(String.class).toCompletableFuture().get();
        } catch (Exception e) {
            // java.util.concurrent.ExecutionException is a wrapper exception. Any exception thrown by the JAX-RS
            // runtime is caught and wrapped by this exception.
            // You can obtain any exception thrown by the JAX-RS runtime when an asynchronous request is executed by
            // calling the ExecutionException.getCause() method. The possible wrapped JAX-RS exceptions are the same as
            // the synchronous ones
            if (e.toString().contains("ExecutionException")) {
                res = "[Proxy Error]:" + e.getCause().toString();
            } else {
                res = "[Proxy Error]:" + e.toString();
            }
        } finally {
            c.close();
            ret.append(res);
        }
        log("testProxy", "<exit> " + res);
    }

    private void testProxyTimeout(String protocol, Map<String, String> param, StringBuilder ret, ConfigurableToTest configurable) {

        log("testProxyTimeout", "<entry>");
        String serverIP = param.get("serverIP");

        String serverPort = "https".equals(protocol) ? param.get("secPort") : param.get("serverPort");

        String host = param.get("proxyhost");
        String port = param.get("proxyport");
        String username = param.get("proxyusername");
        String password = param.get("proxypassword");
        String type = param.get("proxytype");
        String timeout = param.get("timeout");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testProxyTimeout: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        cb.hostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        if (ConfigurableToTest.CLIENT_BUILDER.equals(configurable)) {
            configProperties(cb, timeout, host, port, type, username, password);
        }

        Client c = cb.build();
        if (ConfigurableToTest.CLIENT.equals(configurable)) {
            configProperties(c, timeout, host, port, type, username, password);
        }

        String res = null;
        try {
            String targetURL = protocol + "://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ProxyAuthClientTest/BasicResource";
            log("testProxyTimeout", "Attempting to target: " + targetURL);
            WebTarget target = c.target(targetURL);
            if (ConfigurableToTest.WEB_TARGET.equals(configurable)) {
                configProperties(target, timeout, host, port, type, username, password);
            }
            Builder builder = target.path("echo").path(param.get("param")).request();
            if (ConfigurableToTest.BUILDER.equals(configurable)) {
                builder.property("com.ibm.ws.jaxrs.client.connection.timeout", timeout);
                builder.property("com.ibm.ws.jaxrs.client.receive.timeout", timeout);
                builder.property("com.ibm.ws.jaxrs.client.proxy.host", host);
                builder.property("com.ibm.ws.jaxrs.client.proxy.port", port);
                builder.property("com.ibm.ws.jaxrs.client.proxy.type", type);
                builder.property("com.ibm.ws.jaxrs.client.proxy.username", username);
                builder.property("com.ibm.ws.jaxrs.client.proxy.password", password);
            }
            res = builder.rx().get(String.class).toCompletableFuture().get();
        } catch (Exception e) {
            // java.util.concurrent.ExecutionException is a wrapper exception. Any exception thrown by the JAX-RS
            // runtime is caught and wrapped by this exception.
            // You can obtain any exception thrown by the JAX-RS runtime when an asynchronous request is executed by
            // calling the ExecutionException.getCause() method. The possible wrapped JAX-RS exceptions are the same as
            // the synchronous ones
            if (e.toString().contains("ExecutionException")) {
                res = "[Proxy Error]:" + e.getCause().toString();
            } else {
                res = "[Proxy Error]:" + e.toString();
            }
        } finally {
            c.close();
            ret.append(res);
        }
        log("testProxyTimeout", "<exit> " + res);
    }

    private void configProperties(Configurable<?> configurable, String timeout, String host, String port, String type, String username, String password) {
        if (timeout != null) {
            configurable.property("com.ibm.ws.jaxrs.client.connection.timeout", timeout);
            configurable.property("com.ibm.ws.jaxrs.client.receive.timeout", timeout);
        }
        if (host != null) {
            configurable.property("com.ibm.ws.jaxrs.client.proxy.host", host);
        }
        if (port != null) {
            configurable.property("com.ibm.ws.jaxrs.client.proxy.port", port);
        }
        if (type != null) {
            configurable.property("com.ibm.ws.jaxrs.client.proxy.type", type);
        }
        if (username != null) {
            configurable.property("com.ibm.ws.jaxrs.client.proxy.username", username);
        }
        if (password != null) {
            configurable.property("com.ibm.ws.jaxrs.client.proxy.password", password);
        }
    }
}
