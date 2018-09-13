package com.deepoove.swagger.diff.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
		sb.append(ul_changedVendorExtsDeep(topLevelExts, ""));

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
				sb.append(LI).append(pathUrl).append("\n");
				sb.append(ul_changedVendorExts(changedEndpoint, PRE_LI));
			}

			for (Entry<HttpMethod, ChangedOperation> entry : changedOperations.entrySet()) {
				String method = entry.getKey().toString();
				ChangedOperation changedOperation = entry.getValue();
				String desc = changedOperation.getSummary() != null
					? " - " + changedOperation.getSummary()
					: "";

				StringBuffer ul_detail = new StringBuffer();
				if (changedOperation.vendorExtensionsAreDiff()) {
					ul_detail.append(ul_changedVendorExts(changedOperation, detailPrefix));
				}
				if (changedOperation.isDiffParam()) {
					ul_detail.append(detailTitlePrefix)
							.append(IT).append("Parameters").append(IT)
							.append(ul_param(changedOperation));
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
			sb.append(ul_changedVendorExts(group, pre));
		}
		for (Entry<String, ChangedExtensionGroup> entry : group.getChangedSubGroups().entrySet()) {
			String key = entry.getKey();
			ChangedExtensionGroup subgroup = entry.getValue();
			if (subgroup.vendorExtensionsAreDiff()) {
				sb.append(pre + LI + key + "\n");
				sb.append(ul_changedVendorExtsDeep(subgroup, pre + PRE_LI));
			}
		}

		return sb.toString();
	}

	private String ul_changedVendorExts(ChangedExtensionGroup group, String pre) {
		StringBuffer sb = new StringBuffer();
		ArrayList<String> lines = new ArrayList<String>();
		for (String key : group.getIncreasedVendorExtensions().keySet()) {
			lines.add(pre + li_addVendorExt(key));
		}
		for (String key : group.getMissingVendorExtensions().keySet()) {
			lines.add(pre + li_missingVendorExt(key));
		}
		for (String key : group.getChangedVendorExtensions().keySet()) {
			lines.add(pre + li_changedVendorExt(key));
		}
		Joiner joiner = Joiner.on("\n");

		return sb.append(joiner.join(sort(lines))).toString();
	}

	private List<String> sort(List<String> lines) {
		Collections.sort(lines, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.substring(7).compareTo(s2.substring(7));
			}
		});
		Collections.sort(lines, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}
		});
		return lines;
	}

	private String ul_paramChangedVendorExts(String paramName, ChangedExtensionGroup group, String pre) {
		updateKeysWithParam(paramName, group.getIncreasedVendorExtensions());
		updateKeysWithParam(paramName, group.getMissingVendorExtensions());
		updateKeysWithParam(paramName, group.getChangedVendorExtensions());

		return ul_changedVendorExts(group, pre);
	}

	private <V> void updateKeysWithParam(String prepend, Map<String, V> map) {
		for (String key : map.keySet()) {
			V value = map.remove(key);
			map.put(prepend + "." + key, value);
		}
	}

	private String li_addVendorExt(String key) {
		return LI + "Insert " + CODE + key + CODE + "\n";
	}

	private String li_missingVendorExt(String key) {
		return LI + "Delete " + CODE + key + CODE + "\n";
	}

	private String li_changedVendorExt(String key) {
		return LI + "Modify " + CODE + key + CODE + "\n";
	}

	private String ul_response(ChangedOperation changedOperation) {
		List<ElProperty> addProps = changedOperation.getAddProps();
		List<ElProperty> delProps = changedOperation.getMissingProps();
		List<ElProperty> changedProps = changedOperation.getChangedProps();
		StringBuffer sb = new StringBuffer("\n");

		String prefix = PRE_LI + PRE_LI + LI;
		for (ElProperty prop : addProps) {
			sb.append(prefix).append(li_addProp(prop) + "\n");
		}
		for (ElProperty prop : delProps) {
			sb.append(prefix).append(li_missingProp(prop) + "\n");
		}
		for (ElProperty prop : changedProps) {
			sb.append(prefix).append(li_changedProp(prop) + "\n");
			if (prop.vendorExtensionsAreDiff()) {
				sb.append(prefix).append(ul_changedVendorExts(prop, PRE_LI + PRE_LI));
			}
		}
		return sb.toString();
	}

	private String li_missingProp(ElProperty prop) {
		Property property = prop.getProperty();
		String prefix = "Delete " + CODE;
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
		String prefix = "Insert " + CODE;
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
		String prefix = "Modify " + CODE;
		String desc = " //" + property.getDescription();
		String postfix =  CODE + (null == property.getDescription() ? "" : desc);

		StringBuffer sb = new StringBuffer("");
		sb.append(prefix).append(prop.getEl())
				.append(postfix);
		return sb.toString();
	}

	private String ul_param(ChangedOperation changedOperation) {
		List<Parameter> addParameters = changedOperation.getAddParameters();
		List<Parameter> delParameters = changedOperation.getMissingParameters();
		List<ChangedParameter> changedParameters = changedOperation
				.getChangedParameter();

		String prefix = PRE_LI + PRE_LI + LI;

		StringBuffer sb = new StringBuffer("\n");

		for (Parameter param : addParameters) {
			sb.append(prefix).append(li_addParam(param) + "\n");
		}
		for (ChangedParameter param : changedParameters) {
			List<ElProperty> increased = param.getIncreased();
			for (ElProperty prop : increased) {
				sb.append(prefix).append(li_addProp(prop) + "\n");
			}
		}
		for (ChangedParameter param : changedParameters) {
			boolean changeRequired = param.isChangeRequired();
			boolean changeDescription = param.isChangeDescription();
			boolean changeVendorExts = param.vendorExtensionsAreDiff();

			if (changeRequired || changeDescription || changeVendorExts) {
				sb.append(li_changedParam(param));
			}
		}
		for (ChangedParameter param : changedParameters) {
			List<ElProperty> missing = param.getMissing();
			List<ElProperty> changed = param.getChanged();
			for (ElProperty prop : missing) {
				sb.append(prefix).append(li_missingProp(prop) + "\n");
			}
			for (ElProperty prop : changed) {
				sb.append(prefix).append(li_changedProp(prop) + "\n");
			}
		}
		for (Parameter param : delParameters) {
			sb.append(prefix).append(li_missingParam(param) + "\n");
		}
		return sb.toString();
	}

	private String li_addParam(Parameter param) {
		String prefix = "Insert " + CODE;
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
		String prefix = "Delete " +  CODE;
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
		boolean vendorExtsChanged = changeParam.vendorExtensionsAreDiff();
		Parameter rightParam = changeParam.getRightParameter();
		Parameter leftParam = changeParam.getLeftParameter();

		String prefix = PRE_LI + PRE_LI;

		StringBuffer sb = new StringBuffer("");
		if (vendorExtsChanged) {
			sb.append(ul_paramChangedVendorExts(rightParam.getName(), changeParam, prefix));
		}
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
