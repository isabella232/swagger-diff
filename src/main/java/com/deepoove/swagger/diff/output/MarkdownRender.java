package com.deepoove.swagger.diff.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import com.google.common.base.Joiner;

import io.swagger.models.HttpMethod;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

import static com.deepoove.swagger.diff.output.MarkdownRenderUtils.sort;
import static com.deepoove.swagger.diff.output.MarkdownRenderUtils.prefix;
import static com.deepoove.swagger.diff.output.MarkdownRenderUtils.sortedPrefixJoin;

public class MarkdownRender implements Render {

	final String H6 = "###### ";
	final String H3 = "### ";
	final String H2 = "## ";
	final String BLOCKQUOTE = "> ";
	final String CODE = "`";
	final String PRE_CODE = "    ";
	final String PRE_LI = "    ";
	final String LI = "* ";
	final String HR = "---\n\n";

	// Change strings
	final String DELETE = "Removed ";
	final String INSERT = "Added   ";
	final String MODIFY = "Changed ";

	String IT = "_";
	String BD = "__";
	String RIGHT_ARROW = " &rarr; ";

	public MarkdownRender() {}

	public String render(SwaggerDiff diff) {
		List<Endpoint> newEndpoints = diff.getNewEndpoints();
		String ol_newEndpoint = ol_newEndpoint(newEndpoints);

		List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
		String ol_missingEndpoint = ol_missingEndpoint(missingEndpoints);

		String ol_change = ol_changeSummary(diff).replace("\n\n", "\n");

		return renderMarkdown(diff.getOldVersion(), diff.getNewVersion(), ol_newEndpoint, ol_missingEndpoint, ol_change);
	}

	public String renderBasic(SwaggerDiff diff) {
		MarkdownRender renderer = new MarkdownRender();
		renderer.IT = "";
		renderer.BD = "";
		renderer.RIGHT_ARROW = "->";
		return renderer.render(diff);
	}

	private String renderMarkdown(String oldVersion, String newVersion, String ol_new, String ol_miss,
															 String ol_changed) {
		StringBuffer sb = new StringBuffer();
		sb.append(H2).append("Version " + oldVersion + " to " + newVersion + "\n").append(HR);
		sb.append(H3).append("What's New\n").append(HR)
				.append(ol_new).append("\n").append(H3)
				.append("What's Deprecated\n").append(HR)
				.append(ol_miss).append("\n").append(H3)
				.append("What's Changed\n").append(HR)
				.append(ol_changed);
		return sb.toString();
	}

	private String ol_newEndpoint(List<Endpoint> endpoints) {
		if (null == endpoints) return "";
		StringBuffer sb = new StringBuffer();
		for (Endpoint endpoint : endpoints) {
			sb.append(li_newEndpoint(endpoint.getMethod().toString(),
					endpoint.getPathUrl(), endpoint.getSummary()));
		}
		return sb.toString();
	}

	private String li_newEndpoint(String method, String path, String desc) {
		StringBuffer sb = new StringBuffer();
		sb.append(LI).append(BD + CODE).append(method).append(CODE + BD)
				.append(" " + path).append(" " + desc + "\n");
		return sb.toString();
	}

	private String ol_missingEndpoint(List<Endpoint> endpoints) {
		if (null == endpoints) return "";
		StringBuffer sb = new StringBuffer();
		for (Endpoint endpoint : endpoints) {
			sb.append(li_newEndpoint(endpoint.getMethod().toString(),
					endpoint.getPathUrl(), endpoint.getSummary()));
		}
		return sb.toString();
	}

	private String ol_changeSummary(SwaggerDiff diff) {
		StringBuffer sb = new StringBuffer();

		ChangedExtensionGroup topLevelExts = diff.getChangedVendorExtensions();
		sb.append(ul_changedVendorExtsDeep(topLevelExts, "")).append("\n");

		List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
		String ol_changed = ol_changed(changedEndpoints);

		return sb.append(ol_changed).toString();
	}

	private String ol_changed(List<ChangedEndpoint> changedEndpoints) {
		if (null == changedEndpoints) return "";

		String detailPrefix = PRE_LI;
		String detailTitlePrefix = detailPrefix + LI;
		String operationPrefix = LI + BD + CODE;

		StringBuffer sb = new StringBuffer();
		for (ChangedEndpoint changedEndpoint : changedEndpoints) {
			String pathUrl = changedEndpoint.getPathUrl();
			Map<HttpMethod, ChangedOperation> changedOperations = changedEndpoint
					.getChangedOperations();

			if (changedEndpoint.vendorExtensionsAreDiff()) {
				sb.append(LI).append(pathUrl).append("\n")
					.append(sortedPrefixJoin(ul_changedVendorExts(changedEndpoint), PRE_LI + LI));
			}

			for (Entry<HttpMethod, ChangedOperation> entry : changedOperations.entrySet()) {
				String method = entry.getKey().toString();
				ChangedOperation changedOperation = entry.getValue();
				String desc = changedOperation.getSummary() != null
					? " - " + changedOperation.getSummary()
					: "";

				StringBuffer ul_detail = new StringBuffer();
				if (changedOperation.vendorExtensionsAreDiff()) {
					ul_detail.append(sortedPrefixJoin(ul_changedVendorExts(changedOperation), detailPrefix + LI));
				}
				if (changedOperation.isDiffParam()) {
					ul_detail.append(ul_param(changedOperation));
				}
				if (changedOperation.isDiffProp()) {
					ul_detail.append(detailTitlePrefix)
							.append(IT).append("Return Type").append(IT)
							.append(ul_response(changedOperation));
				}
				if (changedOperation.hasSubGroup("responses")) {
					ChangedExtensionGroup group = changedOperation.getSubGroup("responses");
					if (group.vendorExtensionsAreDiff()) {
						ul_detail.append(detailTitlePrefix)
								.append(IT).append("Responses").append(IT).append("\n");
						ul_detail.append(ul_changedVendorExtsDeep(group, PRE_LI + PRE_LI));
					}
				}
				sb.append(operationPrefix).append(method).append(CODE + BD)
						.append(" " + pathUrl + desc + "  \n")
						.append(ul_detail);
			}
		}
		return sb.toString();
	}

	private String ul_changedVendorExtsDeep(ChangedExtensionGroup group, String pre) {
		StringBuffer sb = new StringBuffer();

		if (group.vendorExtensionsAreDiffShallow()) {
			List<String> changedVendorExts = sort(ul_changedVendorExts(group));
			sb.append(sortedPrefixJoin(changedVendorExts, pre + LI));
		}
		for (Entry<String, ChangedExtensionGroup> entry : group.getChangedSubGroups().entrySet()) {
			String key = entry.getKey();
			ChangedExtensionGroup subgroup = entry.getValue();
			if (subgroup.vendorExtensionsAreDiff()) {
				sb.append("\n").append(prefix(key, pre + LI)).append("\n");
				sb.append(ul_changedVendorExtsDeep(subgroup, pre + PRE_LI));
			}
		}

		return sb.toString();
	}

	private List<String> ul_changedVendorExts(ChangedExtensionGroup group) {
		ArrayList<String> lines = new ArrayList<String>();
		for (String key : group.getIncreasedVendorExtensions().keySet()) {
			lines.add(li_addVendorExt(key));
		}
		for (String key : group.getMissingVendorExtensions().keySet()) {
			lines.add(li_missingVendorExt(key));
		}
		for (String key : group.getChangedVendorExtensions().keySet()) {
			lines.add(li_changedVendorExt(key));
		}
		return lines;
	}

	private String ul_changedVendorExts(ChangedExtensionGroup group, String pre) {
		return sortedPrefixJoin(ul_changedVendorExts(group), pre);
	}

	private List<String> ul_paramChangedVendorExts(String paramName, ChangedExtensionGroup group) {
		updateKeysWithParam(paramName, group.getIncreasedVendorExtensions());
		updateKeysWithParam(paramName, group.getMissingVendorExtensions());
		updateKeysWithParam(paramName, group.getChangedVendorExtensions());

		return ul_changedVendorExts(group);
	}

	private <V> void updateKeysWithParam(String prepend, Map<String, V> map) {
		for (String key : map.keySet()) {
			V value = map.remove(key);
			map.put(prepend + "." + key, value);
		}
	}

	private String li_addVendorExt(String key) {
		return INSERT + CODE + key + CODE;
	}

	private String li_missingVendorExt(String key) {
		return DELETE + CODE + key + CODE;
	}

	private String li_changedVendorExt(String key) {
		return MODIFY + CODE + key + CODE;
	}

	private String ul_response(ChangedOperation changedOperation) {
		List<ElProperty> addProps = changedOperation.getAddProps();
		List<ElProperty> delProps = changedOperation.getMissingProps();
		List<ElProperty> changedProps = changedOperation.getChangedProps();
		List<String> propLines = new ArrayList<String>();

		String prefix = PRE_LI + PRE_LI + LI;
		for (ElProperty prop : addProps) {
			propLines.add(li_addProp(prop));
		}
		for (ElProperty prop : delProps) {
			propLines.add(li_missingProp(prop));
		}
		for (ElProperty prop : changedProps) {
			propLines.add(li_changedProp(prop));
			if (prop.vendorExtensionsAreDiff()) {
				propLines.addAll(ul_changedVendorExts(prop));
			}
		}
		return "\n" + sortedPrefixJoin(propLines, prefix);
	}

	private String li_missingProp(ElProperty prop) {
		Property property = prop.getProperty();
		String prefix = DELETE + CODE;
		String desc = " //" + property.getDescription();
		String postfix = CODE +
				(null == property.getDescription() ? "" : desc);

		StringBuffer sb = new StringBuffer("");
		sb.append(prefix).append(prop.getEl())
				.append(postfix);
		return sb.toString();
	}

	private String li_addProp(ElProperty prop) {
		Property property = prop.getProperty();
		String prefix = INSERT + CODE;
		String desc = " //" + property.getDescription();
		String postfix = CODE +
				(null == property.getDescription() ? "" : desc);

		StringBuffer sb = new StringBuffer("");
		sb.append(prefix).append(prop.getEl())
				.append(postfix);
		return sb.toString();
	}

	private String li_changedProp(ElProperty prop) {
		Property property = prop.getProperty();
		String prefix = MODIFY + CODE;
		String desc = " //" + property.getDescription();
		String postfix =  CODE + (null == property.getDescription() ? "" : desc);

		StringBuffer sb = new StringBuffer("");
		sb.append(prefix).append(prop.getEl())
				.append(postfix);
		return sb.toString();
	}

	private String ul_param(ChangedOperation changedOperation) {
		String typePrefix = PRE_LI + LI;
		String prefix = PRE_LI + typePrefix;

		List<Parameter> addParameters = changedOperation.getAddParameters();
		List<Parameter> delParameters = changedOperation.getMissingParameters();
		List<ChangedParameter> changedParameters = changedOperation.getChangedParameter();
		Map<String, List<String>> paramLineMap = new LinkedHashMap<String,List<String>>();

		StringBuffer sb = new StringBuffer("\n");

		for (Parameter param : addParameters) {
			String in = param.getIn();
			if (!paramLineMap.containsKey(in)) {
				paramLineMap.put(in, new ArrayList<String>());
			}
			paramLineMap.get(in).add(li_addParam(param));
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
				.add(li_addProp(prop));
			}
			for (ElProperty prop : missing) {
				paramLineMap.get(left.getIn()).add(li_missingProp(prop));
			}
			for (ElProperty prop : changed) {
				paramLineMap.get(left.getIn()).add(li_changedProp(prop));
			}
			if (changeVendorExts) {
				paramLineMap.get(left.getIn())
					.addAll(ul_paramChangedVendorExts(left.getName(), param));
			}
		}

		for (Parameter param : delParameters) {
			String in = param.getIn();
			if (!paramLineMap.containsKey(in)) {
				paramLineMap.put(in, new ArrayList<String>());
			}
			paramLineMap.get(param.getIn()).add(li_missingParam(param));
		}

		for (String in : paramLineMap.keySet()) {
			String title = IT + in.substring(0, 1).toUpperCase() + in.substring(1) + " Parameters" + IT;
			sb.append(prefix(title, typePrefix)).append("\n")
				.append(sortedPrefixJoin(paramLineMap.get(in), prefix));

		}

		for (ChangedParameter param : changedParameters) {
			boolean changeRequired = param.isChangeRequired();
			boolean changeDescription = param.isChangeDescription();
			boolean changeVendorExts = param.vendorExtensionsAreDiff();
			if (changeRequired || changeDescription || changeVendorExts) {
				sb.append(li_changedParam(param));
			}
		}
		return sb.toString();
	}

	private String li_addParam(Parameter param) {
		String prefix = INSERT + CODE;
		String desc = " //" + param.getDescription();
		String postfix = CODE +
				(null == param.getDescription() ? "" : desc);

		StringBuffer sb = new StringBuffer("");
		sb.append(prefix).append(param.getName())
				.append(postfix);
		return sb.append("\n").toString();
	}

	private String li_missingParam(Parameter param) {
		StringBuffer sb = new StringBuffer("");
		String prefix = DELETE +  CODE;
		String desc = " //" + param.getDescription();
		String postfix = CODE + 
				(null == param.getDescription() ? "" : desc);
		sb.append(prefix).append(param.getName())
				.append(postfix);
		return sb.append("\n").toString();
	}

	private String li_changedParam(ChangedParameter changeParam) {
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
					.append(oldValue + " " + RIGHT_ARROW + " " + newValue + "\n");
		}
		if (changeDescription) {
			sb.append(prefix).append(LI).append("Notes ")
					.append(leftParam.getDescription()).append(RIGHT_ARROW)
					.append(rightParam.getDescription()).append("\n");
		}
		return sb.append("\n").toString();
	}
}
