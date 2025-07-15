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
package org.exoplatform.commons.persistence.impl;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.SessionFactoryImpl;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.api.persistence.GenericDAO;

public class GenericDAOJPAImpl<E, I extends Serializable> implements GenericDAO<E, I> {

  protected Class<E> modelClass;

  @SuppressWarnings("unchecked")
  public GenericDAOJPAImpl() {
    ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
    this.modelClass = (Class<E>) genericSuperclass.getActualTypeArguments()[0];
  }

  @Override
  public Long count() {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<Long> query = cb.createQuery(Long.class);

    Root<E> entity = query.from(modelClass);

    //Selecting the count
    query.select(cb.count(entity));

    return getEntityManager().createQuery(query).getSingleResult();
  }

  @Override
  public E find(I id) {
    return getEntityManager().find(modelClass, id);
  }

  /**
   * This method makes 2 calls to getEntityManager():
   * 1- The first one to get the CriteriaBuilder
   * 2- The second one to create the query
   * If there is no EntityManager in the threadLocal (i.e: EntityManagerService.getEntityManager() returns null),
   * the EntityManagerHolder will return 2 distinct EntityManager instances.
   * This will result in a org.hibernate.SessionException: Session is closed!.
   *
   * Thus, this method shall always be invoked with an EntityManager in the ThreadLocal
   * (for example, from a request managed by the portal lifecycle or from a method annotated with  @ExoTransactional)
   */
  //Another option is to implement something similar to Spring's DeferredQueryInvocationHandler
  @Override
  public List<E> findAll() {
    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<E> query = cb.createQuery(modelClass);

    Root<E> entity = query.from(modelClass);

    //Selecting the entity
    query.select(entity);

    return getEntityManager().createQuery(query).getResultList();
  }

  @Override
  @ExoTransactional
  public E create(E entity) {
    EntityManager em = getEntityManager();
    em.persist(entity);
    return entity;
  }

  @Override
  @ExoTransactional
  public void createAll(List<E> entities) {
    EntityManager em = getEntityManager();
    for (E entity : entities) {
      em.persist(entity);
    }
  }

  @Override
  @ExoTransactional
  public E update(E entity) {
    getEntityManager().merge(entity);
    return entity;
  }

  @Override
  @ExoTransactional
  public void updateAll(List<E> entities) {
    for (E entity : entities) {
      getEntityManager().merge(entity);
    }
  }

  @Override
  @ExoTransactional
  public void delete(E entity) {
    EntityManager em = getEntityManager();
    em.remove(em.merge(entity));
  }

  @Override
  @ExoTransactional
  public void deleteAll(List<E> entities) {
    EntityManager em = getEntityManager();
    for (E entity : entities) {
      em.remove(entity);
    }
  }

  @Override
  @ExoTransactional
  public void deleteAll() {
    List<E> entities = findAll();

    EntityManager em = getEntityManager();
    for (E entity : entities) {
      em.remove(entity);
    }
  }

  /**
   * Return an EntityManager instance.
   * @return An EntityManger instance.
   */
  protected EntityManager getEntityManager() {
    return EntityManagerHolder.get();
  }

  protected Dialect getHibernateDialect() {
    final Session session = (Session) getEntityManager().getDelegate();
    final SessionFactoryImpl sessionFactory = (SessionFactoryImpl) session.getSessionFactory();
    return sessionFactory.getJdbcServices().getDialect();
  }

  protected boolean isMSSQLDialect() {
    Dialect hibernateDialect = getHibernateDialect();
    return hibernateDialect != null && (StringUtils.contains(hibernateDialect.getClass().getName(), "MsSQL")
        || StringUtils.contains(hibernateDialect.getClass().getName(), "SQLServer"));
  }

  protected boolean isOrcaleDialect() {
    Dialect hibernateDialect = getHibernateDialect();
    return hibernateDialect != null && StringUtils.contains(hibernateDialect.getClass().getName(), "Oracle");
  }
}

