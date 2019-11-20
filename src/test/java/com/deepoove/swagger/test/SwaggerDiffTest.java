package com.deepoove.swagger.test;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.Endpoint;
import com.deepoove.swagger.diff.output.HtmlRender;
import com.deepoove.swagger.diff.output.MarkdownRender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.swagger.models.HttpMethod;

public class SwaggerDiffTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new Jdk8Module());

  final String SWAGGER_V2_DOC1 = "petstore_v2_1.json";
  final String SWAGGER_V2_DOC2 = "petstore_v2_2.json";
  final String SWAGGER_V2_EMPTY_DOC = "petstore_v2_empty.json";
  final String SWAGGER_V2_HTTP = "http://petstore.swagger.io/v2/swagger.json";

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
    String html = new HtmlRender("Changelog",
        "http://deepoove.com/swagger-diff/stylesheets/demo.css")
            .render(diff);

    try {
      FileWriter fw = new FileWriter(
          "testNewApi.html");
      fw.write(html);
      fw.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
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
    String html = new HtmlRender("Changelog",
        "http://deepoove.com/swagger-diff/stylesheets/demo.css")
            .render(diff);

    try {
      FileWriter fw = new FileWriter(
          "testDeprecatedApi.html");
      fw.write(html);
      fw.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
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
    String html = new HtmlRender("Changelog",
        "src/main/resources/demo.css")
        .render(diff);
    
    try {
      FileWriter fw = new FileWriter(
          "testDiff.html");
      fw.write(html);
      fw.close();
      
    } catch (IOException e) {
      e.printStackTrace();
    }

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
//    Assert.assertFalse(changedEndPoints.isEmpty());
  }

  @Test
  public void testDiffAndMarkdown() throws IOException {
    SwaggerDiff diff = SwaggerDiff.compareV2(loadSpec(SWAGGER_V2_DOC1), loadSpec(SWAGGER_V2_DOC2), true);
    String render = new MarkdownRender().render(diff);
    try {
      FileWriter fw = new FileWriter(
          "testDiff.md");
      fw.write(render);
      fw.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

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
