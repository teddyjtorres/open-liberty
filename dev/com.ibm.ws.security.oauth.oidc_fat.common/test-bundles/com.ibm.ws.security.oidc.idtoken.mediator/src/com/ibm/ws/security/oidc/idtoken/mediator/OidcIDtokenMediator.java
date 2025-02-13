/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
package com.ibm.ws.security.oidc.idtoken.mediator;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.wsspi.security.openidconnect.IDTokenMediator;

public class OidcIDtokenMediator implements IDTokenMediator {
    private final Properties properties = new Properties();
    static final String client_id = "client_id";
    static String resolverId = null;
    
    
    String userName = "spi_resolved_user";
    List<String> groups = new ArrayList<String>();
    String realm = "_realm";
    String uniqueId;
    JSONObject json = null;
    
    public OidcIDtokenMediator(Dictionary<String, Object> serviceProps) {
        saveDictionary(serviceProps);
    }
    
    static Subject getRunAsSubjectInternal() throws Exception {
        Subject sub = null;
        try {
            sub = (Subject) AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() throws Exception
                        {
                            return WSSubject.getRunAsSubject();

                        }
                    });
        } catch (PrivilegedActionException e) {
            throw new Exception(e.getCause());
        }
        return sub;

    }
    
    @Override
    public String mediateToken(Map<String, String[]> tokenMap) {
  	  // 238871 check if subject has been put on thread
    	String principal = "null";
        try {
			Subject sub = getRunAsSubjectInternal();
			if (sub == null ) { 
				System.out.println("CHECKSUBJECT: Subject on thread is null");			
			} else {
				if(sub.getPrincipals().isEmpty()){
					System.out.println("CHECKSUBJECT: Subject is on thread, there are no principals") ;
				} else {
					String user = sub.getPrincipals().iterator().next().getName();
					System.out.println("CHECKSUBJECT: Subject is on thread, there is at least one principal with name: "+ user);
					principal = user;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
    	
	   String json = "{\"custom_idt_claim_one\":\"idtoken_spi_user\",\"custom_idt_claim_two\":\"id_token_spi_group\",\"custom_idt_claim_three\":\"id_token_spi_realm\",\"principal\":\"$TBD\"}";
	   json=json.replace("$TBD", principal);
	   System.out.println("returning JSON: " + json);
	   return json; 	
    	 	
    }

    
    void saveDictionary(Dictionary<String, ?> original) {
        Enumeration<String> keys = original.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            properties.put(key, original.get(key));
        }
        resolverId = (String) original.get("id");
    }

   }
