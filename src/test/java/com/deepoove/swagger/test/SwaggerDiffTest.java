package com.deepoove.swagger.test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.compare.SpecificationDiffResult;
import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.Endpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.swagger.models.Contact;
import io.swagger.models.HttpMethod;
import io.swagger.models.License;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

public class SwaggerDiffTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new Jdk8Module());

  final String SWAGGER_V2_DOC1 = "petstore_v2_1.json";
  final String SWAGGER_V2_DOC2 = "petstore_v2_2.json";
  final String SWAGGER_V2_EMPTY_DOC = "petstore_v2_empty.json";

  @Test
  public void testEqual() throws IOException {
    SwaggerDiff diff = SwaggerDiff.compareV2(loadSpec(SWAGGER_V2_DOC2), loadSpec(SWAGGER_V2_DOC2), true);
    assertEqual(diff);
  }

  @Test
  public void testNewApi() throws IOException {
    SwaggerDiff diff = SwaggerDiff.compareV2(loadSpec(SWAGGER_V2_EMPTY_DOC), loadSpec(SWAGGER_V2_DOC2), true);
    List<Endpoint> newEndpoints = diff.getNewEndpoints();
    List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
    List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();

    Assert.assertTrue(newEndpoints.size() > 0);
    Assert.assertTrue(missingEndpoints.isEmpty());
    Assert.assertTrue(changedEndPoints.isEmpty());

  }

  @Test
  public void testDeprecatedApi() throws IOException {
    SwaggerDiff diff = SwaggerDiff.compareV2(loadSpec(SWAGGER_V2_DOC1), loadSpec(SWAGGER_V2_EMPTY_DOC), true);
    List<Endpoint> newEndpoints = diff.getNewEndpoints();
    List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
    List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();

    Assert.assertTrue(newEndpoints.isEmpty());
    Assert.assertTrue(missingEndpoints.size() > 0);
    Assert.assertTrue(changedEndPoints.isEmpty());

  }

  private void assertVendorExtensionsAreDiff(ChangedExtensionGroup vendorExtensions) {
    Assert.assertTrue(vendorExtensions.vendorExtensionsAreDiff());
  }

  @Test
  public void testDiff() throws IOException {
    SwaggerDiff diff = SwaggerDiff.compareV2(loadSpec(SWAGGER_V2_DOC1), loadSpec(SWAGGER_V2_DOC2), true);

    ChangedExtensionGroup tlVendorExts = diff.getChangedVendorExtensions();
    assertVendorExtensionsAreDiff(tlVendorExts);
    for (String key : tlVendorExts.getChangedSubGroups().keySet()) {
      assertVendorExtensionsAreDiff(tlVendorExts.getChangedSubGroups().get(key));
    }

    List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
    for (ChangedEndpoint changedEndpoint : changedEndpoints) {
      if (changedEndpoint.getPathUrl().equals("/pet")) {
        assertVendorExtensionsAreDiff(changedEndpoint);

        ChangedOperation changedOperation = changedEndpoint.getChangedOperations().get(HttpMethod.POST);
        assertVendorExtensionsAreDiff(changedOperation);
      }
    }
  }

  @Test
  public void noChanges_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Assert.assertFalse(SwaggerDiff.compareV2(left, right, true).hasOnlyCosmeticChanges());
  }

  @Test
  public void infoCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getInfo().setDescription("DIFF");
    Assert.assertTrue(SwaggerDiff.compareV2(left, right).hasOnlyCosmeticChanges());
    right.getInfo().setVersion("DIFF");
    Assert.assertTrue(SwaggerDiff.compareV2(left, right).hasOnlyCosmeticChanges());
    right.getInfo().setTermsOfService("DIFF");
    Assert.assertTrue(SwaggerDiff.compareV2(left, right).hasOnlyCosmeticChanges());
    right.getInfo().setTitle("DIFF");
    Assert.assertTrue(SwaggerDiff.compareV2(left, right).hasOnlyCosmeticChanges());
    right.getInfo().setContact(new Contact());
    Assert.assertTrue(SwaggerDiff.compareV2(left, right).hasOnlyCosmeticChanges());
    right.getInfo().setLicense(new License());
    Assert.assertTrue(SwaggerDiff.compareV2(left, right).hasOnlyCosmeticChanges());
    right.getInfo().setVendorExtension("x-diff", null);
    Assert.assertFalse(SwaggerDiff.compareV2(left, right, true).hasOnlyCosmeticChanges());
  }

  @Test
  public void addNewEndpoint_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getPaths().put("diff", new Path().get(new Operation()));

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.getNewEndpoints().isEmpty());
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
  }

  @Test
  public void removeEndpoint_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getPaths().remove("/pet");

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.getMissingEndpoints().isEmpty());
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
  }

  @Test
  public void changedEndpoint_changeVendorExtensions_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getPaths().get("/pet").setVendorExtension("x-remove-path-ext", new Object());

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().get(0).getChangedVendorExtensions().isEmpty());
  }

  @Test
  public void changedOperation_changeOperationDescriptionAndSummary_onlyCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    for (Path path : right.getPaths().values()) {
      Operation op = path.getOperations().get(0);
      op.setDescription("DIFF");
      op.setSummary("DIFF");
    }

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertTrue(diff.hasOnlyCosmeticChanges());
    assertEqual(diff);
  }

  @Test
  public void changedOperation_addVendorExtensions_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getPaths().get("/pet").getOperations().get(0).setVendorExtension("x-diff", null);

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(((SpecificationDiffResult) diff.getChangedVendorExtensions()).getChangedEndpoints().isEmpty());
  }

  @Test
  public void changedOperation_changeOperationId_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getPaths().get("/pet").getOperations().get(0).setOperationId("DIFF");

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertTrue(diff.hasOnlyCosmeticChanges());
    assertEqual(diff);
  }

  @Test
  public void changeOperation_changeParameterDescription_onlyCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getPaths().get("/pet").getOperations().get(0).getParameters().get(0).setDescription("DIFF");

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertTrue(diff.hasOnlyCosmeticChanges());
    assertEqual(diff);
  }

  @Test
  public void changeOperation_changeParameterName_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getPaths().get("/pet").getOperations().get(0).getParameters().get(0).setName("DIFF");

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().get(0).getChangedOperations().isEmpty());
  }

  @Test
  public void changeOperation_changeParameterRequired_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getPaths().get("/pet").getOperations().get(0).getParameters().get(0).setRequired(false);

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().get(0).getChangedOperations().isEmpty());
  }

  @Test
  public void changeOperation_changePropertyDescription_onlyCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getDefinitions().get("Pet").getProperties().get("id").setDescription("DIFF");

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertTrue(diff.hasOnlyCosmeticChanges());
    assertEqual(diff);
  }

  @Test
  public void changeOperation_changePropertyAccess_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getDefinitions().get("Pet").getProperties().get("id").setAccess("DIFF");

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().isEmpty());
  }

  @Test
  public void changeOperation_changePropertyAllowEmptyValue_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getDefinitions().get("Pet").getProperties().get("id").setAllowEmptyValue(true);

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().isEmpty());
  }

  @Test
  public void changeOperation_changePropertyName_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getDefinitions().get("Pet").getProperties().get("id").setName("DIFF");

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().isEmpty());
  }

  @Test
  public void changeOperation_changePropertyReadOnly_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getDefinitions().get("Pet").getProperties().get("id").setReadOnly(true);

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().isEmpty());
  }

  @Test
  public void changeOperation_changePropertyRequired_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getDefinitions().get("Pet").getProperties().get("id").setRequired(true);

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().isEmpty());
  }

  @Test
  public void changeOperation_changePropertyTitle_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getDefinitions().get("Pet").getProperties().get("id").setTitle("DIFF");

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().isEmpty());
  }

  @Test
  public void changeOperation_changePropertyExample_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getDefinitions().get("Pet").getProperties().get("id").setExample("DIFF");

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertTrue(diff.hasOnlyCosmeticChanges());
    assertEqual(diff);
  }

  @Test
  public void changeOperation_changePropertyPosition_noCosmeticChanges() throws IOException {
    SwaggerParser swaggerParser = new SwaggerParser();
    Swagger left = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);
    Swagger right = swaggerParser.read(loadSpec(SWAGGER_V2_DOC1), true);

    right.getDefinitions().get("Pet").getProperties().get("id").setPosition(0);

    SwaggerDiff diff = SwaggerDiff.compareV2(left, right, true);
    Assert.assertFalse(diff.hasOnlyCosmeticChanges());
    Assert.assertFalse(diff.getChangedEndpoints().isEmpty());
  }

  private void assertEqual(SwaggerDiff diff) {
    List<Endpoint> newEndpoints = diff.getNewEndpoints();
    List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
    List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
    Assert.assertTrue(newEndpoints.isEmpty());
    Assert.assertTrue(missingEndpoints.isEmpty());
    Assert.assertTrue(changedEndPoints.isEmpty());
  }

  private JsonNode loadSpec(String name) throws IOException {
    URL resource = Thread.currentThread().getContextClassLoader().getResource(name);
    return OBJECT_MAPPER.readTree(resource);
  }

}
