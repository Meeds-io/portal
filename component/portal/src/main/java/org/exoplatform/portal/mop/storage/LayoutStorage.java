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
package org.exoplatform.portal.mop.storage;

import static org.exoplatform.portal.mop.storage.utils.MOPUtils.parseJsonArray;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.parseJsonObject;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.serialize;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.toJSONString;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.unserialize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.config.StaleModelException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationBackgroundStyle;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.CloneApplicationState;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.ComponentEntity.TYPE;
import org.exoplatform.portal.jdbc.entity.ContainerEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity.AppType;
import org.exoplatform.portal.mop.dao.ContainerDAO;
import org.exoplatform.portal.mop.dao.PermissionDAO;
import org.exoplatform.portal.mop.dao.WindowDAO;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.BodyData;
import org.exoplatform.portal.pom.data.BodyType;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class LayoutStorage {

  private static final String TYPE_PROP     = MappedAttributes.TYPE.getName();

  private static final String ID_PROP       = MappedAttributes.ID.getName();

  private static final String CHILDREN_PROP = "children";

  private static final Log    LOG           = ExoLogger.getExoLogger(LayoutStorage.class);

  private WindowDAO           windowDAO;

  private ContainerDAO        containerDAO;

  private PermissionDAO       permissionDAO;

  public LayoutStorage(WindowDAO windowDAO,
                       ContainerDAO containerDAO,
                       PermissionDAO permissionDAO) {
    this.windowDAO = windowDAO;
    this.containerDAO = containerDAO;
    this.permissionDAO = permissionDAO;
  }

  public Portlet load(ApplicationState state) {
    if (state instanceof TransientApplicationState transientState) {
      Portlet prefs = transientState.getContentState();
      if (prefs == null) {
        return new Portlet();
      } else {
        return prefs;
      }
    }

    Long id;
    if (state instanceof CloneApplicationState cloneApplicationState) {
      id = Safe.parseLong(cloneApplicationState.getStorageId());
    } else {
      id = Safe.parseLong(((PersistentApplicationState) state).getStorageId());
    }
    WindowEntity window = findWindow(id);
    if (window != null) {
      byte[] customization = window.getCustomization();
      if (customization != null) {
        return (Portlet) unserialize(customization);
      } else {
        return new Portlet();
      }
    } else {
      return null;
    }
  }

  public ApplicationState save(ApplicationState state, Portlet preferences) {
    if (state instanceof TransientApplicationState) {
      throw new AssertionError("Does not make sense");
    }

    Long id;
    if (state instanceof CloneApplicationState cloneApplicationState) {
      id = Safe.parseLong(cloneApplicationState.getStorageId());
    } else {
      id = Safe.parseLong(((PersistentApplicationState) state).getStorageId());
    }
    WindowEntity window = findWindow(id);
    if (window != null) {
      if (preferences != null) {
        window.setCustomization(serialize(preferences));
      } else {
        window.setCustomization(null);
      }
      updateWindow(window);
    }

    return state;
  }

  public String getId(ApplicationState state) {
    if (state instanceof TransientApplicationState tstate) {
      return tstate.getContentId();
    }

    Long id;
    if (state instanceof PersistentApplicationState pstate) { // NOSONAR
      id = Safe.parseLong(pstate.getStorageId());
    } else if (state instanceof CloneApplicationState cstate) { // NOSONAR
      id = Safe.parseLong(cstate.getStorageId());
    } else {
      throw new IllegalStateException("Unrecognized Application state class type : " + state);
    }

    WindowEntity window = findWindow(id);
    if (window != null) {
      return window.getContentId();
    } else {
      return null;
    }

  }

  public ApplicationData getApplicationData(String applicationStorageId) {
    WindowEntity window = windowDAO.find(Safe.parseLong(applicationStorageId));
    if (window != null) {
      return buildWindow(window);
    } else {
      return null;
    }
  }

  public Application getApplicationModel(String applicationStorageId) {
    ApplicationData applicationData = getApplicationData(applicationStorageId);
    return new Application(applicationData);
  }

  public List<ComponentEntity> saveChildren(JSONArray pageBody, List<ComponentData> children) {
    cleanDeletedComponents(pageBody, children);
    return saveChildren(children);
  }

  public List<ComponentData> buildChildren(JSONArray jsonBody) {
    Map<Long, ContainerEntity> containers = getContainerEntities(jsonBody);
    Map<Long, WindowEntity> windows = getWindowEntities(jsonBody);
    return buildChildren(jsonBody, containers, windows);
  }

  public void deleteChildren(JSONArray children) {
    for (Object child : children) {
      JSONObject c = (JSONObject) child;
      Long id = Safe.parseLong(c.get(ID_PROP).toString());
      TYPE t = TYPE.valueOf(c.get(TYPE_PROP).toString());

      if (TYPE.CONTAINER.equals(t)) {
        JSONArray descendants = (JSONArray) c.get(CHILDREN_PROP);
        if (descendants != null) {
          deleteChildren(descendants);
        }

        ContainerEntity container = containerDAO.find(id);
        if (container != null) {
          JSONArray dashboardChilds = parseJsonArray(container.getContainerBody());
          deleteChildren(dashboardChilds);

          permissionDAO.deletePermissions(ContainerEntity.class.getName(), container.getId());
          containerDAO.delete(container);
        }
      } else if (TYPE.WINDOW.equals(t)) {
        WindowEntity window = findWindow(id);
        if (window != null) {
          permissionDAO.deletePermissions(WindowEntity.class.getName(), window.getId());
          deleteWindow(window);
        }
      } else {
        throw new IllegalArgumentException("Can't delete child with type: " + t);
      }
    }
  }

  public List<String> getPermissions(String objectType,
                                     Long objectId,
                                     org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE permisssionType) {
    return buildPermission(permissionDAO.getPermissions(objectType,
                                                        objectId,
                                                        permisssionType));
  }

  public List<PermissionEntity> savePermissions(String objectType,
                                                long objectId,
                                                PermissionEntity.TYPE type,
                                                List<String> permissions) {
    return permissionDAO.savePermissions(objectType,
                                         objectId,
                                         type,
                                         permissions);
  }

  public List<ComponentEntity> clone(String objectType, String pageBody) {
    List<ComponentEntity> results = new LinkedList<>();

    JSONArray children = parse(pageBody);

    for (Object child : children) {
      JSONObject c = (JSONObject) child;
      Long id = Safe.parseLong(c.get(ID_PROP).toString());
      TYPE type = TYPE.valueOf(c.get(TYPE_PROP).toString());

      switch (type) {
      case CONTAINER:
        ContainerEntity srcC = containerDAO.find(id);
        ContainerEntity dstC = clone(srcC);

        JSONArray descendants = parse(srcC.getContainerBody());
        if (CollectionUtils.isNotEmpty(descendants)) {
          dstC.setChildren(clone(objectType, srcC.getContainerBody()));
        } else {
          dstC.setChildren(clone(objectType, ((JSONArray) c.get(CHILDREN_PROP)).toJSONString()));
        }
        dstC.setContainerBody(((JSONArray) dstC.toJSON().get(CHILDREN_PROP)).toJSONString());

        containerDAO.create(dstC);
        clonePermissions(objectType, dstC.getId(), srcC.getId());
        results.add(dstC);
        break;
      case WINDOW:
        WindowEntity srcW = findWindow(id);
        WindowEntity dstW = clone(srcW);

        dstW = createWindow(dstW);
        clonePermissions(objectType, dstW.getId(), srcW.getId());
        results.add(dstW);
        break;
      default:
        throw new IllegalStateException("Can't handle type: " + type);
      }

    }
    return results;
  }

  public void clonePermissions(String objectType, long dstId, long srcId) {
    clonePermissions(objectType, dstId, srcId, PermissionEntity.TYPE.ACCESS);
    clonePermissions(objectType, dstId, srcId, PermissionEntity.TYPE.EDIT);
  }

  public void clonePermissions(String objectType, long dstId, long srcId, PermissionEntity.TYPE type) {
    List<String> permissions = getPermissions(objectType, srcId, type);
    if (!permissions.isEmpty()) {
      savePermissions(objectType, dstId, type, permissions);
    }
  }

  public void deletePermissions(String objectType, Long objectId) {
    permissionDAO.deletePermissions(objectType, objectId);
  }

  protected WindowEntity createWindow(WindowEntity dstW) {
    return windowDAO.create(dstW);
  }

  protected WindowEntity updateWindow(WindowEntity window) {
    return windowDAO.update(window);
  }

  protected WindowEntity findWindow(Long id) {
    return windowDAO.find(id);
  }

  protected void deleteWindow(WindowEntity window) {
    windowDAO.delete(window);
  }

  protected void deleteWindowById(Long id) {
    windowDAO.deleteById(id);
  }

  private WindowEntity clone(WindowEntity src) {
    WindowEntity dst = new WindowEntity();
    dst.setAppType(src.getAppType());
    dst.setContentId(src.getContentId());
    dst.setCustomization(src.getCustomization());
    dst.setDescription(src.getDescription());
    dst.setHeight(src.getHeight());
    dst.setIcon(src.getIcon());
    dst.setProperties(src.getProperties());
    dst.setShowApplicationMode(src.isShowApplicationMode());
    dst.setShowApplicationState(src.isShowApplicationState());
    dst.setShowInfoBar(src.isShowInfoBar());
    dst.setTheme(src.getTheme());
    dst.setTitle(src.getTitle());
    dst.setWidth(src.getWidth());

    return dst;
  }

  private JSONArray parse(String body) {
    JSONParser parser = new JSONParser();
    JSONArray children;
    try {
      children = (JSONArray) parser.parse(body);
      return children;
    } catch (ParseException e) {
      throw new IllegalStateException("Can't parse body: " + body);
    }
  }

  private ContainerEntity clone(ContainerEntity src) {
    ContainerEntity dst = new ContainerEntity();

    dst.setDescription(src.getDescription());
    dst.setFactoryId(src.getFactoryId());
    dst.setHeight(src.getHeight());
    dst.setIcon(src.getIcon());
    dst.setName(src.getName());
    dst.setProperties(src.getProperties());
    dst.setTemplate(src.getTemplate());
    dst.setTitle(src.getTitle());
    dst.setWidth(src.getWidth());
    dst.setContainerBody(src.getContainerBody());

    return dst;
  }

  private void cleanDeletedComponents(JSONArray body,
                                      List<ComponentData> children) {
    Set<Long> windowIds = new HashSet<>();
    filterBodyContainerIds(body, TYPE.WINDOW, windowIds);
    for (Long id : windowIds) {
      if (findById(id, TYPE.WINDOW, children) == null) {
        deleteWindowById(id);
      }
    }

    Set<Long> containerIds = new HashSet<>();
    filterBodyContainerIds(body, TYPE.CONTAINER, containerIds);
    for (Long id : containerIds) {
      if (findById(id, TYPE.CONTAINER, children) == null) {
        containerDAO.deleteById(id);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private ContainerEntity buildContainerEntity(ContainerEntity dst, ContainerData src) {
    if (dst == null) {
      dst = new ContainerEntity();
    }
    dst.setWebuiId(src.getId());
    dst.setDescription(src.getDescription());
    dst.setFactoryId(src.getFactoryId());
    dst.setHeight(src.getHeight());
    dst.setIcon(src.getIcon());
    dst.setName(src.getName());
    dst.setTemplate(src.getTemplate());
    dst.setTitle(src.getTitle());
    dst.setWidth(src.getWidth());

    JSONObject properties = new JSONObject();
    if (StringUtils.isNotBlank(src.getProfiles())) {
      properties.put(MappedAttributes.PROFILES.getName(), src.getProfiles());
    }
    if (StringUtils.isNotBlank(src.getCssClass())) {
      properties.put(MappedAttributes.CSS_CLASS.getName(), src.getCssClass());
    }
    ModelStyle cssStyle = src.getCssStyle();
    if (cssStyle != null) {
      mapStyleToProperties(cssStyle, properties);
    }
    ApplicationBackgroundStyle appCssStyle = src.getAppBackgroundStyle();
    if (appCssStyle != null) {
      mapAppStyleToProperties(appCssStyle, properties);
    }
    dst.setProperties(properties.toJSONString());
    return dst;
  }

  @SuppressWarnings("unchecked")
  private WindowEntity buildWindowEntity(WindowEntity dst, ApplicationData srcChild) {
    if (dst == null) {
      dst = new WindowEntity();
      dst.setAppType(AppType.PORTLET);

      ApplicationState state = srcChild.getState();
      if (state instanceof TransientApplicationState s) {
        dst.setContentId(s.getContentId());
        if (s.getContentState() != null) {
          dst.setCustomization(serialize(s.getContentState()));
        }
      } else {
        throw new IllegalStateException("Can't create new window");
      }
    }
    dst.setDescription(srcChild.getDescription());
    dst.setHeight(srcChild.getHeight());
    dst.setIcon(srcChild.getIcon());
    dst.setShowApplicationMode(srcChild.isShowApplicationMode());
    dst.setShowApplicationState(srcChild.isShowApplicationState());
    dst.setShowInfoBar(srcChild.isShowInfoBar());
    dst.setTheme(srcChild.getTheme());
    dst.setTitle(srcChild.getTitle());
    dst.setWidth(srcChild.getWidth());

    boolean hasCssClass = StringUtils.isNotBlank(srcChild.getCssClass());
    if (hasCssClass || srcChild.getCssStyle() != null) {
      JSONObject properties = srcChild.getProperties() == null ? new JSONObject() : new JSONObject(srcChild.getProperties());
      if (hasCssClass) {
        properties.put(MappedAttributes.CSS_CLASS.getName(), srcChild.getCssClass());
      }
      ModelStyle cssStyle = srcChild.getCssStyle();
      if (cssStyle != null) {
        mapStyleToProperties(cssStyle, properties);
      }
      dst.setProperties(properties.toJSONString());
    } else {
      dst.setProperties(toJSONString(srcChild.getProperties()));
    }
    return dst;
  }

  private ComponentData findById(Long id, ComponentEntity.TYPE type, List<ComponentData> children) {
    if (children != null) {
      for (ComponentData child : children) {
        if (id.equals(Safe.parseLong(child.getStorageId()))
            && ((type == TYPE.WINDOW && child instanceof ApplicationData)
                || (type == TYPE.CONTAINER && child instanceof ContainerData))) {

          return child;
        } else if (child instanceof ContainerData childContainer) {
          ComponentData result = findById(id, type, childContainer.getChildren());
          if (result != null) {
            return result;
          }
        }
      }
    }
    return null;
  }

  private List<String> buildPermission(List<PermissionEntity> permissions) {
    List<String> results = new ArrayList<>();
    if (permissions != null) {
      for (PermissionEntity per : permissions) {
        results.add(per.getPermission());
      }
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  private ApplicationData buildWindow(WindowEntity windowEntity) {
    PersistentApplicationState state = new PersistentApplicationState(String.valueOf(windowEntity.getId()));
    Map<String, String> properties = new HashMap<>();
    JSONObject jProp = parseJsonObject(windowEntity.getProperties());
    jProp.forEach((key, value) -> {
      if (key != null && value != null) {
        properties.put(key.toString(), value.toString());
      }
    });

    List<PermissionEntity> access = permissionDAO.getPermissions(WindowEntity.class.getName(),
                                                                 windowEntity.getId(),
                                                                 PermissionEntity.TYPE.ACCESS);

    JSONObject attrs = windowEntity.getProperties() == null ? null : parseJsonObject(windowEntity.getProperties());
    ModelStyle cssStyle = null;
    String cssClass = null;
    if (attrs != null) {
      cssStyle = mapPropertiesToStyle(attrs);
      if (attrs.containsKey(MappedAttributes.CSS_CLASS.getName())) {
        cssClass = (String) attrs.get(MappedAttributes.CSS_CLASS.getName());
      }
    }

    return new ApplicationData(String.valueOf(windowEntity.getId()),
                               null,
                               state,
                               String.valueOf(windowEntity.getId()),
                               windowEntity.getTitle(),
                               windowEntity.getIcon(),
                               windowEntity.getDescription(),
                               windowEntity.isShowInfoBar(),
                               windowEntity.isShowApplicationState(),
                               windowEntity.isShowApplicationMode(),
                               windowEntity.getTheme(),
                               windowEntity.getWidth(),
                               windowEntity.getHeight(),
                               cssClass,
                               cssStyle,
                               properties,
                               buildPermission(access));
  }

  private ContainerData buildContainer(ContainerEntity entity,
                                       JSONObject jsonComponent,
                                       JSONObject attrs,
                                       Map<Long, ContainerEntity> containers,
                                       Map<Long, WindowEntity> windows) {
    List<ComponentData> children = buildChildren((JSONArray) jsonComponent.get(CHILDREN_PROP),
                                                 containers,
                                                 windows);

    List<PermissionEntity> access = permissionDAO.getPermissions(ContainerEntity.class.getName(),
                                                                 entity.getId(),
                                                                 PermissionEntity.TYPE.ACCESS);

    String cssClass = null;
    String profiles = null;
    ModelStyle cssStyle = null;
    ApplicationBackgroundStyle appBackgroundStyle = null;
    if (attrs != null) {
      cssStyle = mapPropertiesToStyle(attrs);
      appBackgroundStyle = mapPropertiesToAppStyle(attrs);
      if (attrs.containsKey(MappedAttributes.CSS_CLASS.getName())) {
        cssClass = (String) attrs.get(MappedAttributes.CSS_CLASS.getName());
      }
      if (attrs.containsKey(MappedAttributes.PROFILES.getName())) {
        profiles = (String) attrs.get(MappedAttributes.PROFILES.getName());
      }
    }

    return new ContainerData(String.valueOf(entity.getId()),
                             entity.getWebuiId(),
                             entity.getName(),
                             entity.getIcon(),
                             entity.getTemplate(),
                             entity.getFactoryId(),
                             entity.getTitle(),
                             entity.getDescription(),
                             entity.getWidth(),
                             entity.getHeight(),
                             cssClass,
                             profiles,
                             cssStyle,
                             appBackgroundStyle,
                             buildPermission(access),
                             children);
  }

  private List<ComponentData> buildChildren(JSONArray jsonBody, // NOSONAR
                                            Map<Long, ContainerEntity> containers,
                                            Map<Long, WindowEntity> windows) {
    List<ComponentData> results = new LinkedList<>();

    if (jsonBody != null) {
      for (Object component : jsonBody) {
        JSONObject jsonComponent = (JSONObject) component;
        Long id = Safe.parseLong(jsonComponent.get(ID_PROP).toString());
        TYPE type = TYPE.valueOf(jsonComponent.get(TYPE_PROP).toString());

        if (type == TYPE.WINDOW) {
          WindowEntity windowEntity = windows.get(id);
          if (windowEntity != null) {
            results.add(buildWindow(windowEntity));
          }
        } else if (type == TYPE.CONTAINER) {
          ContainerEntity srcContainer = containerDAO.find(id);
          if (srcContainer != null) {
            JSONObject attrs = parseJsonObject(srcContainer.getProperties());
            String ctype = (String) attrs.get(MappedAttributes.TYPE.getName());
            if (BodyType.PAGE.name().equals(ctype)) {
              ModelStyle cssStyle = mapPropertiesToStyle(attrs);
              String cssClass = null;
              if (attrs != null) {
                cssStyle = mapPropertiesToStyle(attrs);
                if (attrs.containsKey(MappedAttributes.CSS_CLASS.getName())) {
                  cssClass = (String) attrs.get(MappedAttributes.CSS_CLASS.getName());
                }
              }
              BodyData body = new BodyData(String.valueOf(id),
                                           null,
                                           cssStyle,
                                           containers.get(id).getWidth(),
                                           containers.get(id).getHeight(),
                                           cssClass,
                                           BodyType.PAGE);
              results.add(body);
            } else {
              results.add(buildContainer(containers.get(id),
                                         jsonComponent,
                                         attrs,
                                         containers,
                                         windows));
            }
          }
        }
      }
    }

    return results;
  }

  private List<ComponentEntity> saveChildren(List<ComponentData> children) { // NOSONAR
    List<ComponentEntity> results = new LinkedList<>();

    for (ComponentData srcChild : children) { // NOSONAR
      Long srcChildId = Safe.parseLong(srcChild.getStorageId());

      ComponentEntity dstChild = null;
      if (srcChildId != null && srcChildId > 0) { // update
        if (srcChild instanceof ContainerData containerData) {
          dstChild = containerDAO.find(srcChildId);
          if (dstChild != null) {
            buildContainerEntity((ContainerEntity) dstChild, containerData);
            containerDAO.update((ContainerEntity) dstChild);
          }
        } else if (srcChild instanceof ApplicationData appData) { // NOSONAR
          dstChild = findWindow(srcChildId);
          if (dstChild != null) {
            dstChild = buildWindowEntity((WindowEntity) dstChild, appData);
            if (dstChild == null) {
              continue;
            }
            dstChild = updateWindow((WindowEntity) dstChild);
          }
        } else if (srcChild instanceof BodyData) {
          // nothing to update on body data
          dstChild = containerDAO.find(srcChildId);
        } else {
          throw new StaleModelException("this layout component type is not supported: " + srcChild);
        }
      }

      if (dstChild == null || dstChild.getId() == null) { // create new
        if (srcChild instanceof ContainerData srcChildContainer) { // NOSONAR
          dstChild = buildContainerEntity(null, srcChildContainer);
          dstChild = containerDAO.create((ContainerEntity) dstChild);
        } else if (srcChild instanceof ApplicationData srcChildApplication) { // NOSONAR
          dstChild = buildWindowEntity(null, srcChildApplication);
          if (dstChild == null) {
            continue;
          }
          dstChild = createWindow((WindowEntity) dstChild);
        } else if (srcChild instanceof BodyData srcChildBody) {
          dstChild = buildContainerEntity(srcChildBody);
          dstChild = containerDAO.create((ContainerEntity) dstChild);
        } else {
          throw new StaleModelException("Was not expecting child " + srcChild);
        }
        if (dstChild.getId() == null) {
          throw new IllegalStateException("Id of saved child wasn't found: " + dstChild.getType() + " / " + dstChild);
        }
      }

      savePermissions(dstChild.getId(), srcChild);

      if (srcChild instanceof ContainerData srcChildContainer) {
        List<ComponentEntity> descendants = saveChildren(srcChildContainer.getChildren());
        ((ContainerEntity) dstChild).setChildren(descendants);
      }

      results.add(dstChild);
    }
    return results;
  }

  private Map<Long, ContainerEntity> getContainerEntities(JSONArray jsonBody) {
    Set<Long> ids = new HashSet<>();
    filterBodyContainerIds(jsonBody, TYPE.CONTAINER, ids);
    List<ContainerEntity> entities = containerDAO.findByIds(new LinkedList<>(ids));

    Map<Long, ContainerEntity> results = new HashMap<>();
    for (ContainerEntity entity : entities) {
      results.put(entity.getId(), entity);
    }

    ids.removeAll(results.keySet());
    if (!ids.isEmpty()) {
      LOG.warn("Can't find Container with ids: {}", StringUtils.join(ids, ","));
    }

    return results;
  }

  @SuppressWarnings("unchecked")
  private ComponentEntity buildContainerEntity(BodyData bodyData) {
    ContainerEntity dst = new ContainerEntity();
    dst.setHeight(bodyData.getHeight());
    dst.setWidth(bodyData.getWidth());

    JSONObject properties = new JSONObject();
    properties.put(MappedAttributes.TYPE.getName(), bodyData.getType().name());
    if (StringUtils.isNotBlank(bodyData.getCssClass())) {
      properties.put(MappedAttributes.CSS_CLASS.getName(), bodyData.getCssClass());
    }
    ModelStyle cssStyle = bodyData.getCssStyle();
    if (cssStyle != null) {
      mapStyleToProperties(cssStyle, properties);
    }
    dst.setProperties(properties.toJSONString());
    return dst;
  }

  private Map<Long, WindowEntity> getWindowEntities(JSONArray jsonBody) {
    Set<Long> ids = new HashSet<>();
    filterBodyContainerIds(jsonBody, TYPE.WINDOW, ids);
    List<WindowEntity> entities = ids.stream()
                                     .map(this::findWindow)
                                     .filter(Objects::nonNull)
                                     .toList();

    Map<Long, WindowEntity> results = new HashMap<>();
    for (WindowEntity entity : entities) {
      results.put(entity.getId(), entity);
    }

    ids.removeAll(results.keySet());
    if (!ids.isEmpty()) {
      LOG.error("Can't find Window with ids: {}", StringUtils.join(ids, ","));
    }

    return results;
  }

  private Set<Long> filterBodyContainerIds(JSONArray jsonBody, TYPE type, Set<Long> ids) {
    if (jsonBody != null) {
      for (Object obj : jsonBody) {
        JSONObject component = (JSONObject) obj;
        TYPE componentType = TYPE.valueOf(component.get(TYPE_PROP).toString());
        if (componentType.equals(type)) {
          ids.add(Safe.parseLong(component.get(ID_PROP).toString()));
        }
        if (TYPE.CONTAINER.equals(componentType)) {
          filterBodyContainerIds((JSONArray) component.get(CHILDREN_PROP), type, ids);
        }
      }
    }
    return ids;
  }

  private void savePermissions(Long id, ComponentData srcChild) {
    if (id == null) {
      throw new IllegalArgumentException("id is null");
    }
    List<String> access = null;
    String typeName = srcChild.getClass().getName();
    if (srcChild instanceof ContainerData srcChildContainer) {
      typeName = ContainerEntity.class.getName();
      access = srcChildContainer.getAccessPermissions();
    } else if (srcChild instanceof ApplicationData srcChildApplication) { // NOSONAR
      typeName = WindowEntity.class.getName();
      access = srcChildApplication.getAccessPermissions();
    }

    savePermissions(typeName, id, PermissionEntity.TYPE.ACCESS, access);
  }

  private ModelStyle mapPropertiesToStyle(JSONObject attrs) { // NOSONAR
    ModelStyle cssStyle = new ModelStyle();
    if (attrs.containsKey(MappedAttributes.BORDER_COLOR.getName())) {
      cssStyle.setBorderColor((String) attrs.get(MappedAttributes.BORDER_COLOR.getName()));
    }
    if (attrs.containsKey(MappedAttributes.BACKGROUND_COLOR.getName())) {
      cssStyle.setBackgroundColor((String) attrs.get(MappedAttributes.BACKGROUND_COLOR.getName()));
    }
    if (attrs.containsKey(MappedAttributes.BACKGROUND_IMAGE.getName())) {
      cssStyle.setBackgroundImage((String) attrs.get(MappedAttributes.BACKGROUND_IMAGE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.BACKGROUND_EFFECT.getName())) {
      cssStyle.setBackgroundEffect((String) attrs.get(MappedAttributes.BACKGROUND_EFFECT.getName()));
    }
    if (attrs.containsKey(MappedAttributes.BACKGROUND_POSITION.getName())) {
      cssStyle.setBackgroundPosition((String) attrs.get(MappedAttributes.BACKGROUND_POSITION.getName()));
    }
    if (attrs.containsKey(MappedAttributes.BACKGROUND_SIZE.getName())) {
      cssStyle.setBackgroundSize((String) attrs.get(MappedAttributes.BACKGROUND_SIZE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.BACKGROUND_REPEAT.getName())) {
      cssStyle.setBackgroundRepeat((String) attrs.get(MappedAttributes.BACKGROUND_REPEAT.getName()));
    }
    if (attrs.containsKey(MappedAttributes.BORDER_SIZE.getName())) {
      cssStyle.setBorderSize((String) attrs.get(MappedAttributes.BORDER_SIZE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.BOX_SHADOW.getName())) {
      cssStyle.setBoxShadow((String) attrs.get(MappedAttributes.BOX_SHADOW.getName()));
    }
    if (attrs.containsKey(MappedAttributes.MARGIN_TOP.getName())) {
      cssStyle.setMarginTop(Integer.parseInt((String) attrs.get(MappedAttributes.MARGIN_TOP.getName())));
    }
    if (attrs.containsKey(MappedAttributes.MARGIN_BOTTOM.getName())) {
      cssStyle.setMarginBottom(Integer.parseInt((String) attrs.get(MappedAttributes.MARGIN_BOTTOM.getName())));
    }
    if (attrs.containsKey(MappedAttributes.MARGIN_RIGHT.getName())) {
      cssStyle.setMarginRight(Integer.parseInt((String) attrs.get(MappedAttributes.MARGIN_RIGHT.getName())));
    }
    if (attrs.containsKey(MappedAttributes.MARGIN_LEFT.getName())) {
      cssStyle.setMarginLeft(Integer.parseInt((String) attrs.get(MappedAttributes.MARGIN_LEFT.getName())));
    }
    if (attrs.containsKey(MappedAttributes.RADIUS_TOP_RIGHT_SHADOW.getName())) {
      cssStyle.setRadiusTopRight(Integer.parseInt((String) attrs.get(MappedAttributes.RADIUS_TOP_RIGHT_SHADOW.getName())));
    }
    if (attrs.containsKey(MappedAttributes.RADIUS_TOP_LEFT_SHADOW.getName())) {
      cssStyle.setRadiusTopLeft(Integer.parseInt((String) attrs.get(MappedAttributes.RADIUS_TOP_LEFT_SHADOW.getName())));
    }
    if (attrs.containsKey(MappedAttributes.RADIUS_BOTTOM_RIGHT_SHADOW.getName())) {
      cssStyle.setRadiusBottomRight(Integer.parseInt((String) attrs.get(MappedAttributes.RADIUS_BOTTOM_RIGHT_SHADOW.getName())));
    }
    if (attrs.containsKey(MappedAttributes.RADIUS_BOTTOM_LEFT_SHADOW.getName())) {
      cssStyle.setRadiusBottomLeft(Integer.parseInt((String) attrs.get(MappedAttributes.RADIUS_BOTTOM_LEFT_SHADOW.getName())));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_TITLE_COLOR.getName())) {
      cssStyle.setTextTitleColor((String) attrs.get(MappedAttributes.TEXT_TITLE_COLOR.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_TITLE_FONT_SIZE.getName())) {
      cssStyle.setTextTitleFontSize((String) attrs.get(MappedAttributes.TEXT_TITLE_FONT_SIZE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_TITLE_FONT_WEIGHT.getName())) {
      cssStyle.setTextTitleFontWeight((String) attrs.get(MappedAttributes.TEXT_TITLE_FONT_WEIGHT.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_TITLE_FONT_STYLE.getName())) {
      cssStyle.setTextTitleFontStyle((String) attrs.get(MappedAttributes.TEXT_TITLE_FONT_STYLE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_HEADER_COLOR.getName())) {
      cssStyle.setTextHeaderColor((String) attrs.get(MappedAttributes.TEXT_HEADER_COLOR.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_HEADER_FONT_SIZE.getName())) {
      cssStyle.setTextHeaderFontSize((String) attrs.get(MappedAttributes.TEXT_HEADER_FONT_SIZE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_HEADER_FONT_WEIGHT.getName())) {
      cssStyle.setTextHeaderFontWeight((String) attrs.get(MappedAttributes.TEXT_HEADER_FONT_WEIGHT.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_HEADER_FONT_STYLE.getName())) {
      cssStyle.setTextHeaderFontStyle((String) attrs.get(MappedAttributes.TEXT_HEADER_FONT_STYLE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_COLOR.getName())) {
      cssStyle.setTextColor((String) attrs.get(MappedAttributes.TEXT_COLOR.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_FONT_SIZE.getName())) {
      cssStyle.setTextFontSize((String) attrs.get(MappedAttributes.TEXT_FONT_SIZE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_FONT_WEIGHT.getName())) {
      cssStyle.setTextFontWeight((String) attrs.get(MappedAttributes.TEXT_FONT_WEIGHT.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_FONT_STYLE.getName())) {
      cssStyle.setTextFontStyle((String) attrs.get(MappedAttributes.TEXT_FONT_STYLE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_SUBTITLE_COLOR.getName())) {
      cssStyle.setTextSubtitleColor((String) attrs.get(MappedAttributes.TEXT_SUBTITLE_COLOR.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_SUBTITLE_FONT_SIZE.getName())) {
      cssStyle.setTextSubtitleFontSize((String) attrs.get(MappedAttributes.TEXT_SUBTITLE_FONT_SIZE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_SUBTITLE_FONT_WEIGHT.getName())) {
      cssStyle.setTextSubtitleFontWeight((String) attrs.get(MappedAttributes.TEXT_SUBTITLE_FONT_WEIGHT.getName()));
    }
    if (attrs.containsKey(MappedAttributes.TEXT_SUBTITLE_FONT_STYLE.getName())) {
      cssStyle.setTextSubtitleFontStyle((String) attrs.get(MappedAttributes.TEXT_SUBTITLE_FONT_STYLE.getName()));
    }
    return cssStyle;
  }

  private ApplicationBackgroundStyle mapPropertiesToAppStyle(JSONObject attrs) { // NOSONAR
    ApplicationBackgroundStyle cssStyle = new ApplicationBackgroundStyle();
    if (attrs.containsKey(MappedAttributes.APP_BACKGROUND_COLOR.getName())) {
      cssStyle.setBackgroundColor((String) attrs.get(MappedAttributes.APP_BACKGROUND_COLOR.getName()));
    }
    if (attrs.containsKey(MappedAttributes.APP_BACKGROUND_IMAGE.getName())) {
      cssStyle.setBackgroundImage((String) attrs.get(MappedAttributes.APP_BACKGROUND_IMAGE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.APP_BACKGROUND_EFFECT.getName())) {
      cssStyle.setBackgroundEffect((String) attrs.get(MappedAttributes.APP_BACKGROUND_EFFECT.getName()));
    }
    if (attrs.containsKey(MappedAttributes.APP_BACKGROUND_POSITION.getName())) {
      cssStyle.setBackgroundPosition((String) attrs.get(MappedAttributes.APP_BACKGROUND_POSITION.getName()));
    }
    if (attrs.containsKey(MappedAttributes.APP_BACKGROUND_SIZE.getName())) {
      cssStyle.setBackgroundSize((String) attrs.get(MappedAttributes.APP_BACKGROUND_SIZE.getName()));
    }
    if (attrs.containsKey(MappedAttributes.APP_BACKGROUND_REPEAT.getName())) {
      cssStyle.setBackgroundRepeat((String) attrs.get(MappedAttributes.APP_BACKGROUND_REPEAT.getName()));
    }
    return cssStyle;
  }

  @SuppressWarnings("unchecked")
  private void mapStyleToProperties(ModelStyle cssStyle, JSONObject properties) { // NOSONAR
    if (StringUtils.isNotBlank(cssStyle.getBorderColor())) {
      properties.put(MappedAttributes.BORDER_COLOR.getName(), cssStyle.getBorderColor());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundColor())) {
      properties.put(MappedAttributes.BACKGROUND_COLOR.getName(), cssStyle.getBackgroundColor());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundImage())) {
      properties.put(MappedAttributes.BACKGROUND_IMAGE.getName(), cssStyle.getBackgroundImage());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundEffect())) {
      properties.put(MappedAttributes.BACKGROUND_EFFECT.getName(), cssStyle.getBackgroundEffect());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundPosition())) {
      properties.put(MappedAttributes.BACKGROUND_POSITION.getName(), cssStyle.getBackgroundPosition());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundSize())) {
      properties.put(MappedAttributes.BACKGROUND_SIZE.getName(), cssStyle.getBackgroundSize());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundRepeat())) {
      properties.put(MappedAttributes.BACKGROUND_REPEAT.getName(), cssStyle.getBackgroundRepeat());
    }
    if (StringUtils.isNotBlank(cssStyle.getBorderSize())) {
      properties.put(MappedAttributes.BORDER_SIZE.getName(), cssStyle.getBorderSize());
    }
    if (StringUtils.isNotBlank(cssStyle.getBoxShadow())) {
      properties.put(MappedAttributes.BOX_SHADOW.getName(), cssStyle.getBoxShadow());
    }
    if (cssStyle.getMarginTop() != null) {
      properties.put(MappedAttributes.MARGIN_TOP.getName(), cssStyle.getMarginTop().toString());
    }
    if (cssStyle.getMarginBottom() != null) {
      properties.put(MappedAttributes.MARGIN_BOTTOM.getName(), cssStyle.getMarginBottom().toString());
    }
    if (cssStyle.getMarginRight() != null) {
      properties.put(MappedAttributes.MARGIN_RIGHT.getName(), cssStyle.getMarginRight().toString());
    }
    if (cssStyle.getMarginLeft() != null) {
      properties.put(MappedAttributes.MARGIN_LEFT.getName(), cssStyle.getMarginLeft().toString());
    }
    if (cssStyle.getRadiusTopRight() != null) {
      properties.put(MappedAttributes.RADIUS_TOP_RIGHT_SHADOW.getName(), cssStyle.getRadiusTopRight().toString());
    }
    if (cssStyle.getRadiusTopLeft() != null) {
      properties.put(MappedAttributes.RADIUS_TOP_LEFT_SHADOW.getName(), cssStyle.getRadiusTopLeft().toString());
    }
    if (cssStyle.getRadiusBottomRight() != null) {
      properties.put(MappedAttributes.RADIUS_BOTTOM_RIGHT_SHADOW.getName(), cssStyle.getRadiusBottomRight().toString());
    }
    if (cssStyle.getRadiusBottomLeft() != null) {
      properties.put(MappedAttributes.RADIUS_BOTTOM_LEFT_SHADOW.getName(), cssStyle.getRadiusBottomLeft().toString());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextTitleColor())) {
      properties.put(MappedAttributes.TEXT_TITLE_COLOR.getName(), cssStyle.getTextTitleColor());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextTitleFontSize())) {
      properties.put(MappedAttributes.TEXT_TITLE_FONT_SIZE.getName(), cssStyle.getTextTitleFontSize());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextTitleFontWeight())) {
      properties.put(MappedAttributes.TEXT_TITLE_FONT_WEIGHT.getName(), cssStyle.getTextTitleFontWeight());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextTitleFontStyle())) {
      properties.put(MappedAttributes.TEXT_TITLE_FONT_STYLE.getName(), cssStyle.getTextTitleFontStyle());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextHeaderColor())) {
      properties.put(MappedAttributes.TEXT_HEADER_COLOR.getName(), cssStyle.getTextHeaderColor());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextHeaderFontSize())) {
      properties.put(MappedAttributes.TEXT_HEADER_FONT_SIZE.getName(), cssStyle.getTextHeaderFontSize());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextHeaderFontWeight())) {
      properties.put(MappedAttributes.TEXT_HEADER_FONT_WEIGHT.getName(), cssStyle.getTextHeaderFontWeight());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextHeaderFontStyle())) {
      properties.put(MappedAttributes.TEXT_HEADER_FONT_STYLE.getName(), cssStyle.getTextHeaderFontStyle());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextColor())) {
      properties.put(MappedAttributes.TEXT_COLOR.getName(), cssStyle.getTextColor());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextFontSize())) {
      properties.put(MappedAttributes.TEXT_FONT_SIZE.getName(), cssStyle.getTextFontSize());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextFontWeight())) {
      properties.put(MappedAttributes.TEXT_FONT_WEIGHT.getName(), cssStyle.getTextFontWeight());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextFontStyle())) {
      properties.put(MappedAttributes.TEXT_FONT_STYLE.getName(), cssStyle.getTextFontStyle());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextSubtitleColor())) {
      properties.put(MappedAttributes.TEXT_SUBTITLE_COLOR.getName(), cssStyle.getTextSubtitleColor());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextSubtitleFontSize())) {
      properties.put(MappedAttributes.TEXT_SUBTITLE_FONT_SIZE.getName(), cssStyle.getTextSubtitleFontSize());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextSubtitleFontWeight())) {
      properties.put(MappedAttributes.TEXT_SUBTITLE_FONT_WEIGHT.getName(), cssStyle.getTextSubtitleFontWeight());
    }
    if (StringUtils.isNotBlank(cssStyle.getTextSubtitleFontStyle())) {
      properties.put(MappedAttributes.TEXT_SUBTITLE_FONT_STYLE.getName(), cssStyle.getTextSubtitleFontStyle());
    }
  }

  @SuppressWarnings("unchecked")
  private void mapAppStyleToProperties(ApplicationBackgroundStyle cssStyle, JSONObject properties) { // NOSONAR
    if (StringUtils.isNotBlank(cssStyle.getBackgroundColor())) {
      properties.put(MappedAttributes.APP_BACKGROUND_COLOR.getName(), cssStyle.getBackgroundColor());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundImage())) {
      properties.put(MappedAttributes.APP_BACKGROUND_IMAGE.getName(), cssStyle.getBackgroundImage());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundEffect())) {
      properties.put(MappedAttributes.APP_BACKGROUND_EFFECT.getName(), cssStyle.getBackgroundEffect());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundPosition())) {
      properties.put(MappedAttributes.APP_BACKGROUND_POSITION.getName(), cssStyle.getBackgroundPosition());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundSize())) {
      properties.put(MappedAttributes.APP_BACKGROUND_SIZE.getName(), cssStyle.getBackgroundSize());
    }
    if (StringUtils.isNotBlank(cssStyle.getBackgroundRepeat())) {
      properties.put(MappedAttributes.APP_BACKGROUND_REPEAT.getName(), cssStyle.getBackgroundRepeat());
    }
  }

}
