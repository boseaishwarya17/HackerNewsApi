package com.hackernews.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HackerNewsItem {
    private Long id;
    private Boolean deleted;
    private String type;
    private String by;
    private Long time;
    private String text;
    private Boolean dead;
    private Long parent;
    private Long poll;
    private List<Long> kids;
    private String url;
    private Integer score;
    private String title;
    private List<Long> parts;
    private Integer descendants;
}