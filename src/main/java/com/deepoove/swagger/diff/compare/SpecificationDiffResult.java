package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.Endpoint;

public class SpecificationDiffResult extends ChangedExtensionGroup {
  private List<Endpoint> newEndpoints;
  private List<Endpoint> missingEndpoints;
  private List<ChangedEndpoint> changedEndpoints;
  private boolean hasContractChanges;
  private boolean hasOnlyCosmeticChanges;

  public SpecificationDiffResult() {
    this.newEndpoints = new ArrayList<>();
    this.missingEndpoints = new ArrayList<>();
    this.changedEndpoints = new ArrayList<>();
    this.hasContractChanges = false;
    this.hasOnlyCosmeticChanges = false;
  }

  public List<Endpoint> getNewEndpoints() {
    return newEndpoints;
  }

  public void setNewEndpoints(List<Endpoint> newEndpoints) {
    this.newEndpoints = newEndpoints;
  }

  public void addNewEndpoints(Collection<? extends Endpoint> newEndpoints) {
    this.newEndpoints.addAll(newEndpoints);
  }

  public List<Endpoint> getMissingEndpoints() {
    return missingEndpoints;
  }

  public void setMissingEndpoints(List<Endpoint> missingEndpoints) {
    this.missingEndpoints = missingEndpoints;
  }

  public void addMissingEndpoints(Collection<? extends Endpoint> missingEndpoints) {
    this.missingEndpoints.addAll(missingEndpoints);
  }

  public List<ChangedEndpoint> getChangedEndpoints() {
    return changedEndpoints;
  }

  public void setChangedEndpoints(List<ChangedEndpoint> changedEndpoints) {
    this.changedEndpoints = changedEndpoints;
  }

  public void addChangedEndpoints(Collection<? extends ChangedEndpoint> changedEndpoints) {
    this.changedEndpoints.addAll(changedEndpoints);
  }

  public boolean hasContractChanges() {
    return hasContractChanges;
  }

  public void setHasContractChanges(boolean hasContractChanges) {
    this.hasContractChanges = hasContractChanges;
  }

  public boolean hasOnlyCosmeticChanges() {
    return hasOnlyCosmeticChanges;
  }

  public void setHasOnlyCosmeticChanges(boolean hasOnlyCosmeticChanges) {
    this.hasOnlyCosmeticChanges = hasOnlyCosmeticChanges;
  }
}
