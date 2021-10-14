/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openmetadata.catalog.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openmetadata.common.utils.CommonUtil;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

public final class RestUtil {
  public static final DateFormat DATE_TIME_FORMAT;
  public static final DateFormat DATE_FORMAT;
  private static final Logger LOG = LoggerFactory.getLogger(RestUtil.class);

  static {
    // Quoted "Z" to indicate UTC, no timezone offset
    DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
    DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

    DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private RestUtil() {

  }

  /**
   * Remove leading and trailing slashes
   */
  public static String removeSlashes(String s) {
    s = s.startsWith("/") ? s.substring(1) : s;
    s = s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    return s;
  }

  public static URI getHref(UriInfo uriInfo, String collectionPath, UUID id) {
    collectionPath = removeSlashes(collectionPath);
    try {
      String uriPath = uriInfo.getBaseUri() + collectionPath + "/" + id;
      return URI.create(uriPath);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static URI getHref(UriInfo uriInfo, String collectionPath) {
    collectionPath = removeSlashes(collectionPath);
    String uriPath = uriInfo.getBaseUri() + collectionPath;
    return URI.create(uriPath);
  }

  public static URI getHref(UriInfo uriInfo, String collectionPath, String resourcePath) {
    collectionPath = removeSlashes(collectionPath);
    resourcePath = removeSlashes(resourcePath);
    try {
      String uriPath = uriInfo.getBaseUri() + collectionPath + "/" + resourcePath;
      return URI.create(uriPath);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static URI getHref(URI parent, String child) {
    return URI.create(parent.toString() + "/" + child);
  }

  /**
   * Get list of attributes for an entity based on JsonProperty annotation
   */
  public static <T> List<String> getAttributes(Class<T> clz) {
    List<String> attributes = new ArrayList<>();
    for (Field field : ReflectionUtils.getFields(clz, ReflectionUtils.withAnnotation(JsonProperty.class))) {
      // Attributes are fields that are not of entity type
      if (!field.getType().getName().contains(".entity.")) {
        attributes.add(field.getName());
      }
    }
    return attributes;
  }

  /**
   * Get list of relationships for an entity based on JsonProperty annotation
   */
  public static <T> List<String> getRelationships(Class<T> clz) {
    List<String> relationships = new ArrayList<>();
    for (Field field : ReflectionUtils.getFields(clz, ReflectionUtils.withAnnotation(JsonProperty.class))) {
      // Relationships are fields that are of entity type
      if (field.getType().getName().contains(".entity.")) {
        relationships.add(field.getName());
      }
    }
    return relationships;
  }

  public static int compareDates(String date1, String date2) throws ParseException {
    return DATE_FORMAT.parse(date1).compareTo(DATE_FORMAT.parse(date2));
  }

  public static String today(int offsetDays) throws ParseException {
    Date date = CommonUtil.getDateByOffset(new Date(), offsetDays);
    return DATE_FORMAT.format(date);
  }

  public static void validateCursors(String before, String after) {
    if (before != null && after != null) {
      throw new IllegalArgumentException("Only one of before or after query parameter allowed");
    }
  }

  public static class PutResponse<T> {

    private final T entity;
    private final Response.Status status;

    /**
     * Response.Status.CREATED when PUT operation creates a new entity
     * or Response.Status.OK when PUT operation updates a new entity
     */
    public PutResponse(Response.Status status, T entity) {
      this.entity = entity;
      this.status = status;
    }

    public T getEntity() {
      return entity;
    }

    public Status getStatus() {
      return status;
    }
  }
}