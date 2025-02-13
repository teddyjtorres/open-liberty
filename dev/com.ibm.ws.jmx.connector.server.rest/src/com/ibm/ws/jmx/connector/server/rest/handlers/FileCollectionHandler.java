/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
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
package com.ibm.ws.jmx.connector.server.rest.handlers;

import java.io.IOException;
import java.io.InputStream;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.FileTransferHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;

@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                       RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_ROUTING + "=true",
                       RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=" + APIConstants.JMX_CONNECTOR_API_ROOT_PATH,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_FILE_COLLECTION })
public class FileCollectionHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(FileCollectionHandler.class);

    //OSGi service
    private final String KEY_FILE_TRANSFER_HELPER = "fileTransferHelper";
    private transient FileTransferHelper fileTransferHelper;
    private final AtomicServiceReference<FileTransferHelper> fileTransferHelperRef = new AtomicServiceReference<FileTransferHelper>(KEY_FILE_TRANSFER_HELPER);

    @Activate
    protected void activate(ComponentContext context) {
        fileTransferHelperRef.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        fileTransferHelperRef.deactivate(context);
    }

    // FileTransferHelper
    @Reference(name = KEY_FILE_TRANSFER_HELPER, service = FileTransferHelper.class)
    protected void setFileTransferHelperRef(ServiceReference<FileTransferHelper> ref) {
        fileTransferHelperRef.setReference(ref);
    }

    protected void unsetFileTransferHelperRef(ServiceReference<FileTransferHelper> ref) {
        fileTransferHelperRef.unsetReference(ref);
    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (RESTHelper.isPostMethod(method)) {
            collectionAction(request, response);
        } else {
            throw new RESTHandlerMethodNotAllowedError("POST");
        }
    }

    /**
     * Expecting a set of collection actions.
     * 
     * Currently the only supported action is "delete", so expecting the following JSON payload:
     * {
     * "delete":
     * [
     * "C:/temp",
     * "C:/wlp",
     * "C:/workarea"
     * ]
     * }
     * So basically, a JSON object with a String array called "delete".
     * 
     * Supports direct server delete and single-routing delete (not multi-routing delete)
     */
    private void collectionAction(RESTRequest request, RESTResponse response) {
        RESTHelper.ensureConsumesJson(request);

        InputStream is = RESTHelper.getInputStream(request);
        try {
            JSONObject obj = (JSONObject) JSON.parse(is);
            JSONArray filesArray = (JSONArray) obj.get("delete");
            final int size = filesArray.size();
            if (size == 0) {
                //catch empty JSON cases
                return;
            }
            FileTransferHelper helper = getFileTransferHelper();

            final boolean isSingleRouting = RESTHelper.containsSingleRoutingContext(request);

            //Delete each file using existing our existing internal delete methods
            for (int i = 0; i < size; i++) {
                final String fileToDelete = (String) filesArray.get(i);

                if (isSingleRouting) {
                    helper.routedDeleteInternal(request, fileToDelete, true);
                } else {
                    helper.deleteInternal(fileToDelete, true);
                }
            }
        } catch (NullPointerException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }

    private synchronized FileTransferHelper getFileTransferHelper() {
        if (fileTransferHelper == null) {
            fileTransferHelper = fileTransferHelperRef != null ? fileTransferHelperRef.getService() : null;

            if (fileTransferHelper == null) {
                IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                               APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                               "OSGI_SERVICE_ERROR",
                                                                               new Object[] { "FileTransferHelper" },
                                                                               "CWWKX0122E: OSGi service is not available."));
                throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            }
        }

        return fileTransferHelper;
    }
}
