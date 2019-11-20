package com.deepoove.swagger.diff.output;

import static com.deepoove.swagger.diff.output.MarkdownRenderUtils.buildParentPhrase;
import static com.deepoove.swagger.diff.output.MarkdownRenderUtils.prefix;
import static com.deepoove.swagger.diff.output.MarkdownRenderUtils.sort;
import static com.deepoove.swagger.diff.output.MarkdownRenderUtils.sortedPrefixJoin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.ChangedParameter;
import com.deepoove.swagger.diff.model.ElProperty;
import com.deepoove.swagger.diff.model.Endpoint;

import io.swagger.models.HttpMethod;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

public class MarkdownRender implements Render {

  static final String H6 = "###### ";
  static final String H3 = "### ";
  static final String H2 = "## ";
  static final String BLOCKQUOTE = "> ";
  static final String CODE = "`";
  static final String PRE_CODE = "    ";
  static final String PRE_LI = "    ";
  static final String LI = "* ";
  static final String HR = "---\n\n";

  // Change strings
  static final String DELETE = "Removed ";
  static final String INSERT = "Added   ";
  static final String MODIFY = "Changed ";

  String italic = "_";
  String bold = "__";
  String rightArrow = " &rarr; ";

  public MarkdownRender() {
  }

  public String render(SwaggerDiff diff) {
    List<Endpoint> newEndpoints = diff.getNewEndpoints();
    String olNewEndpoint = olNewEndpoint(newEndpoints);

    List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
    String olMissingEndpoint = olMissingEndpoint(missingEndpoints);

    String olChange = olChangeSummary(diff).replace("\n\n", "\n");

    return renderMarkdown(diff.getOldVersion(), diff.getNewVersion(), olNewEndpoint, olMissingEndpoint, olChange);
  }

  public String renderBasic(SwaggerDiff diff) {
    MarkdownRender renderer = new MarkdownRender();
    renderer.italic = "";
    renderer.bold = "";
    renderer.rightArrow = "->";
    return renderer.render(diff);
  }

  private String renderMarkdown(String oldVersion, String newVersion, String olNew, String olMiss,
                                String olChanged) {
    StringBuffer sb = new StringBuffer();
    sb.append(H2).append("Version " + oldVersion + " to " + newVersion + "\n").append(HR);
    sb.append(H3).append("What's New\n").append(HR)
        .append(olNew).append("\n").append(H3)
        .append("What's Deprecated\n").append(HR)
        .append(olMiss).append("\n").append(H3)
        .append("What's Changed\n").append(HR)
        .append(olChanged);
    return sb.toString();
  }

  private String olNewEndpoint(List<Endpoint> endpoints) {
    if (null == endpoints) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    for (Endpoint endpoint : endpoints) {
      sb.append(liNewEndpoint(endpoint.getMethod().toString(),
          endpoint.getPathUrl(), endpoint.getSummary()));
    }
    return sb.toString();
  }

  private String liNewEndpoint(String method, String path, String desc) {
    StringBuffer sb = new StringBuffer();
    sb.append(LI).append(bold + CODE).append(method).append(CODE + bold)
        .append(" " + path).append(" " + desc + "\n");
    return sb.toString();
  }

  private String olMissingEndpoint(List<Endpoint> endpoints) {
    if (null == endpoints) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    for (Endpoint endpoint : endpoints) {
      sb.append(liNewEndpoint(endpoint.getMethod().toString(),
          endpoint.getPathUrl(), endpoint.getSummary()));
    }
    return sb.toString();
  }

  private String olChangeSummary(SwaggerDiff diff) {
    StringBuffer sb = new StringBuffer();

    ChangedExtensionGroup topLevelExts = diff.getChangedVendorExtensions();
    sb.append(ulChangedVendorExtsDeep(topLevelExts, "")).append("\n");

    List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
    String olChanged = olChanged(changedEndpoints);

    return sb.append(olChanged).toString();
  }

  private String olChanged(List<ChangedEndpoint> changedEndpoints) {
    if (null == changedEndpoints) {
      return "";
    }

    String detailPrefix = PRE_LI;
    String detailTitlePrefix = detailPrefix + LI;
    String operationPrefix = LI + bold + CODE;

    StringBuffer sb = new StringBuffer();
    for (ChangedEndpoint changedEndpoint : changedEndpoints) {
      String pathUrl = changedEndpoint.getPathUrl();
      Map<HttpMethod, ChangedOperation> changedOperations = changedEndpoint
          .getChangedOperations();

      if (changedEndpoint.vendorExtensionsAreDiff()) {
        sb.append(LI).append(pathUrl).append("\n")
            .append(sortedPrefixJoin(ulChangedVendorExts(changedEndpoint), PRE_LI + LI));
      }

      for (Entry<HttpMethod, ChangedOperation> entry : changedOperations.entrySet()) {
        String method = entry.getKey().toString();
        ChangedOperation changedOperation = entry.getValue();
        String desc = changedOperation.getSummary() != null
            ? " - " + changedOperation.getSummary()
            : "";

        StringBuffer ulDetail = new StringBuffer();
        if (changedOperation.vendorExtensionsAreDiff()) {
          ulDetail.append(sortedPrefixJoin(ulChangedVendorExts(changedOperation), detailPrefix + LI));
        }
        if (changedOperation.isDiffParam()) {
          ulDetail.append(ulParam(changedOperation));
        }
        if (changedOperation.isDiffProp()) {
          ulDetail.append(detailTitlePrefix)
              .append(italic).append("Return Type").append(italic)
              .append(ulResponse(changedOperation));
        }
        if (changedOperation.hasSubGroup("responses")) {
          ChangedExtensionGroup group = changedOperation.getSubGroup("responses");
          if (group.vendorExtensionsAreDiff()) {
            ulDetail.append(detailTitlePrefix)
                .append(italic).append("Responses").append(italic).append("\n");
            ulDetail.append(ulChangedVendorExtsDeep(group, PRE_LI + PRE_LI));
          }
        }
        sb.append(operationPrefix).append(method).append(CODE + bold)
            .append(" " + pathUrl + desc + "  \n")
            .append(ulDetail);
      }
    }
    return sb.toString();
  }

  private String ulChangedVendorExtsDeep(ChangedExtensionGroup group, String pre) {
    StringBuffer sb = new StringBuffer();

    if (group.vendorExtensionsAreDiffShallow()) {
      List<String> changedVendorExts = sort(ulChangedVendorExts(group));
      sb.append(sortedPrefixJoin(changedVendorExts, pre + LI));
    }
    for (Entry<String, ChangedExtensionGroup> entry : group.getChangedSubGroups().entrySet()) {
      String key = entry.getKey();
      ChangedExtensionGroup subgroup = entry.getValue();
      if (subgroup.vendorExtensionsAreDiff()) {
        sb.append("\n").append(prefix(key, pre + LI)).append("\n");
        sb.append(ulChangedVendorExtsDeep(subgroup, pre + PRE_LI));
      }
    }

    return sb.toString();
  }

  private List<String> ulChangedVendorExts(ChangedExtensionGroup group) {
    ArrayList<String> lines = new ArrayList<String>();
    for (String key : group.getIncreasedVendorExtensions().keySet()) {
      lines.add(liAddVendorExt(key));
    }
    for (String key : group.getMissingVendorExtensions().keySet()) {
      lines.add(liMissingVendorExt(key));
    }
    for (String key : group.getChangedVendorExtensions().keySet()) {
      lines.add(liChangedVendorExt(key));
    }
    return lines;
  }

  private String ulChangedVendorExts(ChangedExtensionGroup group, String pre) {
    return sortedPrefixJoin(ulChangedVendorExts(group), pre);
  }

  private List<String> ulParamChangedVendorExts(String paramName, ChangedExtensionGroup group) {
    // updateKeysWithParam(paramName, group.getIncreasedVendorExtensions());
    // updateKeysWithParam(paramName, group.getMissingVendorExtensions());
    // updateKeysWithParam(paramName, group.getChangedVendorExtensions());

    return ulChangedVendorExts(group);
  }

  private <V> void updateKeysWithParam(String prepend, Map<String, V> map) {
    for (String key : map.keySet()) {
      V value = map.remove(key);
      map.put(prepend + "." + key, value);
    }
  }

  private String liAddVendorExt(String key) {
    return INSERT + CODE + key + CODE;
  }

  private String liMissingVendorExt(String key) {
    return DELETE + CODE + key + CODE;
  }

  private String liChangedVendorExt(String key) {
    return MODIFY + CODE + key + CODE;
  }

  private String ulResponse(ChangedOperation changedOperation) {
    List<ElProperty> addProps = changedOperation.getAddProps();
    List<ElProperty> delProps = changedOperation.getMissingProps();
    List<ElProperty> changedProps = changedOperation.getChangedProps();
    List<String> propLines = new ArrayList<String>();

    String prefix = PRE_LI + PRE_LI + LI;
    for (ElProperty prop : addProps) {
      propLines.add(liAddProp(prop));
    }
    for (ElProperty prop : delProps) {
      propLines.add(liMissingProp(prop));
    }
    for (ElProperty prop : changedProps) {
      propLines.add(liChangedProp(prop));
      if (prop.vendorExtensionsAreDiff()) {
        propLines.addAll(ulChangedVendorExts(prop));
      }
    }
    return "\n" + sortedPrefixJoin(propLines, prefix);
  }

  private String liMissingProp(ElProperty prop) {
    Property property = prop.getProperty();
    String prefix = DELETE + CODE;
    String desc = " //" + property.getDescription();
    String parentModel = buildParentPhrase(prop.getEl(), prop.getParentModelName());
    String postfix = (null == property.getDescription() ? "" : desc);

    StringBuffer sb = new StringBuffer("");
    sb.append(prefix).append(prop.getEl()).append(CODE).append(parentModel)
        .append(postfix);
    return sb.toString();
  }

  private String liAddProp(ElProperty prop) {
    Property property = prop.getProperty();
    String prefix = INSERT + CODE;
    String desc = " //" + property.getDescription();
    String parentModel = buildParentPhrase(prop.getEl(), prop.getParentModelName());
    String postfix = (null == property.getDescription() ? "" : desc);

    StringBuffer sb = new StringBuffer("");
    sb.append(prefix).append(prop.getEl()).append(CODE).append(parentModel)
        .append(postfix);
    return sb.toString();
  }

  private String liChangedProp(ElProperty prop) {
    Property property = prop.getProperty();
    String prefix = MODIFY + CODE;
    String desc = " //" + property.getDescription();
    String parentModel = buildParentPhrase(prop.getEl(), prop.getParentModelName());
    String postfix = (null == property.getDescription() ? "" : desc);

    StringBuffer sb = new StringBuffer("");
    sb.append(prefix).append(prop.getEl()).append(CODE).append(parentModel)
        .append(postfix);
    return sb.toString();
  }

  private String ulParam(ChangedOperation changedOperation) {
    String typePrefix = PRE_LI + LI;
    String prefix = PRE_LI + typePrefix;

    List<Parameter> addParameters = changedOperation.getAddParameters();
    List<Parameter> delParameters = changedOperation.getMissingParameters();
    List<ChangedParameter> changedParameters = changedOperation.getChangedParameter();
    Map<String, List<String>> paramLineMap = new LinkedHashMap<String, List<String>>();

    StringBuffer sb = new StringBuffer("\n");

    for (Parameter param : addParameters) {
      String in = param.getIn();
      if (!paramLineMap.containsKey(in)) {
        paramLineMap.put(in, new ArrayList<String>());
      }
      paramLineMap.get(in).add(liAddParam(param));
    }
    // Add props and vendor extensions
    for (ChangedParameter param : changedParameters) {
      boolean changeVendorExts = param.vendorExtensionsAreDiff();
      List<ElProperty> increased = param.getIncreased();
      List<ElProperty> missing = param.getMissing();
      List<ElProperty> changed = param.getChanged();
      Parameter left = param.getLeftParameter();
      String in = left.getIn();
      if (!paramLineMap.containsKey(in)) {
        paramLineMap.put(in, new ArrayList<String>());
      }
      for (ElProperty prop : increased) {
        paramLineMap
            .get(left.getIn())
            .add(liAddProp(prop));
      }
      for (ElProperty prop : missing) {
        paramLineMap.get(left.getIn()).add(liMissingProp(prop));
      }
      for (ElProperty prop : changed) {
        paramLineMap.get(left.getIn()).add(liChangedProp(prop));
      }
      if (changeVendorExts) {
        paramLineMap.get(left.getIn())
            .addAll(ulParamChangedVendorExts(left.getName(), param));
      }
    }

    for (Parameter param : delParameters) {
      String in = param.getIn();
      if (!paramLineMap.containsKey(in)) {
        paramLineMap.put(in, new ArrayList<String>());
      }
      paramLineMap.get(param.getIn()).add(liMissingParam(param));
    }

    for (String in : paramLineMap.keySet()) {
      String title = italic + in.substring(0, 1).toUpperCase() + in.substring(1) + " Parameters" + italic;
      sb.append(prefix(title, typePrefix)).append("\n")
          .append(sortedPrefixJoin(paramLineMap.get(in), prefix));

    }

    for (ChangedParameter param : changedParameters) {
      boolean changeRequired = param.isChangeRequired();
      boolean changeDescription = param.isChangeDescription();
      boolean changeVendorExts = param.vendorExtensionsAreDiff();
      if (changeRequired || changeDescription || changeVendorExts) {
        sb.append(liChangedParam(param));
      }
    }
    return sb.toString();
  }

  private String liAddParam(Parameter param) {
    String prefix = INSERT + CODE;
    String desc = " //" + param.getDescription();
    String postfix = CODE +
        (null == param.getDescription() ? "" : desc);

    StringBuffer sb = new StringBuffer("");
    sb.append(prefix).append(param.getName())
        .append(postfix);
    return sb.append("\n").toString();
  }

  private String liMissingParam(Parameter param) {
    StringBuffer sb = new StringBuffer("");
    String prefix = DELETE + CODE;
    String desc = " //" + param.getDescription();
    String postfix = CODE +
        (null == param.getDescription() ? "" : desc);
    sb.append(prefix).append(param.getName())
        .append(postfix);
    return sb.append("\n").toString();
  }

  private String liChangedParam(ChangedParameter changeParam) {
    boolean changeRequired = changeParam.isChangeRequired();
    boolean changeDescription = changeParam.isChangeDescription();
    Parameter rightParam = changeParam.getRightParameter();
    Parameter leftParam = changeParam.getLeftParameter();

    String prefix = PRE_LI + PRE_LI;

    StringBuffer sb = new StringBuffer("");
    if (changeRequired) {
      String oldValue = (rightParam.getRequired() ? "required" : "not required");
      String newValue = (!rightParam.getRequired() ? "required" : "not required");
      sb.append(prefix).append(LI)
          .append(oldValue + " " + rightArrow + " " + newValue + "\n");
    }
    if (changeDescription) {
      sb.append(prefix).append(LI).append("Notes ")
          .append(leftParam.getDescription()).append(rightArrow)
          .append(rightParam.getDescription()).append("\n");
    }
    return sb.append("\n").toString();
  }
}
