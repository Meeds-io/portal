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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.portal.jdbc.entity.DescriptionEntity;
import org.exoplatform.portal.jdbc.entity.DescriptionState;
import org.exoplatform.portal.mop.dao.DescriptionDAO;

public class DescriptionStorageImpl implements DescriptionStorage {

  private static final String DESCRIPTION_IDENTIFIER_IS_MANDATORY = "Description identifier is mandatory";

  private static final String NO_VARIANT_CAB_BE_SET_ON_LOCALE     = "No variant cab be set on locale";

  private static final String NO_LANGUAGE_SET_ON_LOCALE           = "No language set on locale";

  private static final String NO_NULL_LOCALE_ACCEPTED             = "No null locale accepted";

  private static final String NO_NULL_ID_ACCEPTED                 = "No null id accepted";

  private DescriptionDAO      descriptionDAO;

  public DescriptionStorageImpl(DescriptionDAO descriptionDAO) {
    this.descriptionDAO = descriptionDAO;
  }

  public org.exoplatform.portal.mop.State resolveDescription(String id,
                                                             Locale defaultLocale,
                                                             Locale locale) throws NullPointerException {
    org.exoplatform.portal.mop.State state = resolveDescription(id, locale);
    if (state == null && defaultLocale != null) {
      state = resolveDescription(id, defaultLocale);
    }
    return state;
  }

  public org.exoplatform.portal.mop.State resolveDescription(String id, Locale locale) throws NullPointerException {
    return getDescription(id, locale, true);
  }

  public org.exoplatform.portal.mop.State getDescription(String id, Locale locale) {
    return getDescription(id, locale, false);
  }

  public org.exoplatform.portal.mop.State getDescription(String id) {
    return getDescription(id, null, false);
  }

  public Map<Locale, org.exoplatform.portal.mop.State> getDescriptions(String id) {
    if (id == null) {
      throw new NullPointerException(NO_NULL_ID_ACCEPTED);
    }
    Map<Locale, org.exoplatform.portal.mop.State> names = null;

    DescriptionEntity desc = descriptionDAO.getByRefId(id);
    if (desc != null) {
      Map<String, DescriptionState> localized = desc.getLocalized();

      if (localized != null) {
        names = new HashMap<>(localized.size());
        for (Map.Entry<String, DescriptionState> entry : localized.entrySet()) {
          DescriptionState state = entry.getValue();
          names.put(I18N.parseTagIdentifier(entry.getKey()),
                    new org.exoplatform.portal.mop.State(state.getName(),
                                                         state.getDescription()));
        }
      }
    }
    return names;
  }

  public void setDescription(String id, Locale locale, org.exoplatform.portal.mop.State description) {
    if (id == null) {
      throw new NullPointerException(NO_NULL_ID_ACCEPTED);
    }
    if (locale == null) {
      throw new NullPointerException(NO_NULL_LOCALE_ACCEPTED);
    }
    if (locale.getLanguage().length() == 0) {
      throw new IllegalArgumentException(NO_LANGUAGE_SET_ON_LOCALE);
    }
    if (locale.getVariant().length() > 0) {
      throw new IllegalArgumentException(NO_VARIANT_CAB_BE_SET_ON_LOCALE);
    }

    Map<String, DescriptionState> state = new HashMap<>();

    DescriptionEntity entity = descriptionDAO.getByRefId(id);
    if (entity != null) {
      state.putAll(entity.getLocalized());
    }

    if (description != null) {
      state.put(I18N.toTagIdentifier(locale), new DescriptionState(description.getName(), description.getDescription()));
    } else {
      state.remove(I18N.toTagIdentifier(locale));
    }

    descriptionDAO.saveDescriptions(id, state);
  }

  public void setDescription(String id, org.exoplatform.portal.mop.State description) {
    if (id == null) {
      throw new NullPointerException(NO_NULL_ID_ACCEPTED);
    }

    if (description != null) {
      descriptionDAO.saveDescription(id, new DescriptionState(description.getName(), description.getDescription()));
    } else {
      descriptionDAO.deleteByRefId(id);
    }
  }

  public void setDescriptions(String id, Map<Locale, org.exoplatform.portal.mop.State> descriptions) {
    if (id == null) {
      throw new NullPointerException(NO_NULL_ID_ACCEPTED);
    }
    if (descriptions != null) {
      Map<String, DescriptionState> localized = new HashMap<>(descriptions.size());
      for (Map.Entry<Locale, org.exoplatform.portal.mop.State> entry : descriptions.entrySet()) {
        Locale locale = entry.getKey();
        org.exoplatform.portal.mop.State state = entry.getValue();
        if (locale.getLanguage().length() == 0) {
          throw new IllegalArgumentException(NO_LANGUAGE_SET_ON_LOCALE);
        }
        if (locale.getVariant().length() > 0) {
          throw new IllegalArgumentException(NO_VARIANT_CAB_BE_SET_ON_LOCALE);
        }
        localized.put(I18N.toTagIdentifier(locale), new DescriptionState(state.getName(), state.getDescription()));
      }

      descriptionDAO.saveDescriptions(id, localized);
    } else {
      descriptionDAO.deleteByRefId(id);
    }
  }

  public org.exoplatform.portal.mop.State getDescription(String id, // NOSONAR
                                                         Locale locale,
                                                         boolean checkParent) {
    if (id == null) {
      throw new IllegalArgumentException(DESCRIPTION_IDENTIFIER_IS_MANDATORY);
    }
    DescriptionEntity desc = descriptionDAO.getByRefId(id);
    if (desc != null) {
      if (locale == null) {
        DescriptionState state = desc.getState();
        if (state != null) {
          return new org.exoplatform.portal.mop.State(state.getName(), state.getDescription());
        }
      } else {
        do {
          DescriptionState state = desc.getLocalized().get(I18N.toTagIdentifier(locale));
          if (state != null) {
            return new org.exoplatform.portal.mop.State(state.getName(), state.getDescription());
          }
          locale = parent(locale);
        } while (checkParent && locale != null);
      }
    }
    return null;
  }

  private Locale parent(Locale locale) {
    if (locale.getVariant() != null && !locale.getVariant().isEmpty()) {
      return new Locale(locale.getLanguage(), locale.getCountry());
    } else if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
      return new Locale(locale.getLanguage());
    } else {
      return null;
    }
  }
}
