package com.wadpam.guja.oauth2.api;

/*
 * #%L
 * guja-core
 * %%
 * Copyright (C) 2014 Wadpam
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.wadpam.guja.environment.ServerEnvironment;
import com.wadpam.guja.exceptions.InternalServerErrorRestException;
import com.wadpam.guja.exceptions.UnauthorizedRestException;
import com.wadpam.guja.oauth2.dao.DConnectionDaoBean;
import com.wadpam.guja.oauth2.dao.DFactoryDaoBean;
import com.wadpam.guja.oauth2.dao.DFactoryMapper;
import com.wadpam.guja.oauth2.domain.DConnection;
import com.wadpam.guja.oauth2.domain.DFactory;
import com.wadpam.guja.oauth2.domain.DOAuth2User;
import com.wadpam.guja.oauth2.provider.Oauth2UserProvider;
import com.wadpam.guja.oauth2.provider.TokenGenerator;
import com.wadpam.guja.oauth2.social.LifelogProfile;
import com.wadpam.guja.oauth2.social.LifelogTemplate;
import com.wadpam.guja.oauth2.social.SocialProfile;
import com.wadpam.guja.oauth2.social.SocialTemplate;
import com.wadpam.guja.web.JsonCharacterEncodingResponseFilter;
import com.wadpam.guja.oauth2.web.OAuth2Filter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An oauth2 implementation support the Resource Owner Password Credential Grant flow.
 * http://tools.ietf.org/html/rfc6749#section-4.3
 * This flow is typically used by trusted client that my touch the resource owners (user) credentials.
 *
 * @author mattiaslevin
 * @author sosandstrom
 */
@Path("oauth/federated")
@Singleton
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
@Produces(JsonCharacterEncodingResponseFilter.APPLICATION_JSON_UTF8)
public class OAuth2FederatedResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2FederatedResource.class);

  private static final int DEFAULT_EXPIRES_IN = 60 * 60 * 24 * 7;  // 1 week

  private final DConnectionDaoBean connectionDao;
  private final DFactoryDaoBean factoryDao;
  private final Oauth2UserProvider userProvider;
  private final TokenGenerator accessTokenGenerator;

  private int tokenExpiresIn = DEFAULT_EXPIRES_IN;

  @Inject
  public OAuth2FederatedResource(TokenGenerator accessTokenGenerator,
                                 Oauth2UserProvider userProvider,
                                 ServerEnvironment serverEnvironment,
                                 DConnectionDaoBean connectionDao,
                                 DFactoryDaoBean factoryDao) {

    this.connectionDao = connectionDao;
    this.factoryDao = factoryDao;
    this.accessTokenGenerator = accessTokenGenerator;
    this.userProvider = userProvider;

    if (serverEnvironment.isDevEnvironment()) {
      try {

        // TODO Move values to a property file

          factoryDao.put(DFactoryMapper.newBuilder()
                  .id(FactoryResource.PROVIDER_ID_LIFELOG)
                  .baseUrl("https://platform.lifelog.sonymobile.com")
                  .clientId("hRWEZxmekgG11723mTV5TPLeqxcG5vMC")
                  .clientSecret("bw5AqcTnFGbAJy2r")
                  .build());

      } catch (IOException e) {
        LOGGER.error("populating factory", e);
      }
    }
  }

  private DConnection generateConnection(DOAuth2User oauth2User, String profileUrl, String imageUrl) {
    DConnection connection = new DConnection();
    connection.setAccessToken(accessTokenGenerator.generate());
    connection.setRefreshToken(accessTokenGenerator.generate());
    connection.setProviderId(FactoryResource.PROVIDER_ID_SELF);
    connection.setProviderUserId(oauth2User.getId().toString());
    connection.setUserId(oauth2User.getId());
    connection.setExpireTime(calculateExpirationDate(tokenExpiresIn));
    connection.setUserRoles(OAuth2AuthorizationResource.convertRoles(oauth2User.getRoles()));
    connection.setDisplayName(oauth2User.getDisplayName());

    return connection;
  }

  private Date calculateExpirationDate(int expiresInSeconds) {
    return DateTime.now().plusSeconds(expiresInSeconds).toDate();
  }

  private NewCookie createCookie(String accessToken, int expiresInSeconds) {
    return new NewCookie(OAuth2Filter.NAME_ACCESS_TOKEN, accessToken, "/api", null, null, expiresInSeconds, false);
  }

  private boolean hasAccessTokenExpired(DConnection connection) {
    return new DateTime(connection.getExpireTime()).isBeforeNow();
  }

  /**
   * Register federated using an access token obtained from a 3rd party service such as Facebook, Google etc.
   *
   * @param providerId     unique provider id
   * @param providerUserId unique user id within the provider context
   * @param access_token   access token
   * @param secret         secret
   * @param expiresIn      access token expiration
   * @param lifelogAccessToken    A secondary access token - used when performing federated registration with more than one service.
   * @return the userId associated with the Connection, null if new Connection
   */
  @GET
  public Response registerFederatedGet(
      @QueryParam("providerId") String providerId,
      @QueryParam("providerUserId") String providerUserId,
      @QueryParam("access_token") String access_token,
      @QueryParam("secret") String secret,
      @QueryParam("expires_in") @DefaultValue("4601") Integer expiresIn,
      @QueryParam("lifelogAccessToken") String lifelogAccessToken,
      @QueryParam("unnamedAccessToken") String unnamedAccessToken
  ) throws IOException {
    return registerFederated(access_token, providerId, providerUserId,
        secret, expiresIn, lifelogAccessToken, unnamedAccessToken);
  }


  /**
   * Register federated using an access token obtained from a 3rd party service such as Facebook, Google etc.
   *
   * @param providerId     unique provider id
   * @param providerUserId unique user id within the provider context
   * @param access_token   access token
   * @param secret         secret
   * @param expiresIn      access token expiration
   * @param lifelogAccessToken    The secondary access token - used when performing federated registration with more than one service.
   * @return the userId associated with the Connection, null if new Connection
   */
  @GET
  @Path("{providerId}")
  public Response registerFederatedGetPath(
      @PathParam("providerId") String providerId,
      @QueryParam("providerUserId") String providerUserId,
      @QueryParam("access_token") String access_token,
      @QueryParam("secret") String secret,
      @QueryParam("expires_in") @DefaultValue("4601") Integer expiresIn,
      @QueryParam("lifelogAccessToken") String lifelogAccessToken,
      @QueryParam("unnamedAccessToken") String unnamedAccessToken
  ) throws IOException {
    return registerFederated(access_token, providerId, providerUserId,
        secret, expiresIn, lifelogAccessToken, unnamedAccessToken);
  }


  protected Response registerFederated(
      String access_token,
      String providerId,
      String providerUserId,
      String secret,
      Integer expiresInSeconds,
      String lifelogAccessToken,
      String unnamedAccessToken) throws IOException {

    checkNotNull(lifelogAccessToken);
      // TODO: Check if unnamedAccessToken is null.
    checkNotNull(providerId);

    if (null == expiresInSeconds) {
      expiresInSeconds = tokenExpiresIn;
    }

    // use the connectionFactory
    final LifelogTemplate lifelogTemplate = LifelogTemplate.create(providerId, lifelogAccessToken, null);

    LifelogProfile profile = null;
    try {
      profile = lifelogTemplate.getProfile();
      if (null == profile) {
        throw new UnauthorizedRestException("Invalid connection");
      }
    } catch (IOException unauthorized) {
      throw new UnauthorizedRestException("Unauthorized federated side");
    }

    // providerUserId is optional, fetch it if necessary:
    final String realProviderUserId = profile.getUserName();
    if (null == providerUserId) {
      providerUserId = realProviderUserId;
    } else if (!providerUserId.equals(realProviderUserId)) {
      throw new UnauthorizedRestException("Unauthorized federated side mismatch");
    }

      LOGGER.info("Got provideruserID: " + providerUserId);

    // load connection from db async style (likely case is new token for existing user)
    final Iterable<DConnection> existingConnections = connectionDao.queryByProviderUserId(providerUserId);

    boolean isNewUser = !existingConnections.iterator().hasNext();
    DOAuth2User user = null;
    if (isNewUser) {

      // Create new oauth2 user
      user = userProvider.createUser();
      user.setDisplayName(profile.getUserName());
      user.setRoles(OAuth2UserResource.DEFAULT_ROLES_USER);

    } else {
      user = userProvider.getUserById(existingConnections.iterator().next().getUserId());
    }

    user = userProvider.putUser(user);

    // load existing conn for token
    DConnection connection = connectionDao.findByAccessToken(access_token);
    if (null == connection) {

      connection = new DConnection();
      connection.setAccessToken(accessTokenGenerator.generate());
        connection.setLifelogAccessToken(lifelogAccessToken);
        connection.setUnnamedAccessToken(unnamedAccessToken);
      connection.setProviderId(providerId);
      connection.setProviderUserId(providerUserId);
      connection.setSecret(secret);
      connection.setDisplayName(user.getDisplayName());
      connection.setUserId(user.getId());
      connection.setExpireTime(calculateExpirationDate(expiresInSeconds));

    }
    // Always update some properties
    connection.setUserRoles(OAuth2AuthorizationResource.convertRoles(user.getRoles()));
    connectionDao.put(connection);

    // Remove expired connections for the user
    removeExpiredConnections(providerId, existingConnections);

    return Response.status(isNewUser ? Response.Status.CREATED : Response.Status.OK)
        .cookie(createCookie(connection.getAccessToken(), null != expiresInSeconds ? expiresInSeconds : tokenExpiresIn))
        .entity(connection)
        .build();
  }

  private void removeExpiredConnections(String providerId, Iterable<DConnection> connections) {

    // find other connections for this user, discard expired
    final ArrayList<Long> expiredTokens = new ArrayList<Long>();

    for (DConnection dc : connections) {
      if (providerId.equals(dc.getProviderId())) {
        // expired? only remove if no refresh token
        if (null == dc.getRefreshToken() && null != dc.getExpireTime() && hasAccessTokenExpired(dc)) {
          expiredTokens.add(dc.getId());
        }
      }
    }

    try {
      connectionDao.delete(expiredTokens);
    } catch (IOException e) {
      LOGGER.error("Failed to delete expired tokens {}", e);
      throw new InternalServerErrorRestException("Failed to delete expired tokens");
    }

  }

  @Inject(optional = true)
  public void setTokenExpiresIn(@Named("app.oauth.tokenExpiresIn") int tokenExpiresIn) {
    this.tokenExpiresIn = tokenExpiresIn;
  }

}
