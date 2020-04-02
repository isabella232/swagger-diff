package com.deepoove.swagger.diff.compare;

import java.util.HashMap;
import java.util.Map;

import com.deepoove.swagger.diff.model.ChangedOperation;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;

public class OperationsDiffResult {

  private Map<HttpMethod, Operation> increasedOperations;
  private Map<HttpMethod, Operation> missingOperations;
  private Map<HttpMethod, ChangedOperation> changedOperations;

  private boolean hasContractChanges;
  private boolean hasOnlyCosmeticChanges;

  public OperationsDiffResult() {
    this.increasedOperations = new HashMap<>();
    this.missingOperations = new HashMap<>();
    this.changedOperations = new HashMap<>();
    this.hasContractChanges = false;
    this.hasOnlyCosmeticChanges = false;
  }

  public Map<HttpMethod, Operation> getIncreasedOperation() {
    return this.increasedOperations;
  }

  public Map<HttpMethod, Operation> getMissingOperation() {
    return this.missingOperations;
  }

  public Map<HttpMethod, ChangedOperation> getChangedOperations() {
    return this.changedOperations;
  }

  public boolean hasOnlyCosmeticChanges() {
    return this.hasOnlyCosmeticChanges;
  }

  public boolean hasContractChanges() {
    return hasContractChanges;
  }

  public void setIncreasedOperations(Map<HttpMethod, Operation> increasedOperations) {
    this.increasedOperations = increasedOperations;
  }

  public void setMissingOperations(Map<HttpMethod, Operation> missingOperations) {
    this.missingOperations = missingOperations;
  }

  public void setChangedOperations(Map<HttpMethod, ChangedOperation> changedOperations) {
    this.changedOperations = changedOperations;
  }

  public void setHasContractChanges(boolean hasContractChanges) {
    this.hasContractChanges = hasContractChanges;
  }

  public void setHasOnlyCosmeticChanges(boolean hasOnlyCosmeticChanges) {
    this.hasOnlyCosmeticChanges = hasOnlyCosmeticChanges;
  }
}
