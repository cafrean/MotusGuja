package com.wadpam.guja.oauth2.social;

import java.util.Map;

/**
 * Created by Fredrik Andersson on 2015-03-15.
 */
public class HealthGraphProfile {

    private String userId;
    private String imageUri;
    private String userName;

    private Map<String, Object> props = null;

    protected HealthGraphProfile(Map<String, Object> props) {
        this.props = props;
    }

    public HealthGraphProfile build() {
        props = null;
        return this;
    }

    public static HealthGraphProfile with(Map<String, Object> props) {
        return new HealthGraphProfile(props);
    }

    /**
     * Sets the user ID using the value in the properties Map.
     *
     * @param propertyName
     * @return
     */
    public HealthGraphProfile userId(String propertyName) {
        this.userId = (String) props.get(propertyName).toString();
        return this;
    }

    public HealthGraphProfile userName(String propertyName) {
        this.userName = (String) props.get(propertyName).toString();
        return this;
    }

    public HealthGraphProfile imageUri(String propertyName) {
        this.imageUri = (String) props.get(propertyName).toString();
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
