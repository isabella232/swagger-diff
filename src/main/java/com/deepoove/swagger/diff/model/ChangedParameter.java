package com.deepoove.swagger.diff.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.swagger.models.parameters.Parameter;

public class ChangedParameter extends ChangedExtensionGroup implements Changed {

  private List<ElProperty> increased = new ArrayList<ElProperty>();
  private List<ElProperty> missing = new ArrayList<ElProperty>();
  private List<ElProperty> changed = new ArrayList<ElProperty>();

  private Parameter leftParameter;
  private Parameter rightParameter;

  private boolean isChangeRequired;
  // private boolean isChangeType;
  private boolean isChangeDescription;

  public boolean isChangeRequired() {
    return isChangeRequired;
  }

  public void setChangeRequired(boolean isChangeRequired) {
    this.isChangeRequired = isChangeRequired;
  }

  public boolean isChangeDescription() {
    if (this.isChangeDescription) {
      return true;
    }
    return leftParameter.getDescription() != null && rightParameter.getDescription() != null && !leftParameter.getDescription().equals(rightParameter.getDescription());
  }

  public void setChangeDescription(boolean isChangeDescription) {
    this.isChangeDescription = isChangeDescription;
  }

  public Parameter getLeftParameter() {
    return leftParameter;
  }

  public void setLeftParameter(Parameter leftParameter) {
    this.leftParameter = leftParameter;
  }

  public Parameter getRightParameter() {
    return rightParameter;
  }

  public void setRightParameter(Parameter rightParameter) {
    this.rightParameter = rightParameter;
  }

  public boolean isDiff() {
    return isChangeRequired
        || isChangeDescription()
        || !increased.isEmpty()
        || !missing.isEmpty()
        || !changed.isEmpty()
        || vendorExtensionsAreDiff();
  }

  public boolean isMeaningfulChanges() {
    return !isChangeRequired
        && isChangeDescription()
        && increased.isEmpty()
        && missing.isEmpty()
        && changed.isEmpty()
        && !vendorExtensionsAreDiff();
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

//  public boolean isOnlyCosmeticChanges() {
//    return this.increased.isEmpty() && this.missing.isEmpty() &&
//        !leftParameter.getDescription().equals(rightParameter.getDescription()) &&
//        leftParameter.getAllowEmptyValue().equals(rightParameter.getAllowEmptyValue()) &&
//        leftParameter.getRequired() == rightParameter.getRequired() &&
//        leftParameter.getAccess().equals(rightParameter.getAccess()) &&
//        leftParameter.getIn().equals(rightParameter.getIn()) &&
//        leftParameter.isReadOnly().equals(rightParameter.isReadOnly()) &&
//        leftParameter.getName().equals(rightParameter.getName()) &&
//        leftParameter.getPattern().equals(rightParameter.getPattern()) &&
//        leftParameter.getVendorExtensions().equals(rightParameter.getVendorExtensions());
//  }
}
