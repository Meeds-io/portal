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
package io.meeds.spring.web.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.StateKey;
import org.exoplatform.services.security.jaas.UserPrincipal;
import org.exoplatform.services.security.web.HttpSessionStateKey;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;

@Component
public class PortalAuthenticationManager implements AuthenticationProvider {

  private static OrganizationService  organizationService;

  private static ConversationRegistry conversationRegistry;

  private static IdentityRegistry     identityRegistry;

  private static Authenticator        authenticator;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = requestAttributes.getRequest();

    try {
      Identity identity = getCurrentIdentity(request);
      if (isAnonymousUser(identity)
          && authentication.getPrincipal() instanceof String username) {
        identity = getCurrentIdentity(request, username);
      }
      if (isAnonymousUser(identity)) {
        return new AnonymousAuthenticationToken(IdentityConstants.ANONIM,
                                                IdentityConstants.ANONIM,
                                                Collections.singletonList(new JaasGrantedAuthority("guests",
                                                                                                   new UserPrincipal(IdentityConstants.ANONIM))));
      }
      Principal userPrincipal = new UserPrincipal(identity.getUserId());
      List<GrantedAuthority> authorities = getAuthorities(identity, userPrincipal);
      List<GrantedAuthority> extendedAuthorities = getExtendedAuthorities(userPrincipal);
      if (CollectionUtils.isNotEmpty(extendedAuthorities)) {
        authorities = new ArrayList<>(authorities);
        authorities.addAll(extendedAuthorities);
      }
      return new PreAuthenticatedAuthenticationToken(userPrincipal,
                                                     identity.getUserId(),
                                                     authorities);
    } catch (Exception e) {
      throw new AuthenticationServiceException("An unknown error is encountered while authenticating user", e);
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return true;
  }

  private Identity getCurrentIdentity(HttpServletRequest httpRequest) throws Exception {
    ConversationState state = ConversationState.getCurrent();
    if (state == null) {
      return getCurrentIdentity(httpRequest, httpRequest.getRemoteUser());
    } else {
      return state.getIdentity();
    }
  }

  private Identity getCurrentIdentity(HttpServletRequest httpRequest, String userId) throws Exception {
    // only if user authenticated, otherwise there is no reason to do anythings
    if (userId != null) {
      StateKey stateKey = new HttpSessionStateKey(httpRequest.getSession());
      ConversationState state = getStateBySessionId(userId, stateKey);
      if (state == null) {
        state = buildState(userId, stateKey);
      }
      return state == null ? null : state.getIdentity();
    } else {
      return new Identity(IdentityConstants.ANONIM);
    }
  }

  /**
   * A method to allow extending attributed roles to user in current portal
   * context
   * 
   * @param userPrincipal current user {@link Principal}
   * @return {@link List} of {@link GrantedAuthority} to add to current user
   */
  protected List<GrantedAuthority> getExtendedAuthorities(Principal userPrincipal) {
    return Collections.emptyList();
  }

  private List<GrantedAuthority> getAuthorities(Identity identity, Principal principal) {
    return identity.getRoles()
                   .stream()
                   .map(role -> (GrantedAuthority) new JaasGrantedAuthority(role, principal))
                   .toList();
  }

  private ConversationState buildState(String userId, StateKey stateKey) throws Exception {
    Identity identity = buildIdentity(userId);
    if (identity == null) {
      return null;
    } else {
      return buildState(identity, stateKey);
    }
  }

  private ConversationState buildState(Identity identity, StateKey stateKey) {
    ConversationState state = new ConversationState(identity);
    getConversationRegistry().register(stateKey, state);
    return state;
  }

  private Identity buildIdentity(String userId) throws Exception {
    Identity identity = getIdentityRegistry().getIdentity(userId);
    if (identity == null) {
      identity = getAuthenticator().createIdentity(userId);
      getIdentityRegistry().register(identity);
    }
    return identity;
  }

  private ConversationState getStateBySessionId(String userId, StateKey stateKey) {
    ConversationState state = getConversationRegistry().getState(stateKey);
    if (state != null && !userId.equals(state.getIdentity().getUserId())) {
      state = null;
      getConversationRegistry().unregister(stateKey, false);
    }
    return state;
  }

  @SneakyThrows
  private boolean isDisabledUser(String username) {
    return null == getOrganizationService().getUserHandler()
                                           .findUserByName(username, UserStatus.ENABLED);
  }

  private boolean isAnonymousUser(Identity identity) {
    return identity == null
           || IdentityConstants.ANONIM.equals(identity.getUserId())
           || (!identity.isMemberOf("/platform/users") && !identity.isMemberOf("/platform/externals"))
           || isDisabledUser(identity.getUserId());
  }

  private static Authenticator getAuthenticator() {
    if (authenticator == null) {
      authenticator = ExoContainerContext.getService(Authenticator.class);
    }
    return authenticator;
  }

  private static IdentityRegistry getIdentityRegistry() {
    if (identityRegistry == null) {
      identityRegistry = ExoContainerContext.getService(IdentityRegistry.class);
    }
    return identityRegistry;
  }

  private static OrganizationService getOrganizationService() {
    if (organizationService == null) {
      organizationService = ExoContainerContext.getService(OrganizationService.class);
    }
    return organizationService;
  }

  private static ConversationRegistry getConversationRegistry() {
    if (conversationRegistry == null) {
      conversationRegistry = ExoContainerContext.getService(ConversationRegistry.class);
    }
    return conversationRegistry;
  }

}
