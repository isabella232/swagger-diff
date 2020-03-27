package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
public class SpecificationDiff extends ChangedExtensionGroup {

  private List<Endpoint> newEndpoints;
  private List<Endpoint> missingEndpoints;
  private List<ChangedEndpoint> changedEndpoints;
  private boolean hasContractChanges;
  private boolean hasOnlyCosmeticChanges;

  private SpecificationDiff() {
    newEndpoints = new ArrayList<>();
    missingEndpoints = new ArrayList<>();
    changedEndpoints = new ArrayList<>();
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

    ChangedExtensionGroup specExtDiff = extDiffer.diff(oldSpec, newSpec);
    instance.setVendorExtsFromGroup(specExtDiff);
    checkVendorExtsDiff(instance, specExtDiff);

    Info oldInfo = oldSpec.getInfo();
    Info newInfo = newSpec.getInfo();
    ChangedExtensionGroup infoExtDiff = extDiffer.diff(oldInfo, newInfo);
    instance.putSubGroup("info", infoExtDiff);
    checkVendorExtsDiff(instance, infoExtDiff);

    if (!instance.hasContractChanges && infoHasChanges(oldInfo, newInfo)) {
      instance.hasOnlyCosmeticChanges = true;
    }

    List<Tag> oldTags = oldSpec.getTags();
    List<Tag> newTags = newSpec.getTags();
    ChangedExtensionGroup tagExtDiff = extDiffer.diffTagGroup(mapTagsByName(oldTags), mapTagsByName(newTags));
    instance.putSubGroup("tags", tagExtDiff);
    checkVendorExtsDiff(instance, tagExtDiff);

    if (!instance.hasContractChanges && tagsHaveChanges(oldTags, newTags)) {
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
      checkVendorExtsDiff(instance, pathExtDiff);

      // TODO: Operation diff
      OperationDiff operationDiff = new OperationDiff(oldPath.getOperationMap(), newPath.getOperationMap(), oldSpec.getDefinitions(), newSpec.getDefinitions(), extDiffer);
      operationDiff.diff();
      changedEndpoint.setNewOperations(operationDiff.getIncreasedOperation());
      changedEndpoint.setMissingOperations(operationDiff.getMissingOperation());
      changedEndpoint.setChangedOperations(operationDiff.getChangedOperations());
      if (operationDiff.hasOnlyCosmeticChanges() && !instance.hasContractChanges) {
        instance.hasOnlyCosmeticChanges = true;
      }

      changedEndpoint.setNewOperations(operationDiff.getIncreasedOperation());
      changedEndpoint.setMissingOperations(operationDiff.getMissingOperation());
      changedEndpoint.setChangedOperations(operationDiff.getChangedOperations());

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
    checkVendorExtsDiff(instance, securityExtDiff);

    return instance;
  }

  private static boolean tagsHaveChanges(List<Tag> oldTags, List<Tag> newTags) {
    Iterator<Tag> oldTagIterator = oldTags.iterator();
    Iterator<Tag> newTagIterator = newTags.iterator();
    boolean hasChanges = false;

    while (oldTagIterator.hasNext() && newTagIterator.hasNext()) {
      Tag oldTag = oldTagIterator.next();
      Tag newTag = newTagIterator.next();
      hasChanges |= !((oldTag.getDescription() == null && newTag.getDescription() == null || oldTag.getDescription().equals(newTag.getDescription())) &&
          (oldTag.getName() == null && newTag.getName() == null || oldTag.getName().equals(newTag.getName())) &&
          (oldTag.getExternalDocs() == null && newTag.getExternalDocs() == null || oldTag.getExternalDocs().equals(newTag.getExternalDocs())));
    }

    return hasChanges;
  }

  private static boolean infoHasChanges(Info oldInfo, Info newInfo) {
    return !((oldInfo.getDescription() == null && newInfo.getDescription() == null || oldInfo.getDescription().equals(newInfo.getDescription())) &&
        (oldInfo.getVersion() == null && newInfo.getVersion() == null || oldInfo.getVersion().equals(newInfo.getVersion())) &&
        (oldInfo.getTitle() == null && newInfo.getTitle() == null || oldInfo.getTitle().equals(newInfo.getTitle())) &&
        (oldInfo.getContact() == null && newInfo.getContact() == null || oldInfo.getContact().equals(newInfo.getContact())) &&
        (oldInfo.getLicense() == null && newInfo.getLicense() == null || oldInfo.getLicense().equals(newInfo.getLicense())) &&
        (oldInfo.getTermsOfService() == null && newInfo.getTermsOfService() == null || oldInfo.getTermsOfService().equals(newInfo.getTermsOfService())));
  }

  public static void checkVendorExtsDiff(SpecificationDiff diff, ChangedExtensionGroup extDiff) {
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
