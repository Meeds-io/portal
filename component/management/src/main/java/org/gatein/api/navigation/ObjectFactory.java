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
package org.gatein.api.navigation;

import java.util.*;

import org.gatein.api.ApiException;
import org.gatein.api.common.i18n.Localized.Value;
import org.gatein.api.common.i18n.LocalizedString;
import org.gatein.api.navigation.Visibility.Status;

import org.exoplatform.portal.mop.navigation.NodeState;

public class ObjectFactory {
    private ObjectFactory() {
    }

    public static LocalizedString createLocalizedString(Map<Locale, org.exoplatform.portal.mop.State> descriptions) {
        if (descriptions == null)
            return null;

        Map<Locale, String> m = new HashMap<Locale, String>();
        for (Map.Entry<Locale, org.exoplatform.portal.mop.State> entry : descriptions.entrySet()) {
            // For some reason (UI issue possibly) an english locale can be set with no value.
            if (entry.getValue().getName() != null) {
                m.put(entry.getKey(), entry.getValue().getName());
            }
        }
        return new LocalizedString(m);
    }

    public static Map<Locale, org.exoplatform.portal.mop.State> createDescriptions(LocalizedString string) {
        Map<Locale, org.exoplatform.portal.mop.State> descriptions = new HashMap<Locale, org.exoplatform.portal.mop.State>();
        for (Value<String> v : string.getLocalizedValues()) {
            descriptions.put(v.getLocale(), new org.exoplatform.portal.mop.State(v.getValue(), null));
        }
        return descriptions;
    }

    public static org.exoplatform.portal.mop.Visibility createVisibility(Status flag) {
        switch (flag) {
            case VISIBLE:
                return org.exoplatform.portal.mop.Visibility.DISPLAYED;
            case HIDDEN:
                return org.exoplatform.portal.mop.Visibility.HIDDEN;
            case SYSTEM:
                return org.exoplatform.portal.mop.Visibility.SYSTEM;
            default:
                throw new ApiException("Unknown visibility flag " + flag);
        }
    }

    public static Visibility createVisibility(NodeState nodeState) {
        Status flag = createFlag(nodeState.getVisibility());

        if (flag == Status.PUBLICATION) {
            long start = nodeState.getStartPublicationTime();
            long end = nodeState.getEndPublicationTime();

            PublicationDate publicationDate = null;
            if (start != -1 && end != -1) {
                publicationDate = PublicationDate.between(new Date(start), new Date(end));
            } else if (start != -1) {
                publicationDate = PublicationDate.startingOn(new Date(start));
            } else if (end != -1) {
                publicationDate = PublicationDate.endingOn(new Date(end));
            }

            return new Visibility(publicationDate);
        }

        return new Visibility(flag);
    }

    private static Status createFlag(org.exoplatform.portal.mop.Visibility visibility) {
        switch (visibility) {
            case DISPLAYED:
                return Status.VISIBLE;
            case HIDDEN:
                return Status.HIDDEN;
            case SYSTEM:
                return Status.SYSTEM;
            case TEMPORAL:
                return Status.PUBLICATION;
            default:
                throw new ApiException("Unknown internal visibility '" + visibility + "'");
        }
    }
}
