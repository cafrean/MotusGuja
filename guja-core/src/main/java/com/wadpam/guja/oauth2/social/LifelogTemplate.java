package com.wadpam.guja.oauth2.social;

import com.wadpam.guja.oauth2.api.FactoryResource;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Fredrik Andersson on 2015-02-12.
 */
public class LifelogTemplate extends NetworkTemplate {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LifelogTemplate.class);

    public static final String BASE_URL_LIFELOG = "https://platform.lifelog.sonymobile.com";

    protected final String access_token;

    public LifelogTemplate(String access_token, String baseUrl) {
        super(baseUrl);
        this.access_token = access_token;
    }

    public static LifelogTemplate create(String providerId, String access_token,
                                         String domain) {

        if (FactoryResource.PROVIDER_ID_LIFELOG.equals(providerId)) {
            return new LifelogTemplate(access_token, BASE_URL_LIFELOG);
        }

        throw new IllegalArgumentException(String.format("No such provider %s.", providerId));
    }

    public LifelogProfile getProfile() throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", String.format("Bearer %s", access_token));

        Map<String, Object> props = get(String.format("%s/v1/users/me", getBaseUrl()), headers, Map.class);
        return parseProfile(props);
    }

    @Override
    public <J> J exchange(String method, String url,
                          Map<String, String> requestHeaders,
                          Object requestBody, Class<J> responseClass) {

        return super.exchange(method, url,
                requestHeaders, requestBody, responseClass);
    }


    protected LifelogProfile parseProfile(Map<String, Object> props) {

        if (!props.containsKey("result")) {
            throw new IllegalArgumentException("Could not fetch result");
        }

        LifelogProfile profile = LifelogProfile.with(props);

        // This is done since Lifelog doesn't return proper, JSON-formatted, responses.
        String response = props.get("result").toString();
        Matcher matcher = Pattern.compile("username=(.*?),").matcher(response);

        if (matcher.find()) {
            profile.setUserName(matcher.group(1));
        }

        return profile;
    }
}
