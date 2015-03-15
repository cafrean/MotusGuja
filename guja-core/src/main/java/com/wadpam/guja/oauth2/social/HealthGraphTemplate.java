package com.wadpam.guja.oauth2.social;

import com.wadpam.guja.oauth2.api.FactoryResource;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Fredrik Andersson on 2015-03-15.
 */
public class HealthGraphTemplate extends NetworkTemplate {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HealthGraphTemplate.class);

    public static final String BASE_URL_HEALTHGRAPH = "https://api.runkeeper.com/user/";

    protected final String access_token;

    public HealthGraphTemplate(String access_token, String baseUrl) {
        super(baseUrl);
        this.access_token = access_token;
    }

    public static HealthGraphTemplate create(String providerId, String access_token,
                                         String domain) {

        if (FactoryResource.PROVIDER_ID_LIFELOG.equals(providerId)) {
            return new HealthGraphTemplate(access_token, BASE_URL_HEALTHGRAPH);
        }

        throw new IllegalArgumentException(String.format("No such provider %s.", providerId));
    }

    public HealthGraphProfile getProfile() throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", String.format("Bearer %s", access_token));

        Map<String, Object> props = get(getBaseUrl(), headers, Map.class);
        return parseProfile(props);
    }

    @Override
    public <J> J exchange(String method, String url,
                          Map<String, String> requestHeaders,
                          Object requestBody, Class<J> responseClass) {

        return super.exchange(method, url,
                requestHeaders, requestBody, responseClass);
    }


    protected HealthGraphProfile parseProfile(Map<String, Object> props) {
        if (props == null || !props.containsKey("userID")) {
            throw new IllegalArgumentException("Could not fetch result");
        }

        HealthGraphProfile profile = HealthGraphProfile.with(props)
                .userId("userID");


        return profile;
    }
}
