package com.deepoove.swagger.diff.output;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.del;
import static j2html.TagCreator.div;
import static j2html.TagCreator.document;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.head;
import static j2html.TagCreator.header;
import static j2html.TagCreator.hr;
import static j2html.TagCreator.html;
import static j2html.TagCreator.i;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.li;
import static j2html.TagCreator.link;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.ol;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.title;
import static j2html.TagCreator.ul;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

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
import j2html.tags.ContainerTag;

public class HtmlRender implements Render {

  private String title;
  private String linkCss;

  public HtmlRender() {
    this("Api Change Log", "http://deepoove.com/swagger-diff/stylesheets/demo.css");
  }

  public HtmlRender(String title, String linkCss) {
    this.title = title;
    this.linkCss = linkCss;
  }


  public String render(SwaggerDiff diff) {
    List<Endpoint> newEndpoints = diff.getNewEndpoints();
    ContainerTag olNewEndpoint = olNewEndpoint(newEndpoints);

    List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
    ContainerTag olMissingEndpoint = olMissingEndpoint(missingEndpoints);

    ContainerTag changedSummary = divChangedSummary(diff);

    ContainerTag pVersions = olVersions(diff.getOldVersion(), diff.getNewVersion());

    return renderHtml(olNewEndpoint, olMissingEndpoint, changedSummary, pVersions);
  }

  public String renderHtml(ContainerTag olNew, ContainerTag olMiss, ContainerTag olChanged, ContainerTag olVersions) {
    ContainerTag html = html().attr("lang", "en").with(
        head().with(
            meta().withCharset("utf-8"),
            title(title),
            script(rawHtml("function showHide(id  ) {if(document.getElementById(id).style.display==\'none\'  ) {document.getElementById(id).style.display=\'block\';document.getElementById(\'btn_\'+id).innerHTML=\'&uArr;\';}else{document.getElementById(id).style.display=\'none\';document.getElementById(\'btn_\'+id).innerHTML=\'&dArr;\';}return true;}"))
                .withType("text/javascript"),
            link().withRel("stylesheet").withHref(linkCss)
        ),
        body().with(
            header().with(h1(title)),
            div().withClass("article").with(
                divHeadArticle("Versions", "versions", olVersions),
                divHeadArticle("What's New", "new", olNew),
                divHeadArticle("What's Deprecated", "deprecated", olMiss),
                divHeadArticle("What's Changed", "changed", olChanged)
            )
        )
    );

    return document().render() + html.render();
  }

  private ContainerTag divHeadArticle(final String title, final String type, final ContainerTag ol) {
    return div().with(h2(title).with(a(rawHtml("&uArr;")).withId("btn_" + type).withClass("showhide").withHref("#").attr("onClick", "javascript:showHide('" + type + "');")), hr(), ol);
  }

  private ContainerTag olVersions(String oldVersion, String newVersion) {
    ContainerTag p = p().withId("versions");
    p.withText("Changes from " + oldVersion + " to " + newVersion + ".");
    return p;
  }

  private ContainerTag olNewEndpoint(List<Endpoint> endpoints) {
    if (null == endpoints) {
      return ol().withId("new");
    }
    ContainerTag ol = ol().withId("new");
    for (Endpoint endpoint : endpoints) {
      ol.with(liNewEndpoint(endpoint.getMethod().toString(),
          endpoint.getPathUrl(), endpoint.getSummary()));
    }
    return ol;
  }

  private ContainerTag liNewEndpoint(String method, String path,
                                     String desc) {
    return li().with(span(method).withClass(method)).withText(path + " ")
        .with(span(null == desc ? "" : desc));
  }

  private ContainerTag olMissingEndpoint(List<Endpoint> endpoints) {
    if (null == endpoints) {
      return ol().withId("deprecated");
    }
    ContainerTag ol = ol().withId("deprecated");
    for (Endpoint endpoint : endpoints) {
      ol.with(liMissingEndpoint(endpoint.getMethod().toString(),
          endpoint.getPathUrl(), endpoint.getSummary()));
    }
    return ol;
  }

  private ContainerTag liMissingEndpoint(String method, String path,
                                         String desc) {
    return li().with(span(method).withClass(method),
        del().withText(path)).with(span(null == desc ? "" : " " + desc));
  }

  private ContainerTag divChangedSummary(SwaggerDiff diff) {
    List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
    ContainerTag olChanged = olChanged(changedEndpoints);

    ContainerTag container = div().withId("changed");
    ChangedExtensionGroup group;

    ChangedExtensionGroup topLevelExts = diff.getChangedVendorExtensions();
    if (topLevelExts.vendorExtensionsAreDiffShallow()) {
      container.with(li().withClass("indent"))
          .with(span("Root-Level Extensions").withClass("indent"))
          .with(ulChangedVendorExtList(topLevelExts, true, true));
    }
    if (topLevelExts.hasSubGroup("info")) {
      group = topLevelExts.getSubGroup("info");
      if (group.vendorExtensionsAreDiff()) {
        container.with(li().withClass("indent").withText("Info Extensions")
            .with(ulChangedVendorExtList(group, false, true)));
      }
    }
    if (topLevelExts.hasSubGroup("securityDefinitions")) {
      group = topLevelExts.getSubGroup("securityDefinitions");
      if (group.vendorExtensionsAreDiff()) {
        container.with(li().withClass("indent").withText("Security Definition Extensions"))
            .with(ulChangedVendorExtMap(group, true));
      }
    }
    if (topLevelExts.hasSubGroup("tags")) {
      group = topLevelExts.getSubGroup("tags");
      if (group.vendorExtensionsAreDiff()) {
        container.with(li().withClass("indent").withText("Tag Extensions"))
            .with(ulChangedVendorExtMap(group, true));
      }
    }

    return container.with(olChanged);
  }

  private ContainerTag ulChangedVendorExtMap(ChangedExtensionGroup group, boolean styled) {
    ContainerTag ul = ul().withClasses("indent", iff(styled, "extension-container"));
    ;
    for (Entry<String, ChangedExtensionGroup> entry : group.getChangedSubGroups().entrySet()) {
      if (entry.getValue().vendorExtensionsAreDiff()) {
        ul.with(li().with(h3(entry.getKey()))
            .with(ulChangedVendorExtList(entry.getValue(), true, false)));
      }
    }
    return ul;
  }

  private ContainerTag ulChangedVendorExtList(ChangedExtensionGroup group, boolean indented, boolean styled) {
    ContainerTag ul = ul().withClasses(iff(indented, "indent"), iff(styled, "extension-container"));
    for (ContainerTag li : changedVendorExts(group)) {
      ul.with(li);
    }
    return ul;
  }

  private List<ContainerTag> changedVendorExts(ChangedExtensionGroup group) {
    LinkedList<ContainerTag> list = new LinkedList<ContainerTag>();
    for (String key : group.getIncreasedVendorExtensions().keySet()) {
      list.add(liAddVendorExt(key));
    }
    for (String key : group.getMissingVendorExtensions().keySet()) {
      list.add(liMissingVendorExt(key));
    }
    for (Entry<String, Pair<Object, Object>> entry : group.getChangedVendorExtensions().entrySet()) {
      String key = entry.getKey();
      Object left = entry.getValue().getLeft();
      Object right = entry.getValue().getRight();
      list.add(liChangedVendorExt(key, left, right));
    }
    return list;
  }

  private ContainerTag liAddVendorExt(String key) {
    return li().withText("Add " + key);
  }

  private ContainerTag liMissingVendorExt(String key) {
    return li().withClass("missing").withText("Delete ").with(del(key));
  }

  private ContainerTag liChangedVendorExt(String key, Object oldVal, Object newVal) {
    return li().with(text(key + ": "))
        .with(del(oldVal.toString()))
        .with(text(" -> "))
        .with(i().with(text(newVal.toString())));
  }

  private ContainerTag olChanged(List<ChangedEndpoint> changedEndpoints) {
    if (null == changedEndpoints) {
      return ol();
    }
    ContainerTag ol = ol();
    ContainerTag ulDetail;
    for (ChangedEndpoint changedEndpoint : changedEndpoints) {
      String pathUrl = changedEndpoint.getPathUrl();
      Map<HttpMethod, ChangedOperation> changedOperations = changedEndpoint.getChangedOperations();

      if (changedEndpoint.vendorExtensionsAreDiff()) {
        ulDetail = ul();
        if (changedEndpoint.vendorExtensionsAreDiff()) {
          ulDetail.with(li().with(ulChangedVendorExtList(changedEndpoint, false, false)));
        }
        ol.with(li().withText(pathUrl).with(ulDetail));
      }

      for (Entry<HttpMethod, ChangedOperation> entry : changedOperations.entrySet()) {
        String method = entry.getKey().toString();
        ChangedOperation changedOperation = entry.getValue();
        String desc = changedOperation.getSummary();

        ulDetail = ul().withClass("detail");
        if (changedOperation.vendorExtensionsAreDiff()) {
          ulDetail.with(li().with(ulChangedVendorExtList(changedOperation, false, false)));
        }
        if (changedOperation.isDiffParam()) {
          ulDetail.with(li().with(h3("Parameter")).with(ulParam(changedOperation)));
        }
        if (changedOperation.isDiffProp()) {
          ulDetail.with(li().with(h3("Return Type")).with(ulResponse(changedOperation)));
        }
        if (changedOperation.hasSubGroup("responses")) {
          ChangedExtensionGroup group = changedOperation.getSubGroup("responses");
          if (group.vendorExtensionsAreDiff()) {
            ContainerTag ulResponse = ul().with(li().with(h3("Responses")));
            for (Entry<String, ChangedExtensionGroup> rEntry : group.getChangedSubGroups().entrySet()) {
              ulResponse.with(li().withClass("indent").withText(rEntry.getKey()).with(ulChangedVendorExtList(rEntry.getValue(), true, false)));
            }
            ulDetail.with(ulResponse);
          }
        }
        ol.with(li().with(span(method).withClass(method)).withText(pathUrl + " ").with(span(null == desc ? "" : desc))
            .with(ulDetail));
      }
    }
    return ol;
  }

  private ContainerTag ulResponse(ChangedOperation changedOperation) {
    List<ElProperty> addProps = changedOperation.getAddProps();
    List<ElProperty> delProps = changedOperation.getMissingProps();
    ContainerTag ul = ul().withClass("change response");
    for (ElProperty prop : addProps) {
      ul.with(liAddProp(prop));
    }
    for (ElProperty prop : delProps) {
      ul.with(liMissingProp(prop));
    }
    return ul;
  }

  private ContainerTag liMissingProp(ElProperty prop) {
    Property property = prop.getProperty();
    return li().withClass("missing").withText("Delete").with(del(prop.getEl())).with(span(null == property.getDescription() ? "" : ("//" + property.getDescription())).withClass("comment"));
  }

  private ContainerTag liAddProp(ElProperty prop) {
    Property property = prop.getProperty();
    return li().withText("Add " + prop.getEl()).with(span(null == property.getDescription() ? "" : ("//" + property.getDescription())).withClass("comment"));
  }

  private ContainerTag ulParam(ChangedOperation changedOperation) {
    List<Parameter> addParameters = changedOperation.getAddParameters();
    List<Parameter> delParameters = changedOperation.getMissingParameters();
    List<ChangedParameter> changedParameters = changedOperation.getChangedParameter();
    ContainerTag ul = ul().withClass("change param");
    for (Parameter param : addParameters) {
      ul.with(liAddParam(param));
    }
    for (ChangedParameter param : changedParameters) {
      List<ElProperty> increased = param.getIncreased();
      for (ElProperty prop : increased) {
        ul.with(liAddProp(prop));
      }
    }
    for (ChangedParameter param : changedParameters) {
      boolean changeRequired = param.isChangeRequired();
      boolean changeDescription = param.isChangeDescription();
      boolean changeVendorExtensions = param.vendorExtensionsAreDiff();
      if (changeRequired || changeDescription || changeVendorExtensions) {
        ul.with(liChangedParam(param));
      }
    }
    for (ChangedParameter param : changedParameters) {
      List<ElProperty> missing = param.getMissing();
      for (ElProperty prop : missing) {
        ul.with(liMissingProp(prop));
      }
    }
    for (Parameter param : delParameters) {
      ul.with(liMissingParam(param));
    }
    return ul;
  }

  private ContainerTag liAddParam(Parameter param) {
    return li().withText("Add " + param.getName()).with(span(null == param.getDescription() ? "" : ("//" + param.getDescription())).withClass("comment"));
  }

  private ContainerTag liMissingParam(Parameter param) {
    return li().withClass("missing").with(span("Delete")).with(del(param.getName())).with(span(null == param.getDescription() ? "" : ("//" + param.getDescription())).withClass("comment"));
  }

  private ContainerTag liChangedParam(ChangedParameter changeParam) {
    boolean changeRequired = changeParam.isChangeRequired();
    boolean changeDescription = changeParam.isChangeDescription();
    boolean changeVendorExtensions = changeParam.vendorExtensionsAreDiff();
    Parameter rightParam = changeParam.getRightParameter();
    Parameter leftParam = changeParam.getLeftParameter();
    ContainerTag li = li().withText("Change " + rightParam.getName() + ":");
    ContainerTag ul = ul().withClass("indent");
    if (changeRequired) {
      String newValue = (rightParam.getRequired() ? "required" : "not required");
      String oldValue = (!rightParam.getRequired() ? "required" : "not required");
      ul.with(li().with(del(oldValue)).withText(" -> " + newValue));
    }
    if (changeDescription) {
      ul.with(li().withText("Notes:")
          .with(del(leftParam.getDescription()).withClass("comment"))
          .withText(" -> ")
          .with(span(span(null == rightParam.getDescription() ? "" : rightParam.getDescription()).withClass("comment"))));
    }
    if (changeVendorExtensions) {
      ul.with(li().with(ulChangedVendorExtList(changeParam, false, false)));
    }
    return li.with(ul);
  }

}
