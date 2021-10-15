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

import org.openmetadata.catalog.resources.tags.TagResourceTest;
import org.openmetadata.catalog.security.CatalogOpenIdAuthorizationRequestFilter;
import org.openmetadata.catalog.type.EntityReference;
import org.openmetadata.catalog.type.JdbcInfo;
import org.apache.http.client.HttpResponseException;
import org.eclipse.jetty.http.HttpStatus;
import org.openmetadata.catalog.type.TagLabel;

import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TestUtils {
  // Entity name length allowed is 64 characters. This is a 65 char length invalid entity name
  public static final String LONG_ENTITY_NAME = "012345678901234567890123456789012345678901234567890123456789012345";
  public static final UUID NON_EXISTENT_ENTITY = UUID.randomUUID();
  public static JdbcInfo JDBC_INFO;
  public static URI DASHBOARD_URL;
  public static URI PIPELINE_URL;

  public enum UpdateType {
    NO_CHANGE,    // PUT/PATCH made no change
    MINOR_UPDATE, // PUT/PATCH made backward compatible minor version change
    MAJOR_UPDATE    // PUT/PATCH made backward incompatible minor version change
  }

  static {
    JDBC_INFO = new JdbcInfo().withConnectionUrl("scheme://user_name:password#_@%:localhost:1000/test")
            .withDriverClass("driverClass");
  }

  static {
    try {
      DASHBOARD_URL = new URI("http://localhost:8088");
    } catch (URISyntaxException e) {
      DASHBOARD_URL = null;
      e.printStackTrace();
    }
  }

  static {
    try {
      PIPELINE_URL = new URI("http://localhost:8080");
    } catch (URISyntaxException e) {
      PIPELINE_URL = null;
      e.printStackTrace();
    }
  }

  private TestUtils() {
  }

  public static Builder addHeaders(WebTarget target, Map<String, String> headers) {
    if (headers != null) {
      return target.request().header(CatalogOpenIdAuthorizationRequestFilter.X_AUTH_PARAMS_EMAIL_HEADER,
              headers.get(CatalogOpenIdAuthorizationRequestFilter.X_AUTH_PARAMS_EMAIL_HEADER));
    }
    return target.request();
  }

  public static void readResponseError(Response response) throws HttpResponseException {
    JsonObject error = response.readEntity(JsonObject.class);
    throw new HttpResponseException(error.getInt("code"), error.getString("message"));
  }

  public static void readResponse(Response response, int expectedResponse) throws HttpResponseException {
    if (!HttpStatus.isSuccess(response.getStatus())) {
      readResponseError(response);
    }
    assertEquals(expectedResponse, response.getStatus());
  }

  public static <T> T readResponse(Response response, Class<T> clz, int expectedResponse) throws HttpResponseException {
    if (!HttpStatus.isSuccess(response.getStatus())) {
      readResponseError(response);
    }
    assertEquals(expectedResponse, response.getStatus());
    return response.readEntity(clz);
  }

  public static void assertResponse(HttpResponseException exception, Status expectedCode, String expectedReason) {
    assertEquals(expectedCode.getStatusCode(), exception.getStatusCode());
    assertEquals(expectedReason, exception.getReasonPhrase());
  }

  public static void assertResponseContains(HttpResponseException exception, Status expectedCode,
                                            String expectedReason) {
    assertEquals(expectedCode.getStatusCode(), exception.getStatusCode());
    assertTrue(exception.getReasonPhrase().contains(expectedReason),
            expectedReason + " not in actual " + exception.getReasonPhrase());
  }

  public static <T> void assertEntityPagination(List<T> allEntities, ResultList<T> actual, int limit, int offset) {
    assertFalse(actual.getData().isEmpty());
    if (actual.getPaging().getAfter() != null && actual.getPaging().getBefore() != null) {
      // Last page may have less than limit number of records
      assertEquals(limit, actual.getData().size());
    }
    for (int i = 0; i < actual.getData().size(); i++) {
      assertEquals(allEntities.get(offset + i), actual.getData().get(i));
    }
    // Ensure total count returned in paging is correct
    assertEquals(allEntities.size(), actual.getPaging().getTotal());
  }

  public static void post(WebTarget target, Map<String, String> headers) throws HttpResponseException {
    Response response = addHeaders(target, headers).post(null);
    readResponse(response, Status.CREATED.getStatusCode());
  }

  public static <K> void post(WebTarget target, K request, Map<String, String> headers) throws HttpResponseException {
    Response response = addHeaders(target, headers).post(Entity.entity(request, MediaType.APPLICATION_JSON));
    readResponse(response, Status.CREATED.getStatusCode());
  }

  public static <T, K> T post(WebTarget target, K request, Class<T> clz, Map<String, String> headers)
          throws HttpResponseException {
    Response response = addHeaders(target, headers).post(Entity.entity(request, MediaType.APPLICATION_JSON));
    return readResponse(response, clz, Status.CREATED.getStatusCode());
  }

  public static <T> T patch(WebTarget target, JsonPatch patch, Class<T> clz, Map<String, String> headers)
          throws HttpResponseException {
    Response response = addHeaders(target, headers).method("PATCH",
            Entity.entity(patch.toJsonArray().toString(), MediaType.APPLICATION_JSON_PATCH_JSON_TYPE));
    return readResponse(response, clz, Status.OK.getStatusCode());
  }

  public static <K> void put(WebTarget target, K request, Status expectedStatus,  Map<String, String> headers)
          throws HttpResponseException {
    Response response = addHeaders(target, headers).method("PUT", Entity.entity(request,
            MediaType.APPLICATION_JSON));
    readResponse(response, expectedStatus.getStatusCode());
  }

  public static <T, K> T put(WebTarget target, K request, Class<T> clz, Status expectedStatus,
                             Map<String, String> headers)
          throws HttpResponseException {
    Response response = addHeaders(target, headers).method("PUT", Entity.entity(request,
            MediaType.APPLICATION_JSON));
    return readResponse(response, clz, expectedStatus.getStatusCode());
  }

  public static <T> T get(WebTarget target, Class<T> clz, Map<String, String> headers) throws HttpResponseException {
    final Response response = addHeaders(target, headers).get();
    return readResponse(response, clz, Status.OK.getStatusCode());
  }

  public static void delete(WebTarget target, Map<String, String> headers) throws HttpResponseException {
    final Response response = addHeaders(target, headers).delete();
    if (!HttpStatus.isSuccess(response.getStatus())) {
      readResponseError(response);
    }
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  public static void validateEntityReference(EntityReference ref) {
    assertNotNull(ref.getId());
    assertNotNull(ref.getHref());
    assertNotNull(ref.getName());
    assertNotNull(ref.getType());
    // Ensure data entities use fully qualified name
    if (List.of("table", "database", "metrics", "dashboard", "pipeline", "report", "topic", "chart")
            .contains(ref.getType())) {
      // FullyQualifiedName has "." as separator
      assertTrue(ref.getName().contains("."), "entity name is not fully qualified - " + ref.getName());
    }
  }

  public static void validateEntityReference(List<EntityReference> list) {
    Optional.ofNullable(list).orElse(Collections.emptyList()).forEach(TestUtils::validateEntityReference);
  }

  public static Map<String, String> authHeaders(String username) {
    Map<String, String> headers = new HashMap<>();
    if (username != null) {
      headers.put(CatalogOpenIdAuthorizationRequestFilter.X_AUTH_PARAMS_EMAIL_HEADER, username);
    }
    return headers;
  }

  public static void validateTags(String fqn, List<TagLabel> expectedList, List<TagLabel> actualList)
          throws HttpResponseException {
    if (expectedList == null) {
      return;
    }
    actualList = Optional.ofNullable(actualList).orElse(Collections.emptyList());
    // When tags from the expected list is added to an entity, the derived tags for those tags are automatically added
    // So add to the expectedList, the derived tags before validating the tags
    List<TagLabel> updatedExpectedList = new ArrayList<>(expectedList);
    for (TagLabel expected : expectedList) {
      List<TagLabel> derived = EntityUtil.getDerivedTags(expected, TagResourceTest.getTag(expected.getTagFQN(),
              adminAuthHeaders()));
      updatedExpectedList.addAll(derived);
    }
    updatedExpectedList = updatedExpectedList.stream().distinct().collect(Collectors.toList());
    updatedExpectedList.sort(Comparator.comparing(TagLabel::getTagFQN));
    actualList.sort(Comparator.comparing(TagLabel::getTagFQN));

    assertEquals(updatedExpectedList.size(), actualList.size(), fqn);
    for (int i = 0; i < actualList.size(); i++) {
      assertEquals(updatedExpectedList.get(i), actualList.get(i));
    }
  }

  public static Map<String, String> adminAuthHeaders() {
    return authHeaders("admin@open-metadata.org");
  }

  public static Map<String, String> userAuthHeaders() {
    return authHeaders("test@open-metadata.org");
  }

  public static String getPrincipal(Map<String, String> authHeaders) {
    // Get user name from the email address
    return authHeaders.get(CatalogOpenIdAuthorizationRequestFilter.X_AUTH_PARAMS_EMAIL_HEADER).split("@")[0];
  }

  public static void validateUpdate(Double previousVersion, Double newVersion, UpdateType updateType) {
    if (updateType == UpdateType.NO_CHANGE) {
      assertEquals(previousVersion, newVersion); // No change in the version
    } else if (updateType == UpdateType.MINOR_UPDATE) {
      assertEquals(previousVersion + 0.1, newVersion); // Minor version change
    } else if (updateType == UpdateType.MAJOR_UPDATE) {
      assertEquals(previousVersion + 1.0, newVersion); // Minor version change
    }
  }
}
