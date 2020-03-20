package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.ChangedParameter;
import com.deepoove.swagger.diff.model.Endpoint;

import io.swagger.models.HttpMethod;
import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

/**
 * compare two Swagger
 *
 * @author Sayi
 */
public class SpecificationDiff extends ChangedExtensionGroup {

  private List<Endpoint> newEndpoints;
  private List<Endpoint> missingEndpoints;
  private List<ChangedEndpoint> changedEndpoints;
  private boolean hasContractChanges;
  private boolean hasOnlyCosmeticChanges;

  private SpecificationDiff() {
    hasContractChanges = false;
    hasOnlyCosmeticChanges = false;
  }

  public static SpecificationDiff diff(Swagger oldSpec, Swagger newSpec) {
    return diff(oldSpec, newSpec, false);
  }

  public static SpecificationDiff diff(Swagger oldSpec, Swagger newSpec, boolean withExtensions) {
    SpecificationDiff instance = new SpecificationDiff();
    VendorExtensionDiff extDiffer = new VendorExtensionDiff(withExtensions);
    if (null == oldSpec || null == newSpec) {
      throw new IllegalArgumentException("cannot diff null spec.");
    }
    Map<String, Path> oldPaths = oldSpec.getPaths();
    Map<String, Path> newPaths = newSpec.getPaths();
    MapKeyDiff<String, Path> pathDiff = MapKeyDiff.diff(oldPaths, newPaths);
    instance.newEndpoints = convert2EndpointList(pathDiff.getIncreased());
    instance.missingEndpoints = convert2EndpointList(pathDiff.getMissing());
    instance.changedEndpoints = new ArrayList<ChangedEndpoint>();

    ChangedExtensionGroup specExtDiff = extDiffer.diff(oldSpec, newSpec);
    instance.setVendorExtsFromGroup(specExtDiff);
    checkContractChanges(instance, specExtDiff);

    Info oldInfo = oldSpec.getInfo();
    Info newInfo = newSpec.getInfo();
    ChangedExtensionGroup infoExtDiff = extDiffer.diff(oldInfo, newInfo);
    instance.putSubGroup("info", infoExtDiff);
    checkContractChanges(instance, infoExtDiff);

    if (!instance.hasContractChanges && infoHasOnlyCosmeticChanges(oldInfo, newInfo)) {
      instance.hasOnlyCosmeticChanges = true;
    }

    List<Tag> oldTags = oldSpec.getTags();
    List<Tag> newTags = newSpec.getTags();
    ChangedExtensionGroup tagExtDiff = extDiffer.diffTagGroup(mapTagsByName(oldTags), mapTagsByName(newTags));
    instance.putSubGroup("tags", tagExtDiff);
    checkContractChanges(instance, tagExtDiff);

    if (!instance.hasContractChanges && tagsHaveOnlyCosmeticChanges(oldTags, newTags)) {
      instance.hasOnlyCosmeticChanges = true;
    }

    List<String> sharedKey = pathDiff.getSharedKey();
    ChangedEndpoint changedEndpoint = null;
    for (String pathUrl : sharedKey) {
      changedEndpoint = new ChangedEndpoint();
      changedEndpoint.setPathUrl(pathUrl);
      Path oldPath = oldPaths.get(pathUrl);
      Path newPath = newPaths.get(pathUrl);

      ChangedExtensionGroup pathExtDiff = extDiffer.diff(oldPath, newPath);
      changedEndpoint.setVendorExtsFromGroup(pathExtDiff);
      checkContractChanges(instance, pathExtDiff);

      Map<HttpMethod, Operation> oldOperationMap = oldPath.getOperationMap();
      Map<HttpMethod, Operation> newOperationMap = newPath.getOperationMap();
      MapKeyDiff<HttpMethod, Operation> operationDiff = MapKeyDiff.diff(oldOperationMap, newOperationMap);
      Map<HttpMethod, Operation> increasedOperation = operationDiff.getIncreased();
      Map<HttpMethod, Operation> missingOperation = operationDiff.getMissing();
      changedEndpoint.setNewOperations(increasedOperation);
      changedEndpoint.setMissingOperations(missingOperation);
      if (!increasedOperation.isEmpty() || !missingOperation.isEmpty()) {
        instance.hasContractChanges = true;
        instance.hasOnlyCosmeticChanges = false;
      }

      List<HttpMethod> sharedMethods = operationDiff.getSharedKey();
      Map<HttpMethod, ChangedOperation> operas = new HashMap<HttpMethod, ChangedOperation>();
      ChangedOperation changedOperation = null;

      for (HttpMethod method : sharedMethods) {
        changedOperation = new ChangedOperation();
        Operation oldOperation = oldOperationMap.get(method);
        Operation newOperation = newOperationMap.get(method);
        changedOperation.setSummary(newOperation.getSummary());

        changedOperation.setVendorExtsFromGroup(extDiffer.diff(oldOperation, newOperation));

        List<Parameter> oldParameters = oldOperation.getParameters();
        List<Parameter> newParameters = newOperation.getParameters();
        ParameterDiff parameterDiff = ParameterDiff
            .buildWithDefinition(oldSpec.getDefinitions(), newSpec.getDefinitions())
            .diff(oldParameters, newParameters);
        changedOperation.setAddParameters(parameterDiff.getIncreased());
        changedOperation.setMissingParameters(parameterDiff.getMissing());
        changedOperation.setChangedParameters(parameterDiff.getChanged());

        if (!instance.hasContractChanges && parameterDiff.hasOnlyCosmeticChanges()) {
          instance.hasOnlyCosmeticChanges = true;
        }

        for (ChangedParameter param : parameterDiff.getChanged()) {
          ChangedExtensionGroup paramExtDiff = extDiffer.diff(param.getLeftParameter(), param.getRightParameter());
          param.setVendorExtsFromGroup(paramExtDiff);
          if (paramExtDiff.vendorExtensionsAreDiff()) {
            instance.hasContractChanges = true;
            instance.hasOnlyCosmeticChanges = false;
          }
        }

        Property oldResponseProperty = getResponseProperty(oldOperation);
        Property newResponseProperty = getResponseProperty(newOperation);
        PropertyDiff propertyDiff = PropertyDiff.buildWithDefinition(oldSpec.getDefinitions(),
            newSpec.getDefinitions());
        propertyDiff.diff(oldResponseProperty, newResponseProperty);
        changedOperation.setAddProps(propertyDiff.getIncreased());
        changedOperation.setMissingProps(propertyDiff.getMissing());
        changedOperation.setChangedProps(propertyDiff.getChanged());

        changedOperation.setChangeResponseDescription(operationResponseHasCosmeticChanges(oldOperation.getResponses().values(), newOperation.getResponses().values()));
        changedOperation.setChangeDescription(diffOperationDescription(oldOperation.getDescription(), newOperation.getDescription()));
        changedOperation.setChangeSummary(diffOperationSummary(oldOperation.getSummary(), newOperation.getSummary()));

        ChangedExtensionGroup responseExtDiff = extDiffer.diffResGroup(oldOperation.getResponses(), newOperation.getResponses());
        changedOperation.putSubGroup("responses", responseExtDiff);
        checkContractChanges(instance, responseExtDiff);

        if (changedOperation.isDiff()) {
          operas.put(method, changedOperation);
        }
        if (!instance.hasContractChanges && changedOperation.hasOnlyCosmeticChanges()) {
          instance.hasOnlyCosmeticChanges = true;
        }
      }
      changedEndpoint.setChangedOperations(operas);

      instance.newEndpoints
          .addAll(convert2EndpointList(changedEndpoint.getPathUrl(), changedEndpoint.getNewOperations()));
      instance.missingEndpoints
          .addAll(convert2EndpointList(changedEndpoint.getPathUrl(), changedEndpoint.getMissingOperations()));

      if (changedEndpoint.isDiff()) {
        instance.changedEndpoints.add(changedEndpoint);
      }
      if (changedEndpoint.hasOnlyCosmeticChanges() && !instance.hasContractChanges) {
        instance.hasOnlyCosmeticChanges = true;
      }
    }

    ChangedExtensionGroup securityExtDiff = extDiffer.diffSecGroup(oldSpec.getSecurityDefinitions(), newSpec.getSecurityDefinitions());
    instance.putSubGroup("securityDefinitions", securityExtDiff);
    checkContractChanges(instance, securityExtDiff);

    return instance;
  }

  private static boolean tagsHaveOnlyCosmeticChanges(List<Tag> oldTags, List<Tag> newTags) {
    Iterator<Tag> oldTagIterator = oldTags.iterator();
    Iterator<Tag> newTagIterator = newTags.iterator();
    boolean onlyCosmeticChanges = true;
    while (oldTagIterator.hasNext() && newTagIterator.hasNext()) {
      Tag oldTag = oldTagIterator.next();
      Tag newTag = newTagIterator.next();
      onlyCosmeticChanges &= ((oldTag.getDescription() == null ^ newTag.getDescription() == null) || !oldTag.getDescription().equals(newTag.getDescription())) &&
          (oldTag.getName() == null && newTag.getName() == null || oldTag.getName().equals(newTag.getName())) &&
          (oldTag.getExternalDocs() == null && newTag.getExternalDocs() == null || oldTag.getExternalDocs().equals(newTag.getExternalDocs()));
    }

    return onlyCosmeticChanges;
  }

  private static boolean infoHasOnlyCosmeticChanges(Info oldInfo, Info newInfo) {
    return ((oldInfo.getDescription() == null ^ newInfo.getDescription() == null) || !oldInfo.getDescription().equals(newInfo.getDescription())) &&
        (oldInfo.getVersion() == null && newInfo.getVersion() == null || oldInfo.getVersion().equals(newInfo.getVersion())) &&
        (oldInfo.getTitle() == null && newInfo.getTitle() == null || oldInfo.getTitle().equals(newInfo.getTitle())) &&
        (oldInfo.getContact() == null && newInfo.getContact() == null || oldInfo.getContact().equals(newInfo.getContact())) &&
        (oldInfo.getLicense() == null && newInfo.getLicense() == null || oldInfo.getLicense().equals(newInfo.getLicense())) &&
        (oldInfo.getTermsOfService() == null && newInfo.getTermsOfService() == null || oldInfo.getTermsOfService().equals(newInfo.getTermsOfService()));
  }


  private static boolean diffOperationSummary(String oldSummary, String newSummary) {
    return ((oldSummary== null) ^ (newSummary== null)) || ((oldSummary != null) && !oldSummary.equals(newSummary));
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

  private static void checkContractChanges(SpecificationDiff diff, ChangedExtensionGroup extDiff) {
    if (extDiff.vendorExtensionsAreDiff()) {
      diff.hasContractChanges = true;
      diff.hasOnlyCosmeticChanges = false;
    }
  }

  private static Map<String, Tag> mapTagsByName(List<Tag> tags) {
    Map<String, Tag> mappedTags = new LinkedHashMap<String, Tag>();
    if (tags == null) {
      return mappedTags;
    }
    for (Tag tag : tags) {
      mappedTags.put(tag.getName(), tag);
    }
    return mappedTags;
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

  private static List<Endpoint> convert2EndpointList(Map<String, Path> map) {
    List<Endpoint> endpoints = new ArrayList<Endpoint>();
    if (null == map) {
      return endpoints;
    }
    for (Entry<String, Path> entry : map.entrySet()) {
      String url = entry.getKey();
      Path path = entry.getValue();

      Map<HttpMethod, Operation> operationMap = path.getOperationMap();
      for (Entry<HttpMethod, Operation> entryOper : operationMap.entrySet()) {
        HttpMethod httpMethod = entryOper.getKey();
        Operation operation = entryOper.getValue();

        Endpoint endpoint = new Endpoint();
        endpoint.setPathUrl(url);
        endpoint.setMethod(httpMethod);
        endpoint.setSummary(operation.getSummary());
        endpoint.setPath(path);
        endpoint.setOperation(operation);
        endpoints.add(endpoint);
      }
    }
    return endpoints;
  }

  private static Collection<? extends Endpoint> convert2EndpointList(String pathUrl, Map<HttpMethod, Operation> map) {
    List<Endpoint> endpoints = new ArrayList<Endpoint>();
    if (null == map) {
      return endpoints;
    }
    for (Entry<HttpMethod, Operation> entry : map.entrySet()) {
      HttpMethod httpMethod = entry.getKey();
      Operation operation = entry.getValue();
      Endpoint endpoint = new Endpoint();
      endpoint.setPathUrl(pathUrl);
      endpoint.setMethod(httpMethod);
      endpoint.setSummary(operation.getSummary());
      endpoint.setOperation(operation);
      endpoints.add(endpoint);
    }
    return endpoints;
  }

  public List<Endpoint> getNewEndpoints() {
    return newEndpoints;
  }

  public List<Endpoint> getMissingEndpoints() {
    return missingEndpoints;
  }

  public List<ChangedEndpoint> getChangedEndpoints() {
    return changedEndpoints;
  }

  public boolean hasOnlyCosmeticChanges() {
    return hasOnlyCosmeticChanges;
  }
}
