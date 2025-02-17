package org.openmetadata.catalog.resources.dqtests;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openmetadata.catalog.util.TestUtils.ADMIN_AUTH_HEADERS;
import static org.openmetadata.catalog.util.TestUtils.assertResponse;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.client.WebTarget;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openmetadata.catalog.CatalogApplicationTest;
import org.openmetadata.catalog.Entity;
import org.openmetadata.catalog.api.tests.CreateTestCase;
import org.openmetadata.catalog.api.tests.CreateTestSuite;
import org.openmetadata.catalog.resources.EntityResourceTest;
import org.openmetadata.catalog.tests.TestCase;
import org.openmetadata.catalog.tests.TestSuite;
import org.openmetadata.catalog.type.EntityReference;
import org.openmetadata.catalog.util.ResultList;
import org.openmetadata.catalog.util.TestUtils;

public class TestSuiteResourceTest extends EntityResourceTest<TestSuite, CreateTestSuite> {
  public TestSuiteResourceTest() {
    super(
        Entity.TEST_SUITE,
        TestSuite.class,
        TestSuiteResource.TestSuiteList.class,
        "testSuite",
        TestSuiteResource.FIELDS);
    supportsEmptyDescription = false;
    supportsFollowers = false;
    supportsAuthorizedMetadataOperations = false;
    supportsOwner = false;
  }

  public void setupTestSuites(TestInfo test) throws IOException {
    TestSuiteResourceTest testSuiteResourceTest = new TestSuiteResourceTest();
    CreateTestSuite createTestSuite = testSuiteResourceTest.createRequest(test);
    TEST_SUITE1 = testSuiteResourceTest.createAndCheckEntity(createTestSuite, ADMIN_AUTH_HEADERS);
    TEST_SUITE1_REFERENCE = TEST_SUITE1.getEntityReference();
    createTestSuite = testSuiteResourceTest.createRequest("testSuite2");
    TEST_SUITE2 = testSuiteResourceTest.createAndCheckEntity(createTestSuite, ADMIN_AUTH_HEADERS);
    TEST_SUITE2_REFERENCE = TEST_SUITE2.getEntityReference();
  }

  @Test
  void post_testDefinitionWithoutRequiredFields_4xx(TestInfo test) {
    // name is required field
    assertResponse(
        () -> createEntity(createRequest(test).withName(null), ADMIN_AUTH_HEADERS),
        BAD_REQUEST,
        "[name must not be null]");
  }

  @Test
  void put_testCaseResults_200(TestInfo test) throws IOException, ParseException {
    TestCaseResourceTest testCaseResourceTest = new TestCaseResourceTest();
    List<EntityReference> testCases1 = new ArrayList<>();
    List<EntityReference> testCases2 = new ArrayList<>();
    CreateTestSuite createTestSuite1 = createRequest(test);
    TestSuite testSuite1 = createAndCheckEntity(createTestSuite1, ADMIN_AUTH_HEADERS);
    CreateTestSuite createTestSuite2 = createRequest(test.getDisplayName() + UUID.randomUUID());
    TestSuite testSuite2 = createAndCheckEntity(createTestSuite2, ADMIN_AUTH_HEADERS);

    for (int i = 0; i < 5; i++) {
      CreateTestCase createTestCase =
          testCaseResourceTest.createRequest("test_testSuite_1_" + i).withTestSuite(testSuite1.getEntityReference());
      TestCase testCase = testCaseResourceTest.createAndCheckEntity(createTestCase, ADMIN_AUTH_HEADERS);
      testCases1.add(testCase.getEntityReference());
    }

    for (int i = 5; i < 10; i++) {
      CreateTestCase create =
          testCaseResourceTest.createRequest("test_testSuite_2_" + i).withTestSuite(testSuite2.getEntityReference());
      TestCase testCase = testCaseResourceTest.createAndCheckEntity(create, ADMIN_AUTH_HEADERS);
      testCases2.add(testCase.getEntityReference());
    }

    ResultList<TestSuite> actualTestSuites = getTestSuites(10, "*", ADMIN_AUTH_HEADERS);
    verifyTestSuites(actualTestSuites, List.of(createTestSuite1, createTestSuite2));

    for (TestSuite testSuite : actualTestSuites.getData()) {
      if (testSuite.getName().equals(createTestSuite1.getName())) {
        verifyTestCases(testSuite.getTests(), testCases1);
      }
    }
    deleteEntity(testSuite1.getId(), true, false, ADMIN_AUTH_HEADERS);
    assertResponse(
        () -> getEntity(testSuite1.getId(), ADMIN_AUTH_HEADERS),
        NOT_FOUND,
        "testSuite instance for " + testSuite1.getId() + " not found");
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("include", "all");
    TestSuite deletedTestSuite = getEntity(testSuite1.getId(), queryParams, null, ADMIN_AUTH_HEADERS);
    assertEquals(testSuite1.getId(), deletedTestSuite.getId());
    assertEquals(deletedTestSuite.getDeleted(), true);
  }

  public static ResultList<TestSuite> getTestSuites(Integer limit, String fields, Map<String, String> authHeaders)
      throws HttpResponseException {
    WebTarget target = CatalogApplicationTest.getResource("testSuite");
    target = limit != null ? target.queryParam("limit", limit) : target;
    target = target.queryParam("fields", fields);
    return TestUtils.get(target, TestSuiteResource.TestSuiteList.class, authHeaders);
  }

  private void verifyTestSuites(ResultList<TestSuite> actualTestSuites, List<CreateTestSuite> expectedTestSuites)
      throws HttpResponseException {
    Map<String, TestSuite> testSuiteMap = new HashMap<>();
    for (TestSuite result : actualTestSuites.getData()) {
      testSuiteMap.put(result.getName(), result);
    }
    for (CreateTestSuite result : expectedTestSuites) {
      TestSuite storedTestSuite = testSuiteMap.get(result.getName());
      if (storedTestSuite == null) continue;
      validateCreatedEntity(storedTestSuite, result, ADMIN_AUTH_HEADERS);
    }
  }

  private void verifyTestCases(List<EntityReference> actualTestCases, List<EntityReference> expectedTestCases)
      throws HttpResponseException {
    assertEquals(expectedTestCases.size(), actualTestCases.size());
    Map<UUID, EntityReference> testCaseMap = new HashMap<>();
    for (EntityReference result : actualTestCases) {
      testCaseMap.put(result.getId(), result);
    }
    for (EntityReference result : expectedTestCases) {
      EntityReference storedTestCase = testCaseMap.get(result.getId());
      assertEquals(result.getId(), storedTestCase.getId());
      assertEquals(result.getName(), storedTestCase.getName());
      assertEquals(result.getDescription(), storedTestCase.getDescription());
    }
  }

  @Override
  public CreateTestSuite createRequest(String name) {
    return new CreateTestSuite().withName(name).withDescription(name);
  }

  @Override
  public void validateCreatedEntity(TestSuite createdEntity, CreateTestSuite request, Map<String, String> authHeaders)
      throws HttpResponseException {
    assertEquals(request.getName(), createdEntity.getName());
    assertEquals(request.getDescription(), createdEntity.getDescription());
  }

  @Override
  public void compareEntities(TestSuite expected, TestSuite updated, Map<String, String> authHeaders)
      throws HttpResponseException {
    assertEquals(expected.getName(), updated.getName());
    assertEquals(expected.getDescription(), updated.getDescription());
  }

  @Override
  public TestSuite validateGetWithDifferentFields(TestSuite entity, boolean byName) throws HttpResponseException {
    return null;
  }

  @Override
  public void assertFieldChange(String fieldName, Object expected, Object actual) throws IOException {
    if (expected == actual) {
      return;
    }
    assertCommonFieldChange(fieldName, expected, actual);
  }
}
