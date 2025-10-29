package com.ankit.projects.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class ObjectMetadata {
    public String key;
    public Date createdAt;

    @JsonCreator
    @JsonIgnoreProperties(ignoreUnknown = true)
    public ObjectMetadata(@JsonProperty("key") String key,
                          @JsonProperty("createdAt") Date createdAt){
        this.key = key;
        this.createdAt = createdAt;
    }
}
