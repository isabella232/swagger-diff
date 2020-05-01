package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.deepoove.swagger.diff.model.ChangedParameter;

import io.swagger.models.parameters.Parameter;

public class ParameterDiffResult {

  private List<Parameter> increased;
  private List<Parameter> missing;
  private List<ChangedParameter> changed;

  private boolean hasOnlyCosmeticChanges;

  public ParameterDiffResult() {
    this.increased = new ArrayList<>();
    this.missing = new ArrayList<>();
    this.changed = new ArrayList<>();
    this.hasOnlyCosmeticChanges = false;
  }

  public List<Parameter> getIncreased() {
    return increased;
  }

  public void setIncreased(List<Parameter> increased) {
    this.increased = increased;
  }

  public void addIncreased(Collection<? extends Parameter> increased) {
    this.increased.addAll(increased);
  }

  public void removeIncreased(int index) {
    this.increased.remove(index);
  }

  public List<Parameter> getMissing() {
    return missing;
  }

  public void setMissing(List<Parameter> missing) {
    this.missing = missing;
  }

  public void addMissing(Collection<? extends Parameter> missing) {
    this.missing.addAll(missing);
  }

  public List<ChangedParameter> getChanged() {
    return changed;
  }

  public void setChanged(List<ChangedParameter> changed) {
    this.changed = changed;
  }

  public void addChanged(Collection<? extends ChangedParameter> changed) {
    this.changed.addAll(changed);
  }

  public boolean hasOnlyCosmeticChanges() {
    return hasOnlyCosmeticChanges;
  }

  public void setHasOnlyCosmeticChanges(boolean hasOnlyCosmeticChanges) {
    this.hasOnlyCosmeticChanges = hasOnlyCosmeticChanges;
  }
}
