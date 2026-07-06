package com.anti.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Agent 检索来源。
 */
@Data
public class SourceVO implements Serializable {

    private String title;

    private String url;

    private String domain;

    private String snippet;

    private String content;

    private String publishedAt;

    private static final long serialVersionUID = 1L;
}
