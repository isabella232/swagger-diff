package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.deepoove.swagger.diff.model.ElProperty;

import io.swagger.models.Model;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;

public class PropertyDiff {

  private List<ElProperty> increased;
  private List<ElProperty> missing;
  private List<ElProperty> changed;

  private Map<String, Model> oldDefinitions;
  private Map<String, Model> newDefinitions;

  private boolean hasOnlyCosmeticChanges;

  public PropertyDiff(Map<String, Model> left, Map<String, Model> right) {
    this.oldDefinitions = left;
    this.newDefinitions = right;
    increased = new ArrayList<ElProperty>();
    missing = new ArrayList<ElProperty>();
    changed = new ArrayList<ElProperty>();
    hasOnlyCosmeticChanges = false;
  }

  public void diff(Property left, Property right) {
    if ((null == left || left instanceof RefProperty) && (null == right || right instanceof RefProperty)) {
      Model leftModel = null == left ? null : oldDefinitions.get(((RefProperty) left).getSimpleRef());
      Model rightModel = null == right ? null : newDefinitions.get(((RefProperty) right).getSimpleRef());
      String ref = leftModel != null
          ? ((RefProperty) left).getSimpleRef()
          : right != null
          ? ((RefProperty) right).getSimpleRef()
          : null;
      ModelDiff modelDiff = new ModelDiff(oldDefinitions, newDefinitions);
      modelDiff.diff(leftModel, rightModel, ref);
      increased.addAll(modelDiff.getIncreased());
      missing.addAll(modelDiff.getMissing());
      changed.addAll(modelDiff.getChanged());
      this.hasOnlyCosmeticChanges = modelDiff.hasOnlyCosmeticChanges();
    } else if (left != null && right != null && !left.equals(right)) {
      ElProperty elProperty = new ElProperty();
      elProperty.setEl(String.format("%s -> %s", left.getType(), right.getType()));
      elProperty.setParentModelName("response");
      elProperty.setProperty(left);
      elProperty.setResponseTypeChanged(true);
      changed.add(elProperty);
      if (ModelDiff.propertyHasOnlyCosmeticChanges(left, right)) {
        this.hasOnlyCosmeticChanges = true;
      }
    }
  }

  public List<ElProperty> getIncreased() {
    return increased;
  }

  public void setIncreased(List<ElProperty> increased) {
    this.increased = increased;
  }

  public List<ElProperty> getMissing() {
    return missing;
  }

  public void setMissing(List<ElProperty> missing) {
    this.missing = missing;
  }

  public List<ElProperty> getChanged() {
    return changed;
  }

  public void setChanged(List<ElProperty> changed) {
    this.changed = changed;
  }

  public boolean hasOnlyCosmeticChanges() {
    return hasOnlyCosmeticChanges;
  }

}
