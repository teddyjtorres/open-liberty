/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.impl.netty;

import java.util.EventListener;

/**
 * Sip specific listener interface for receiving events back from 
 * a resolver
 * <p>
 * A class which implements this interface is passed with a
 * {@link SipResolverMsg} which represents a request, so that 
 * a resolver knows who to pass an asynchronous response back to
 */
public interface SipResolverListener extends EventListener {
   
	/**
	 * Method to receive callback for a {@link SipResolverEvent}
	 * 
	 * @param event {@link SipResolverEvent} representing a response to 
	 * 				a request
	 */
	public void handleSipResolverEvent(SipResolverEvent event);
  
}
