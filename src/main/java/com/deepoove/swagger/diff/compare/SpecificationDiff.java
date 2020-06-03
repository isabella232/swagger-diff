package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.Endpoint;

import io.swagger.models.HttpMethod;
import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;

/**
 * compare two Swagger
 *
 * @author Sayi
 */
public class SpecificationDiff {
  private Swagger oldSpec;
  private Swagger newSpec;
  private boolean withExtensions;

  private SpecificationDiff(Swagger oldSpec, Swagger newSpec, boolean withExtensions) {
    this.oldSpec = oldSpec;
    this.newSpec = newSpec;
    this.withExtensions = withExtensions;
  }

  public static SpecificationDiff build(Swagger oldSpec, Swagger newSpec) {
    return SpecificationDiff.build(oldSpec, newSpec, false);
  }

  public static SpecificationDiff build(Swagger oldSpec, Swagger newSpec, boolean withExtensions) {
    return new SpecificationDiff(oldSpec, newSpec, withExtensions);
  }

  public SpecificationDiffResult diff() {
    SpecificationDiffResult specificationDiffResult = new SpecificationDiffResult();
    VendorExtensionDiff extDiffer = new VendorExtensionDiff(withExtensions);
    if (null == oldSpec || null == newSpec) {
      throw new IllegalArgumentException("cannot diff null spec.");
    }
    Map<String, Path> oldPaths = oldSpec.getPaths();
    Map<String, Path> newPaths = newSpec.getPaths();
    MapKeyDiff<String, Path> pathDiff = MapKeyDiff.diff(oldPaths, newPaths);
    specificationDiffResult.setNewEndpoints(convert2EndpointList(pathDiff.getIncreased()));
    specificationDiffResult.setMissingEndpoints(convert2EndpointList(pathDiff.getMissing()));

    ChangedExtensionGroup specExtDiff = extDiffer.diff(oldSpec, newSpec);
    specificationDiffResult.setVendorExtsFromGroup(specExtDiff);
    checkVendorExtsDiff(specificationDiffResult, specExtDiff);

    Info oldInfo = oldSpec.getInfo();
    Info newInfo = newSpec.getInfo();
    ChangedExtensionGroup infoExtDiff = extDiffer.diff(oldInfo, newInfo);
    specificationDiffResult.putSubGroup("info", infoExtDiff);
    checkVendorExtsDiff(specificationDiffResult, infoExtDiff);

    if (!specificationDiffResult.hasContractChanges() && infoHasChanges(oldInfo, newInfo)) {
      specificationDiffResult.setHasOnlyCosmeticChanges(true);
    }

    List<Tag> oldTags = oldSpec.getTags();
    List<Tag> newTags = newSpec.getTags();
    ChangedExtensionGroup tagExtDiff = extDiffer.diffTagGroup(mapTagsByName(oldTags), mapTagsByName(newTags));
    specificationDiffResult.putSubGroup("tags", tagExtDiff);

    List<String> sharedKey = pathDiff.getSharedKey();
    ChangedEndpoint changedEndpoint;
    for (String pathUrl : sharedKey) {
      changedEndpoint = new ChangedEndpoint();
      changedEndpoint.setPathUrl(pathUrl);
      Path oldPath = oldPaths.get(pathUrl);
      Path newPath = newPaths.get(pathUrl);

      ChangedExtensionGroup pathExtDiff = extDiffer.diff(oldPath, newPath);
      changedEndpoint.setVendorExtsFromGroup(pathExtDiff);
      checkVendorExtsDiff(specificationDiffResult, pathExtDiff);

      OperationsDiffResult operationsDiffResult = OperationsDiff.build(oldPath.getOperationMap(),
          newPath.getOperationMap(),
          oldSpec.getDefinitions(),
          newSpec.getDefinitions(),
          extDiffer).diff();
      changedEndpoint.setNewOperations(operationsDiffResult.getIncreasedOperation());
      changedEndpoint.setMissingOperations(operationsDiffResult.getMissingOperation());
      changedEndpoint.setChangedOperations(operationsDiffResult.getChangedOperations());
      if (operationsDiffResult.hasOnlyCosmeticChanges() && !specificationDiffResult.hasContractChanges()) {
        specificationDiffResult.setHasOnlyCosmeticChanges(true);
      } else if (operationsDiffResult.hasContractChanges()) {
        specificationDiffResult.setHasContractChanges(true);
        specificationDiffResult.setHasOnlyCosmeticChanges(false);
      }

      changedEndpoint.setNewOperations(operationsDiffResult.getIncreasedOperation());
      changedEndpoint.setMissingOperations(operationsDiffResult.getMissingOperation());
      changedEndpoint.setChangedOperations(operationsDiffResult.getChangedOperations());

      specificationDiffResult.addNewEndpoints(convert2EndpointList(changedEndpoint.getPathUrl(), changedEndpoint.getNewOperations()));
      specificationDiffResult.addMissingEndpoints(convert2EndpointList(changedEndpoint.getPathUrl(), changedEndpoint.getMissingOperations()));

      if (changedEndpoint.isDiff()) {
        specificationDiffResult.addChangedEndpoints(Collections.singleton(changedEndpoint));
      }
      if (changedEndpoint.hasOnlyCosmeticChanges() && !specificationDiffResult.hasContractChanges()) {
        specificationDiffResult.setHasOnlyCosmeticChanges(true);
      }
    }

    ChangedExtensionGroup securityExtDiff = extDiffer.diffSecGroup(oldSpec.getSecurityDefinitions(), newSpec.getSecurityDefinitions());
    specificationDiffResult.putSubGroup("securityDefinitions", securityExtDiff);
    checkVendorExtsDiff(specificationDiffResult, securityExtDiff);

    return specificationDiffResult;
  }

//  private static boolean tagsHaveChanges(List<Tag> oldTags, List<Tag> newTags) {
//    Iterator<Tag> oldTagIterator = oldTags.iterator();
//    Iterator<Tag> newTagIterator = newTags.iterator();
//    boolean hasChanges = false;
//
//    while (oldTagIterator.hasNext() && newTagIterator.hasNext()) {
//      Tag oldTag = oldTagIterator.next();
//      Tag newTag = newTagIterator.next();
//      hasChanges |= !((oldTag.getDescription() == null && newTag.getDescription() == null || oldTag.getDescription().equals(newTag.getDescription())) &&
//          (oldTag.getName() == null && newTag.getName() == null || oldTag.getName().equals(newTag.getName())) &&
//          (oldTag.getExternalDocs() == null && newTag.getExternalDocs() == null || oldTag.getExternalDocs().equals(newTag.getExternalDocs())));
//    }
//
//    return hasChanges;
//  }

  static boolean infoHasChanges(Info oldInfo, Info newInfo) {
    return hasChanges(oldInfo, newInfo,
            Info::getDescription,
            Info::getVersion,
            Info::getTitle,
            Info::getContact,
            Info::getLicense,
            Info::getTermsOfService
        );
  }

  private static <T, O> boolean hasChanges(T oldItem, T newItem, Function<T, O>... fieldGetters) {
    if (oldItem == null ^ newItem == null) {
      return true;
    }
    for (Function<T, O> fieldGetter : fieldGetters) {
      O oldField = fieldGetter.apply(oldItem);
      O newField = fieldGetter.apply(newItem);
      if ((oldField == null ^ newField == null) || oldField != null && !oldField.equals(newField)) {
        return true;
      }
    }
    return false;
  }

  public static void checkVendorExtsDiff(SpecificationDiffResult diff, ChangedExtensionGroup extDiff) {
    if (extDiff.vendorExtensionsAreDiff()) {
      diff.setHasContractChanges(true);
      diff.setHasOnlyCosmeticChanges(false);
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
}
