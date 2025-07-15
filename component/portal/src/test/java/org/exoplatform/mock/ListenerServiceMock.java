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
package org.exoplatform.mock;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerBase;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A class to delete Asynchronous calls to ListenerService in Test context
 */
@SuppressWarnings({
    "rawtypes", "unchecked"
})
public class ListenerServiceMock extends ListenerService {

  private static final Log                  LOG       = ExoLogger.getLogger("exo.kernel.component.common.ListenerService");

  private final Map<String, List<ListenerBase>> listeners = new ConcurrentHashMap<>();

  public ListenerServiceMock(ExoContainerContext ctx) {
    super(ctx, null, null);
  }

  @Override
  public void addListener(Listener listener) {
    addListener(listener.getName(), listener);
  }

  @Override
  public synchronized void addListener(String eventName, ListenerBase listener) {
    listeners.computeIfAbsent(eventName, key -> new Vector<ListenerBase>())
             .add(listener);
  }

  @Override
  public <S, D> void broadcast(String name, S source, D data) {
    List<ListenerBase> list = listeners.get(name);
    if (list == null) {
      return;
    }
    for (ListenerBase<S, D> listener : list) {
      try {
        listener.onEvent(new Event<>(name, source, data));
      } catch (Exception e) {
        LOG.error("Exception while broadcasting event with name {}", name, e);
      }
    }
  }

  @Override
  public <T extends Event> void broadcast(T event) {
    List<ListenerBase> list = listeners.get(event.getEventName());
    if (list == null) {
      return;
    }
    for (ListenerBase listener : list) {
      try {
        listener.onEvent(event);
      } catch (Exception e) {
        LOG.error("Exception while broadcasting event {}: ", event, e);
      }
    }
  }

}
