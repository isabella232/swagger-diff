package com.deepoove.swagger.diff.compare;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.ChangedParameter;

import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Response;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

public class OperationsDiff {

  private Map<HttpMethod, Operation> oldOperations;
  private Map<HttpMethod, Operation> newOperations;
  private VendorExtensionDiff extDiffer;
  private ParameterDiff paramDiffer;
  private PropertyDiff propertyDiffer;

  private OperationsDiff(Map<HttpMethod, Operation> oldOperations, Map<HttpMethod, Operation> newOperations,
                         Map<String, Model> oldDefinitions, Map<String, Model> newDefinitions, VendorExtensionDiff extDiffer) {
    this.oldOperations = oldOperations;
    this.newOperations = newOperations;
    this.extDiffer = extDiffer;
    this.paramDiffer = ParameterDiff.build(oldDefinitions, newDefinitions);
    this.propertyDiffer = PropertyDiff.build(oldDefinitions, newDefinitions);
  }

  public static OperationsDiff build(Map<HttpMethod, Operation> oldOperations, Map<HttpMethod, Operation> newOperations,
                                     Map<String, Model> oldDefinitions, Map<String, Model> newDefinitions, VendorExtensionDiff extDiffer) {
    return new OperationsDiff(oldOperations, newOperations, oldDefinitions, newDefinitions, extDiffer);
  }

  public OperationsDiffResult diff() {
    OperationsDiffResult diffResult = new OperationsDiffResult();

    MapKeyDiff<HttpMethod, Operation> operationDiff = MapKeyDiff.diff(oldOperations, newOperations);
    diffResult.setIncreasedOperations(operationDiff.getIncreased());
    diffResult.setMissingOperations(operationDiff.getMissing());
    if (!operationDiff.getIncreased().isEmpty() || !operationDiff.getMissing().isEmpty()) {
      diffResult.setHasContractChanges(true);
      diffResult.setHasOnlyCosmeticChanges(false);
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
      ParameterDiffResult parameterDiff = paramDiffer.diff(oldParameters, newParameters);
      changedOperation.setAddParameters(parameterDiff.getIncreased());
      changedOperation.setMissingParameters(parameterDiff.getMissing());
      changedOperation.setChangedParameters(parameterDiff.getChanged());

      if (!diffResult.hasContractChanges() && parameterDiff.hasOnlyCosmeticChanges()) {
        diffResult.setHasOnlyCosmeticChanges(true);
      }

      for (ChangedParameter param : parameterDiff.getChanged()) {
        ChangedExtensionGroup paramExtDiff = extDiffer.diff(param.getLeftParameter(), param.getRightParameter());
        param.setVendorExtsFromGroup(paramExtDiff);
        if (paramExtDiff.vendorExtensionsAreDiff()) {
          diffResult.setHasContractChanges(true);
          diffResult.setHasOnlyCosmeticChanges(false);
        }
      }

      Property oldResponseProperty = getResponseProperty(oldOperation);
      Property newResponseProperty = getResponseProperty(newOperation);
      PropertyDiffResult propertyDiff = propertyDiffer.diff(oldResponseProperty, newResponseProperty);
      changedOperation.setAddProps(propertyDiff.getIncreased());
      changedOperation.setMissingProps(propertyDiff.getMissing());
      changedOperation.setChangedProps(propertyDiff.getChanged());

      changedOperation.setChangeResponseDescription(operationResponseHasCosmeticChanges(oldOperation.getResponses().values(), newOperation.getResponses().values()));
      changedOperation.setChangeDescription(!Objects.equals(oldOperation.getDescription(), newOperation.getDescription()));
      changedOperation.setChangeSummary(!Objects.equals(oldOperation.getSummary(), newOperation.getSummary()));
      changedOperation.setChangeOperationId(!Objects.equals(oldOperation.getOperationId(), newOperation.getOperationId()));

      ChangedExtensionGroup responseExtDiff = extDiffer.diffResGroup(oldOperation.getResponses(), newOperation.getResponses());
      changedOperation.putSubGroup("responses", responseExtDiff);
      if (responseExtDiff.vendorExtensionsAreDiff()) {
        diffResult.setHasContractChanges(true);
        diffResult.setHasOnlyCosmeticChanges(false);
      }

      if (changedOperation.isDiff()) {
        diffResult.getChangedOperations().put(method, changedOperation);
      }
      if (!diffResult.hasContractChanges() && changedOperation.hasOnlyCosmeticChanges()) {
        diffResult.setHasOnlyCosmeticChanges(true);
      }
    }

    return diffResult;
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
}
