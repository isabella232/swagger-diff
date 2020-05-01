package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.deepoove.swagger.diff.model.ElProperty;

import io.swagger.models.Model;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;

/**
 * compare two model
 *
 * @author Sayi
 */
public class ModelDiff {

  private Map<String, Model> oldDefinitions;
  private Map<String, Model> newDefinitions;

  private ModelDiff(Map<String, Model> left, Map<String, Model> right) {
    this.oldDefinitions = left;
    this.newDefinitions = right;
  }

  public static ModelDiff build(Map<String, Model> left, Map<String, Model> right) {
    return new ModelDiff(left, right);
  }

  public ModelDiffResult diff(Model leftModel, Model rightModel) {
    return this.diff(leftModel, rightModel, null, null, new HashSet<Model>());
  }

  public ModelDiffResult diff(Model leftModel, Model rightModel, String parentModel) {
    return this.diff(leftModel, rightModel, null, parentModel, new HashSet<Model>());
  }

  private ModelDiffResult diff(Model leftModel, Model rightModel, String parentEl, String parentModel, Set<Model> visited) {
    ModelDiffResult modelDiffResult = new ModelDiffResult();

    // Stop recursing if both models are null
    // OR either model is already contained in the visiting history
    if ((null == leftModel && null == rightModel) || visited.contains(leftModel) || visited.contains(rightModel)) {
      return modelDiffResult;
    }

    Map<String, Property> leftProperties = null == leftModel ? null : leftModel.getProperties();
    Map<String, Property> rightProperties = null == rightModel ? null : rightModel.getProperties();

    // Diff the properties
    MapKeyDiff<String, Property> propertyDiff = MapKeyDiff.diff(leftProperties, rightProperties);

    modelDiffResult.addIncreased(convert2ElPropertys(propertyDiff.getIncreased(), parentEl, parentModel));
    modelDiffResult.addMissing(convert2ElPropertys(propertyDiff.getMissing(), parentEl, parentModel));

    // Recursively find the diff between properties
    List<String> sharedKey = propertyDiff.getSharedKey();
    for (String key : sharedKey) {
      Property left = leftProperties.get(key);
      Property right = rightProperties.get(key);

      if ((left instanceof RefProperty) && (right instanceof RefProperty)) {
        String leftRef = ((RefProperty) left).getSimpleRef();
        String rightRef = ((RefProperty) right).getSimpleRef();

        diff(oldDefinitions.get(leftRef), newDefinitions.get(rightRef),
            buildElString(parentEl, key), leftRef,
            copyAndAdd(visited, leftModel, rightModel));

      } else if (left != null && right != null && !left.equals(right)) {
        if (modelDiffResult.getIncreased().isEmpty() && modelDiffResult.getMissing().isEmpty()) {
          if (PropertyDiff.hasOnlyCosmeticChanges(left, right) && !modelDiffResult.hasContractChanges()) {
            modelDiffResult.setHasOnlyCosmeticChanges(true);
          } else {
            modelDiffResult.setHasOnlyCosmeticChanges(false);
            modelDiffResult.setHasContractChanges(true);
          }
        } else {
          modelDiffResult.setHasOnlyCosmeticChanges(false);
          modelDiffResult.setHasContractChanges(true);
        }

        // Add a changed ElProperty if not a Reference
        modelDiffResult.addChanged(Collections.singleton(convert2ElProperty(key, parentEl, parentModel, left)));
      }
    }

    return modelDiffResult;
  }

  private Collection<? extends ElProperty> convert2ElPropertys(
      Map<String, Property> propMap, String parentEl, String parentModel) {

    List<ElProperty> result = new ArrayList<ElProperty>();
    if (null == propMap) {
      return result;
    }

    for (Entry<String, Property> entry : propMap.entrySet()) {
      result.add(convert2ElProperty(entry.getKey(), parentEl, parentModel, entry.getValue()));
    }
    return result;
  }

  private String buildElString(String parentEl, String propName) {
    return null == parentEl ? propName : (parentEl + "." + propName);
  }

  private ElProperty convert2ElProperty(String propName, String parentEl, String parentModel, Property property) {
    ElProperty pWithPath = new ElProperty();
    pWithPath.setProperty(property);
    pWithPath.setEl(buildElString(parentEl, propName));
    pWithPath.setParentModelName(parentModel);
    return pWithPath;
  }

  private <T> Set<T> copyAndAdd(Set<T> set, T... add) {
    Set<T> newSet = new HashSet<T>(set);
    newSet.addAll(Arrays.asList(add));
    return newSet;
  }
}
