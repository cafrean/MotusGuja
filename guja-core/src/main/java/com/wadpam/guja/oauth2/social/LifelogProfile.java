package com.wadpam.guja.oauth2.social;

import java.util.Map;

/**
 * Created by Fredrik Andersson on 2015-02-12.
 */
public class LifelogProfile{

    private String userName;

    private Map<String, Object> props = null;

    protected LifelogProfile(Map<String, Object> props) {
        this.props = props;
    }

    public LifelogProfile build(){
        props = null;
        return this;
    }

    public static LifelogProfile with(Map<String, Object> props) {
        return new com.wadpam.guja.oauth2.social.LifelogProfile(props);
    }

    /**
     * Sets the user name using the value in the properties Map.
     *
     * This method is currently unusable due to Sonys formatting of the response.
     *
     * @param propertyName
     * @return
     */
    public LifelogProfile userName(String propertyName) {
        this.userName = (String) props.get(propertyName).toString();
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
