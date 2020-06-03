package com.deepoove.swagger.diff.compare;

import org.junit.Assert;
import org.junit.Test;

import io.swagger.models.Info;

public class SpecificationDiffTest {

  @Test
  public void infoHasChanges_firstInfoNull_returnsTrue() {
    boolean result = SpecificationDiff.infoHasChanges(
        null,
        new Info().description("abc")
    );

    Assert.assertTrue(result);
  }

  @Test
  public void infoHasChanges_secondInfoNull_returnsTrue() {
    boolean result = SpecificationDiff.infoHasChanges(
        null,
        new Info().description("abc")
    );

    Assert.assertTrue(result);
  }

  @Test
  public void infoHasChanges_noDifference_returnsFalse() {
    boolean result = SpecificationDiff.infoHasChanges(
        new Info().description("abc"),
        new Info().description("abc")
    );

    Assert.assertFalse(result);
  }

  @Test
  public void infoHasChanges_withDifference_returnsTrue() {
    boolean result = SpecificationDiff.infoHasChanges(
        new Info().description("abc"),
        new Info().description("def")
    );

    Assert.assertTrue(result);
  }

  @Test
  public void infoHasChanges_withSecondFieldNull_returnsTrue() {
    boolean result = SpecificationDiff.infoHasChanges(
        new Info().description("abc"),
        new Info().description(null)
    );

    Assert.assertTrue(result);
  }

  @Test
  public void infoHasChanges_withFirstFieldNull_returnsTrue() {
    boolean result = SpecificationDiff.infoHasChanges(
        new Info().description(null),
        new Info().description("def")
    );

    Assert.assertTrue(result);
  }
}
