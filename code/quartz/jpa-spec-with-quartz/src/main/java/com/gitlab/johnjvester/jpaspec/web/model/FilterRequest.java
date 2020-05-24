package com.gitlab.johnjvester.jpaspec.web.model;

import lombok.Data;

@Data
public class FilterRequest {
    private Boolean active;
    private String zipFilter;

    public FilterRequest() {
    }
}
