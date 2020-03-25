package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.deepoove.swagger.diff.model.ChangedParameter;

import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;

/**
 * compare two parameter
 *
 * @author Sayi
 */
public class ParameterDiff {

  private List<Parameter> increased;
  private List<Parameter> missing;
  private List<ChangedParameter> changed;

  private Map<String, Model> oldDefinitions;
  private Map<String, Model> newDefinitions;

  private boolean hasOnlyCosmeticChanges;

  public ParameterDiff(Map<String, Model> left, Map<String, Model> right) {
    this.oldDefinitions = left;
    this.newDefinitions = right;
    this.hasOnlyCosmeticChanges = false;
  }

  public void diff(List<Parameter> left,
                   List<Parameter> right) {
    if (null == left) {
      left = new ArrayList<>();
    }
    if (null == right) {
      right = new ArrayList<>();
    }

    this.increased = new ArrayList<>(right);
    this.missing = new ArrayList<>();
    this.changed = new ArrayList<>();
    for (Parameter leftPara : left) {
      String name = leftPara.getName();
      int index = index(this.increased, name);
      if (-1 == index) {
        this.missing.add(leftPara);
      } else {
        Parameter rightPara = this.increased.get(index);
        this.increased.remove(index);

        ChangedParameter changedParameter = new ChangedParameter();
        changedParameter.setLeftParameter(leftPara);
        changedParameter.setRightParameter(rightPara);

        if (leftPara instanceof BodyParameter && rightPara instanceof BodyParameter) {
          BodyParameter leftBodyPara = (BodyParameter) leftPara;
          Model leftSchema = leftBodyPara.getSchema();
          BodyParameter rightBodyPara = (BodyParameter) rightPara;
          Model rightSchema = rightBodyPara.getSchema();
          if (leftSchema instanceof RefModel && rightSchema instanceof RefModel) {
            String leftRef = ((RefModel) leftSchema).getSimpleRef();
            String rightRef = ((RefModel) rightSchema).getSimpleRef();
            Model leftModel = oldDefinitions.get(leftRef);
            Model rightModel = newDefinitions.get(rightRef);
            String aRef = leftRef != null ? leftRef : rightRef;
            ModelDiff modelDiff = new ModelDiff(oldDefinitions, newDefinitions);
            modelDiff.diff(leftModel, rightModel, aRef);
            changedParameter.setIncreased(modelDiff.getIncreased());
            changedParameter.setMissing(modelDiff.getMissing());
            changedParameter.setChanged(modelDiff.getChanged());
            this.hasOnlyCosmeticChanges = modelDiff.hasOnlyCosmeticChanges();
          }
        }

        // is required
        boolean rightRequired = rightPara.getRequired();
        boolean leftRequired = leftPara.getRequired();
        changedParameter.setChangeRequired(leftRequired != rightRequired);

        // description
        String newDescription = rightPara.getDescription();
        String oldDescription = leftPara.getDescription();
        if (StringUtils.isBlank(newDescription)) {
          newDescription = "";
        }
        if (StringUtils.isBlank(oldDescription)) {
          oldDescription = "";
        }
        changedParameter.setChangeDescription(!newDescription.equals(oldDescription));

        if (changedParameter.isDiff()) {
          this.changed.add(changedParameter);
        }
      }
    }
  }

  private static int index(List<Parameter> right, String name) {
    for (int i = 0; i < right.size(); i++) {
      Parameter para = right.get(i);
      if (name.equals(para.getName())) {
        return i;
      }
    }
    return -1;
  }

  public List<Parameter> getIncreased() {
    return increased;
  }

  public void setIncreased(List<Parameter> increased) {
    this.increased = increased;
  }

  public List<Parameter> getMissing() {
    return missing;
  }

  public void setMissing(List<Parameter> missing) {
    this.missing = missing;
  }

  public List<ChangedParameter> getChanged() {
    return changed;
  }

  public void setChanged(List<ChangedParameter> changed) {
    this.changed = changed;
  }

  public boolean hasOnlyCosmeticChanges() {
    return hasOnlyCosmeticChanges;
  }

}
