/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.jpa.container.v21;

import java.util.List;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;

import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.jpa.management.JPAEntityManager;
import com.ibm.ws.jpa.management.JPATxEmInvocation;

public class JPATxEmInvocationV21 extends JPATxEmInvocation {
    protected JPATxEmInvocationV21(UOWCoordinator uowCoord, EntityManager em, JPAEntityManager jpaEm, boolean txIsUnsynchronized) {
        super(uowCoord, em, jpaEm, txIsUnsynchronized);
        this.ivPoolEM = false;
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
        return ivEm.createEntityGraph(arg0);
    }

    @Override
    public EntityGraph<?> createEntityGraph(String arg0) {
        return ivEm.createEntityGraph(arg0);
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
        return ivEm.createNamedStoredProcedureQuery(arg0);
    }

    @Override
    public Query createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate arg0) {
        return ivEm.createQuery(arg0);
    }

    @Override
    public Query createQuery(@SuppressWarnings("rawtypes") CriteriaDelete arg0) {
        return ivEm.createQuery(arg0);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
        return ivEm.createStoredProcedureQuery(arg0);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0, @SuppressWarnings("rawtypes") Class... arg1) {
        return ivEm.createStoredProcedureQuery(arg0, arg1);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0, String... arg1) {
        return ivEm.createStoredProcedureQuery(arg0, arg1);
    }

    @Override
    public EntityGraph<?> getEntityGraph(String arg0) {
        return ivEm.getEntityGraph(arg0);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
        return ivEm.getEntityGraphs(arg0);
    }

    @Override
    public boolean isJoinedToTransaction() {
        return ivEm.isJoinedToTransaction();
    }
}
