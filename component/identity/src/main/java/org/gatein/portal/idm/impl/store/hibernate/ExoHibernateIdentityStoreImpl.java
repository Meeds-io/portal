/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.gatein.portal.idm.impl.store.hibernate;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gatein.portal.idm.IdentityStoreSource;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.api.PasswordCredential;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObject;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectCredential;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectCredentialType;
import org.picketlink.idm.impl.store.hibernate.PatchedHibernateIdentityStoreImpl;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectCredentialType;
import org.picketlink.idm.spi.store.IdentityStore;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;


/**
 * extends the class HibernateIdentityStoreImpl from PicketLink Idm to benefit from its methods in the implementation of isFirstlyCreatedIn() method declared in the IdentityStoreSource interface
 *in order to customize the Hibernate store
 */
public class ExoHibernateIdentityStoreImpl extends PatchedHibernateIdentityStoreImpl implements IdentityStore, IdentityStoreSource, Serializable {

    private static Logger log = Logger.getLogger(ExoHibernateIdentityStoreImpl.class.getName());

    public static final String DEFAULT_REALM_NAME = ExoHibernateIdentityStoreImpl.class.getName() + ".DEFAULT_REALM";

    private boolean isRealmAware = false;

    public ExoHibernateIdentityStoreImpl(String id) {
        super(id);
    }

    /** Add this method from the HibernateIdentityStoreImpl class because it has a private access in its original class
     *
     * @param ctx
     * @return DEFAULT_REALM_NAME (String) used in the implementation of getHibernateIdentityObject() method
     */
    private String getRealmName(IdentityStoreInvocationContext ctx) {
        if (isRealmAware()) {
            return ctx.getRealmId();
        } else {
            return DEFAULT_REALM_NAME;
        }
    }

    /** Add this method from the HibernateIdentityStoreImpl class because it has a private access in its original class
     * used in the implementation of getRealmName() method
     * @return
     */
    private boolean isRealmAware() {
        return isRealmAware;
    }

    /** Add this method from the HibernateIdentityStoreImpl class because and it has a private access in its original class
     *  used in the implementation of safeGet() method
     * @param io
     */
    private void checkIOInstance(IdentityObject io) {
        if (io == null) {
            throw new IllegalArgumentException("IdentityObject is null");
        }

    }

    /** Add this method from the HibernateIdentityStoreImpl class because it has a private access in its original class
     * @param ctx
     * @param credentialType
     * @return HibernateIdentityObjectCredentialType used in the implementation of isFirstlyCreatedIn() method
     * @throws IdentityException
     */
    private HibernateIdentityObjectCredentialType getHibernateIdentityObjectCredentialType(IdentityStoreInvocationContext ctx, IdentityObjectCredentialType credentialType) throws IdentityException {
        Session session = getHibernateSession(ctx);

        HibernateIdentityObjectCredentialType hibernateType = null;

        try {
          hibernateType = (HibernateIdentityObjectCredentialType) session.createQuery("SELECT ct FROM HibernateIdentityObjectCredentialType ct WHERE ct.name = :name")
                                                                         .setParameter("name", credentialType.getName())
                                                                         .uniqueResult();
        } catch (HibernateException e) {
            if (log.isLoggable(Level.FINER)) {
                log.log(Level.FINER, "Exception occurred: ", e);
            }

            throw new IdentityException("IdentityObjectCredentialType[ " + credentialType.getName() + "] not present in the store.");
        }

        return hibernateType;

    }

    /** Add this method from the HibernateIdentityStoreImpl class because it has a private access in its original class
     * @param ctx
     * @param io
     * @return HibernateIdentityObject used in the implementation of isFirstlyCreatedIn() method
     * @throws IdentityException
     */
    private HibernateIdentityObject safeGet(IdentityStoreInvocationContext ctx, IdentityObject io) throws IdentityException
    {
        checkIOInstance(io);

        if (io instanceof HibernateIdentityObject)
        {
            return (HibernateIdentityObject)io;
        }

        return getHibernateIdentityObject(ctx, io);

    }

    /**
     * Add this method from the HibernateIdentityStoreImpl class because it has a private access
     * @param ctx
     * @param io
     * @return  HibernateIdentityObject used in the implementation of safeGet() method
     * @throws IdentityException
     */
    private HibernateIdentityObject getHibernateIdentityObject(IdentityStoreInvocationContext ctx, IdentityObject io) throws IdentityException {
      Session hibernateSession = getHibernateSession(ctx);
      try {
        return hibernateSession.createNamedQuery("HibernateIdentityObject.findIdentityObjectByNameAndType", HibernateIdentityObject.class)
                .setParameter("name", io.getName().toLowerCase())
                .setParameter("typeName", io.getIdentityType().getName())
                .setParameter("realmName", getRealmName(ctx))
                .uniqueResult();
      } catch (Exception e) {
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "Exception occurred: ", e);
        }
        throw new IdentityException("IdentityObject[ " + io.getName() + " | " + io.getIdentityType().getName() + "] not present in the store.", e);
      }
    }

    /** Add the implementation of the method isFirstlyCreatedIn()
     *       declared in the interface IdentityStoreSource
     * @param ctx
     * @param identityObject
     * @return
     * @throws Exception
     */
    public boolean isFirstlyCreatedIn(IdentityStoreInvocationContext ctx, IdentityObject identityObject) throws Exception {
        HibernateIdentityObject hibernateObject = safeGet(ctx, identityObject);
        if (hibernateObject != null) {
            if (getSupportedFeatures().isCredentialSupported(hibernateObject.getIdentityType(), PasswordCredential.TYPE)) {
                HibernateIdentityObjectCredentialType hibernateCredentialType = getHibernateIdentityObjectCredentialType(ctx, PasswordCredential.TYPE);

                if (hibernateCredentialType == null) {
                    throw new IllegalStateException("Credential type not present in this store: " + PasswordCredential.TYPE.getName());
                }

                HibernateIdentityObjectCredential hibernateCredential = hibernateObject.getCredential(PasswordCredential.TYPE);

                return (hibernateCredential != null);
            }
        }
        return false;
    }

}