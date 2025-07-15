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
package org.exoplatform.services.organization.idm;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.Property;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class IdmHibernateService implements ComponentRequestLifecycle {

  private static final Log          LOG          = ExoLogger.getLogger(IdmHibernateService.class);

  public static final String        AUTO_DIALECT = "AUTO";

  private ThreadLocal<Session>      threadLocal_;

  private IdmHibernateConfiguration conf_;

  private SessionFactory            sessionFactory_;

  public IdmHibernateService(InitParams initParams) {
    threadLocal_ = new ThreadLocal<Session>();
    PropertiesParam param = initParams.getPropertiesParam("hibernate.properties");
    conf_ = new IdmHibernateConfiguration();
    Iterator<?> properties = param.getPropertyIterator();
    while (properties.hasNext()) {
      Property p = (Property) properties.next();
      conf_.setProperty(p.getName(), p.getValue());
    }

    // Replace the potential "java.io.tmpdir" variable in the connection URL
    String connectionURL = conf_.getProperty("hibernate.connection.url");
    if (connectionURL != null) {
      connectionURL = connectionURL.replace("${java.io.tmpdir}", System.getProperty("java.io.tmpdir"));
      conf_.setProperty("hibernate.connection.url", connectionURL);
    }
  }

  public Configuration getHibernateConfiguration() {
    return conf_;
  }

  /**
   * @return the SessionFactory
   */
  public SessionFactory getSessionFactory() {
    if (sessionFactory_ == null) {
      sessionFactory_ = conf_.buildSessionFactory();
    }
    return sessionFactory_;
  }

  public Session openSession() {
    Session currentSession = threadLocal_.get();
    if (currentSession == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("open new hibernate session in openSession()");
      }
      currentSession = getSessionFactory().openSession();
      threadLocal_.set(currentSession);
    }
    return currentSession;
  }

  public Session openNewSession() {
    Session currentSession = threadLocal_.get();
    if (currentSession != null) {
      closeSession(currentSession);
    }
    currentSession = getSessionFactory().openSession();
    threadLocal_.set(currentSession);
    return currentSession;
  }

  public void closeSession(Session session) {
    if (session == null) {
      return;
    }
    try {
      session.close();
      if (LOG.isDebugEnabled()) {
        LOG.debug("close hibernate session in openSession(Session session)");
      }
    } catch (HibernateException t) {
      LOG.error("Error closing hibernate session : " + t.getMessage(), t);
    }
    threadLocal_.remove();
  }

  final public void closeSession() {
    Session s = threadLocal_.get();
    if (s != null) {
      s.close();
    }
    threadLocal_.remove();
  }

  public Object findExactOne(Session session, String query, String id) throws Exception {
    Object res = session.createQuery(query).setParameter("id", id).uniqueResult();
    if (res == null) {
      throw new ObjectNotFoundException("Cannot find the object with id: " + id);
    }
    return res;
  }

  public Object findOne(Session session, String query, String id) throws Exception {
    List<?> l = session.createQuery(query).setParameter("id", id).list();
    if (l.size() == 0) {
      return null;
    } else if (l.size() > 1) {
      throw new Exception("Expect only one object but found" + l.size());
    } else {
      return l.get(0);
    }
  }

  public Collection<?> findAll(Session session, String query) throws Exception {
    List<?> l = session.createQuery(query).list();
    if (l.size() == 0) {
      return null;
    } else {
      return l;
    }
  }

  @SuppressWarnings("rawtypes")
  public Object findOne(Class clazz, Serializable id) throws Exception {
    Session session = openSession();
    Object obj = session.get(clazz, id);
    return obj;
  }

  public Object create(Object obj) throws Exception {
    Session session = openSession();
    session.save(obj);
    session.flush();
    return obj;
  }

  public Object update(Object obj) throws Exception {
    Session session = openSession();
    session.update(obj);
    session.flush();
    return obj;
  }

  public Object save(Object obj) throws Exception {
    Session session = openSession();
    session.merge(obj);
    session.flush();
    return obj;
  }

  public Object remove(Object obj) throws Exception {
    Session session = openSession();
    session.delete(obj);
    session.flush();
    return obj;
  }

  @SuppressWarnings("rawtypes")
  public Object remove(Class clazz, Serializable id) throws Exception {
    Session session = openSession();
    Object obj = session.get(clazz, id);
    session.delete(obj);
    session.flush();
    return obj;
  }

  @SuppressWarnings("rawtypes")
  public Object remove(Session session, Class clazz, Serializable id) throws Exception {
    Object obj = session.get(clazz, id);
    session.delete(obj);
    return obj;
  }

  public void startRequest(ExoContainer container) {

  }

  public void endRequest(ExoContainer container) {
    closeSession();
  }

  @Override
  public boolean isStarted(ExoContainer container) {
    Session s = threadLocal_.get();
    return s != null && s.isOpen();
  }

}
