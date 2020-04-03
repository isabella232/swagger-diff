package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.deepoove.swagger.diff.model.ElProperty;

import io.swagger.models.Model;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;

public class PropertyDiff {

  private Map<String, Model> oldDefinitions;
  private Map<String, Model> newDefinitions;

  private PropertyDiff(Map<String, Model> left, Map<String, Model> right) {
    this.oldDefinitions = left;
    this.newDefinitions = right;
  }

  public static PropertyDiff build(Map<String, Model> left, Map<String, Model> right) {
    return new PropertyDiff(left, right);
  }

  public PropertyDiffResult diff(Property left, Property right) {
    PropertyDiffResult diffResult = new PropertyDiffResult();
    if ((null == left || left instanceof RefProperty) && (null == right || right instanceof RefProperty)) {
      Model leftModel = null == left ? null : oldDefinitions.get(((RefProperty) left).getSimpleRef());
      Model rightModel = null == right ? null : newDefinitions.get(((RefProperty) right).getSimpleRef());
      String ref = leftModel != null
          ? ((RefProperty) left).getSimpleRef()
          : right != null
          ? ((RefProperty) right).getSimpleRef()
          : null;
      ModelDiffResult modelDiff = ModelDiff.build(oldDefinitions, newDefinitions).diff(leftModel, rightModel, ref);
      diffResult.addIncreased(modelDiff.getIncreased());
      diffResult.addMissing(modelDiff.getMissing());
      diffResult.addChanged(modelDiff.getChanged());
      diffResult.setHasOnlyCosmeticChanges(modelDiff.hasOnlyCosmeticChanges());
    } else if (left != null && right != null && !left.equals(right)) {
      ElProperty elProperty = new ElProperty();
      elProperty.setEl(String.format("%s -> %s", left.getType(), right.getType()));
      elProperty.setParentModelName("response");
      elProperty.setProperty(left);
      elProperty.setResponseTypeChanged(true);
      diffResult.addChanged(Collections.singleton(elProperty));
      diffResult.setHasOnlyCosmeticChanges(hasOnlyCosmeticChanges(left, right));
    }

    return diffResult;
  }

  public static boolean hasOnlyCosmeticChanges(Property left, Property right) {
    return (areNotEqual(left.getDescription(), right.getDescription()) || areNotEqual(left.getExample(), right.getExample())) &&
        areEqual(left.getAllowEmptyValue(), right.getAllowEmptyValue()) &&
        areEqual(left.getAccess(), right.getAccess()) &&
        areEqual(left.getTitle(), right.getTitle()) &&
        areEqual(left.getReadOnly(), right.getReadOnly()) &&
        areEqual(left.getName(), right.getName()) &&
        areEqual(left.getType(), right.getType()) &&
        areEqual(left.getFormat(), right.getFormat()) &&
        areEqual(left.getVendorExtensions(), right.getVendorExtensions()) &&
        areEqual(left.getPosition(), right.getPosition()) &&
        left.getRequired() == right.getRequired();
  }

  private static boolean areNotEqual(Object left, Object right) {
    return (left == null ^ right == null) || left != null && !left.equals(right);
  }

  private static boolean areEqual(Object left, Object right) {
    return Objects.equals(left, right);
  }
}
