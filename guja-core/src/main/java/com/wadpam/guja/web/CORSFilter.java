package com.wadpam.guja.web;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.wadpam.guja.environment.ServerEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Convert box requests and responses Content-Type and Accept headers from octet/stream to application/x-protobuf
 * The box modem is not able to set a custom Content-Type or Accept header.
 *
 * @author mattiaslevin
 */
@Singleton
public class CORSFilter implements Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(CORSFilter.class);

  private final static String DEFAULT_ALLOWED_ORIGINS = "*";
  private final static String DEFAULT_ALLOWED_METHODS = "GET, POST, DELETE";

  private final Provider<ServerEnvironment> environmentProvider;

  private boolean alwaysEnabled = false;

  @Inject
  public CORSFilter(Provider<ServerEnvironment> environmentProvider) {
    this.environmentProvider = environmentProvider;
  }


  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    String value = filterConfig.getInitParameter("alwaysEnabled");
    if (null != value && value.equals("true")) {
      LOGGER.warn("Always enable CORS");
      alwaysEnabled = true;
    }
  }

  @Override
  public void doFilter(final ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest)req;
    HttpServletResponse response = (HttpServletResponse)res;

    if (shouldEnableCORS()) {
      //LOGGER.debug("Set CORS headers");
      response.addHeader("Access-Control-Allow-Origin", DEFAULT_ALLOWED_ORIGINS);
      response.addHeader("Access-Control-Allow-Methods", DEFAULT_ALLOWED_METHODS);
    }

    chain.doFilter(request, response);
  }

  private boolean shouldEnableCORS() {
    return alwaysEnabled || (null != environmentProvider && environmentProvider.get().isDevEnvironment());
  }


  @Override
  public void destroy() {
    // Do nothing
  }

}
