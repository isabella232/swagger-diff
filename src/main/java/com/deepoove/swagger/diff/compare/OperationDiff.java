package com.deepoove.swagger.diff.compare;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.ChangedParameter;

import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Response;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

public class OperationDiff {

  private Map<HttpMethod, Operation> increasedOperations;
  private Map<HttpMethod, Operation> missingOperations;
  private Map<HttpMethod, ChangedOperation> changedOperations;

  private Map<HttpMethod, Operation> oldOperations;
  private Map<HttpMethod, Operation> newOperations;
  private Map<String, Model> oldDefinitions;
  private Map<String, Model> newDefinitions;
  private VendorExtensionDiff extDiffer;

  private boolean hasContractChanges;
  private boolean hasOnlyCosmeticChanges;

  public OperationDiff(Map<HttpMethod, Operation> oldOperations, Map<HttpMethod, Operation> newOperations,
                       Map<String, Model> oldDefinitions, Map<String, Model> newDefinitions, VendorExtensionDiff extDiffer) {
    this.increasedOperations = new HashMap<>();
    this.missingOperations = new HashMap<>();
    this.changedOperations = new HashMap<>();
    this.oldOperations = oldOperations;
    this.newOperations = newOperations;
    this.oldDefinitions = oldDefinitions;
    this.newDefinitions = newDefinitions;
    this.extDiffer = extDiffer;
    this.hasContractChanges = false;
    this.hasOnlyCosmeticChanges = false;
  }

  public void diff() {
    MapKeyDiff<HttpMethod, Operation> operationDiff = MapKeyDiff.diff(oldOperations, newOperations);
    Map<HttpMethod, Operation> increasedOperation = operationDiff.getIncreased();
    Map<HttpMethod, Operation> missingOperation = operationDiff.getMissing();
    if (!increasedOperation.isEmpty() || !missingOperation.isEmpty()) {
      this.hasContractChanges = true;
      this.hasOnlyCosmeticChanges = false;
    }

    List<HttpMethod> sharedMethods = operationDiff.getSharedKey();
    ChangedOperation changedOperation = null;

    for (HttpMethod method : sharedMethods) {
      changedOperation = new ChangedOperation();
      Operation oldOperation = oldOperations.get(method);
      Operation newOperation = newOperations.get(method);
      changedOperation.setSummary(newOperation.getSummary());

      changedOperation.setVendorExtsFromGroup(extDiffer.diff(oldOperation, newOperation));

      List<Parameter> oldParameters = oldOperation.getParameters();
      List<Parameter> newParameters = newOperation.getParameters();
      ParameterDiff parameterDiff = new ParameterDiff(oldDefinitions, newDefinitions);
      parameterDiff.diff(oldParameters, newParameters);
      changedOperation.setAddParameters(parameterDiff.getIncreased());
      changedOperation.setMissingParameters(parameterDiff.getMissing());
      changedOperation.setChangedParameters(parameterDiff.getChanged());

      if (!this.hasContractChanges && parameterDiff.hasOnlyCosmeticChanges()) {
        this.hasOnlyCosmeticChanges = true;
      }

      for (ChangedParameter param : parameterDiff.getChanged()) {
        ChangedExtensionGroup paramExtDiff = extDiffer.diff(param.getLeftParameter(), param.getRightParameter());
        param.setVendorExtsFromGroup(paramExtDiff);
        if (paramExtDiff.vendorExtensionsAreDiff()) {
          this.hasContractChanges = true;
          this.hasOnlyCosmeticChanges = false;
        }
      }

      Property oldResponseProperty = getResponseProperty(oldOperation);
      Property newResponseProperty = getResponseProperty(newOperation);
      PropertyDiff propertyDiff = new PropertyDiff(oldDefinitions, newDefinitions);
      propertyDiff.diff(oldResponseProperty, newResponseProperty);
      changedOperation.setAddProps(propertyDiff.getIncreased());
      changedOperation.setMissingProps(propertyDiff.getMissing());
      changedOperation.setChangedProps(propertyDiff.getChanged());

      changedOperation.setChangeResponseDescription(operationResponseHasCosmeticChanges(oldOperation.getResponses().values(), newOperation.getResponses().values()));
      changedOperation.setChangeDescription(diffOperationDescription(oldOperation.getDescription(), newOperation.getDescription()));
      changedOperation.setChangeSummary(diffOperationSummary(oldOperation.getSummary(), newOperation.getSummary()));

      ChangedExtensionGroup responseExtDiff = extDiffer.diffResGroup(oldOperation.getResponses(), newOperation.getResponses());
      changedOperation.putSubGroup("responses", responseExtDiff);
      if (responseExtDiff.vendorExtensionsAreDiff()) {
        this.hasContractChanges = true;
        this.hasOnlyCosmeticChanges = false;
      }

      if (changedOperation.isDiff()) {
        this.changedOperations.put(method, changedOperation);
      }
      if (!this.hasContractChanges && changedOperation.hasOnlyCosmeticChanges()) {
        this.hasOnlyCosmeticChanges = true;
      }
    }
  }

  private static boolean diffOperationSummary(String oldSummary, String newSummary) {
    return ((oldSummary == null) ^ (newSummary == null)) || ((oldSummary != null) && !oldSummary.equals(newSummary));
  }

  private static boolean diffOperationDescription(String oldDescription, String newDescription) {
    return ((oldDescription == null) ^ (newDescription == null)) || ((oldDescription != null) && !oldDescription.equals(newDescription));
  }

  private static boolean operationResponseHasCosmeticChanges(Collection<Response> oldResponses, Collection<Response> newResponses) {
    Iterator<Response> oldOpResponseIterator = oldResponses.iterator();
    Iterator<Response> newOpResponseIterator = newResponses.iterator();
    boolean isChangeDescription = false;

    while (oldOpResponseIterator.hasNext() && newOpResponseIterator.hasNext()) {
      String oldDescription = oldOpResponseIterator.next().getDescription();
      String newDescription = newOpResponseIterator.next().getDescription();
      if (((oldDescription == null) ^ (newDescription == null)) || ((oldDescription != null) && !oldDescription.equals(newDescription))) {
        isChangeDescription = true;
      }
    }

    return isChangeDescription;
  }

  private static Property getResponseProperty(Operation operation) {
    Map<String, Response> responses = operation.getResponses();
    // temporary workaround for missing response messages
    if (responses == null) {
      return null;
    }
    Response response = responses.get("200");
    return null == response ? null : response.getSchema();
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
}
