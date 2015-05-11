package com.wadpam.guja.oauth2.social;

import com.wadpam.guja.oauth2.api.FactoryResource;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Fredrik Andersson on 2015-03-15.
 */
public class HealthGraphTemplate extends NetworkTemplate {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HealthGraphTemplate.class);

    public static final String BASE_URL_HEALTHGRAPH = "https://api.runkeeper.com";
    private final String USER_ENDPOINT = "/user";

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

        Map<String, Object> userInfo = get(getBaseUrl() + USER_ENDPOINT, headers, Map.class);
        LOGGER.info(String.format("Map to string: %s", userInfo));
        Map<String, Object> profile = null;

        if (userInfo.containsKey("profile")) {
            String profileUri = (String) userInfo.get("profile");
            LOGGER.info(String.format("Got profile URI: %s", profileUri));
            profile = get(getBaseUrl() + profileUri, headers, Map.class);
        }

        return parseProfile(profile);
    }

    @Override
    public <J> J exchange(String method, String url,
                          Map<String, String> requestHeaders,
                          Object requestBody, Class<J> responseClass) {

        return super.exchange(method, url,
                requestHeaders, requestBody, responseClass);
    }


    protected HealthGraphProfile parseProfile(Map<String, Object> props) {


        if (props == null || !props.containsKey("elite")) {
            throw new IllegalArgumentException("Could not fetch result");
        }

        LOGGER.info(String.format("Profile response contents: %s", props.toString()));

        String imageUrl = "";


        // Since there is no way to tell what sort of image Healthgraph will send, or if the user name will be included, the awful mess below is necessary...

        if(props.containsKey("large_picture")){
            imageUrl = (String)props.get("large_picture");
        }else if(props.containsKey("medium_picture")){
            imageUrl = (String)props.get("medium_picture");
        }else if(props.containsKey("normal_picture")){
            imageUrl = (String)props.get("normal_picture");
        }else if(props.containsKey("small_picture")){
            imageUrl = (String)props.get("small_picture");
        }



        HealthGraphProfile profile = new HealthGraphProfile(props);

        // Set user name (if it exists)
        if(props.containsKey("name")){
            profile.setUserName((String) props.get("name"));
        }

        // Set image URI (if it exists)
        if(!imageUrl.isEmpty()){
            profile.setImageUri(imageUrl);
        }

        return profile;
    }
}
