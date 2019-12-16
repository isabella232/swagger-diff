package com.deepoove.swagger.diff.model;

import io.swagger.models.properties.Property;

/**
 * property with expression Language grammar
 * @author Sayi
 */
public class ElProperty extends ChangedExtensionGroup {

  private String el;
  private String parentModelName;
  private Property property;
  private boolean responseTypeChanged;

  public Property getProperty() {
    return property;
  }

  public String getParentModelName() {
    return parentModelName;
  }

  public void setParentModelName(String parentModelName) {
    this.parentModelName = parentModelName;
  }

  public void setProperty(Property property) {
    this.property = property;
  }

  public String getEl() {
    return el;
  }

  public void setEl(String el) {
    this.el = el;
  }

  public boolean getResponseTypeChanged() {
    return responseTypeChanged;
  }

  public void setResponseTypeChanged(boolean responseTypeChanged) {
    this.responseTypeChanged = responseTypeChanged;
  }
}
