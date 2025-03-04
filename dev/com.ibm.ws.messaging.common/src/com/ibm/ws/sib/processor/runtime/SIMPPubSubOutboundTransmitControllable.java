/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.sib.processor.runtime;

import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;

/**
 * A transmit stream set for pub sub messaging 
 */
public interface SIMPPubSubOutboundTransmitControllable extends SIMPDeliveryStreamSetTransmitControllable
{
  /**
   * Provides an iterator over all of the tick ranges in all of the streams in the set. 
   *  The messages are listed for each stream in turn and in the order that they are
   *  saved in the stream starting with the oldest message at the head of the stream.
   *
   * @return Iterator  iterator over the messages in the stream. The iterator contains a set of 
   *         DeliveryStreamTickRange objects in ascending tick order starting with the oldest
   *         for each class of service and priority. All ticks for one class of service and priority
   *         are returned before the next class of service and priority. 
   */
  SIMPIterator getTransmitMessagesIterator(int maxMsgs)
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException;
  
  /**
   * Returns the message on the outbound transmission stream with the 
   * given id.
   * @param id
   * @return the transmit message
   */
  SIMPTransmitMessageControllable getTransmitMessageByID(String id)
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException;
    
  
  /**
   * @return a SIMPIterator containing SIMPRemoteConsumerTransmitControllable
   * for each of the remote attatched subscribers
   * @author tpm
   */
  SIMPIterator getAttatchedRemoteSubscribers();
}
