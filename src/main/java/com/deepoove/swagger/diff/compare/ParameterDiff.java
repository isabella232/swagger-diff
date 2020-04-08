package com.deepoove.swagger.diff.compare;

import java.util.ArrayList;
import java.util.Collections;
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

  private Map<String, Model> oldDefinitions;
  private Map<String, Model> newDefinitions;

  private ParameterDiff(Map<String, Model> left, Map<String, Model> right) {
    this.oldDefinitions = left;
    this.newDefinitions = right;
  }

  public static ParameterDiff build(Map<String, Model> left, Map<String, Model> right) {
    return new ParameterDiff(left, right);
  }

  public ParameterDiffResult diff(List<Parameter> left, List<Parameter> right) {
    ParameterDiffResult parameterDiffResult = new ParameterDiffResult();

    if (null == left) {
      left = new ArrayList<>();
    }
    if (null == right) {
      right = new ArrayList<>();
    }

    parameterDiffResult.addIncreased(right);
    for (Parameter leftPara : left) {
      String name = leftPara.getName();
      int index = index(parameterDiffResult.getIncreased(), name);
      if (-1 == index) {
        parameterDiffResult.addMissing(Collections.singleton(leftPara));
      } else {
        Parameter rightPara = parameterDiffResult.getIncreased().get(index);
        parameterDiffResult.removeIncreased(index);

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
            ModelDiffResult modelDiff = ModelDiff.build(oldDefinitions, newDefinitions).diff(leftModel, rightModel, aRef);
            changedParameter.setIncreased(modelDiff.getIncreased());
            changedParameter.setMissing(modelDiff.getMissing());
            changedParameter.setChanged(modelDiff.getChanged());
            parameterDiffResult.setHasOnlyCosmeticChanges(modelDiff.hasOnlyCosmeticChanges());
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

        if (changedParameter.isDiff()) { //&& !changedParameter.hasOnlyCosmeticChanges()) {
          parameterDiffResult.addChanged(Collections.singleton(changedParameter));
        }
      }
    }

    return parameterDiffResult;
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
}
