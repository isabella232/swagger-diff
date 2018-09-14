package com.deepoove.swagger.diff.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Joiner;

public class MarkdownRenderUtils {

  private static Joiner LINE_JOINER = Joiner.on("\n");

  private MarkdownRenderUtils() {}

  public static List<String> prefix(List<String> lines, String pre) {
    List<String> prefixedLines = new ArrayList<String>();
    for (String line : lines) {
      prefixedLines.add(prefix(line, pre));
    }
    return prefixedLines;
  }

  public static String prefix(String line, String pre) {
    return pre + line;
  }

  public static List<String> sort(List<String> lines) {
    Collections.sort(lines, propertyComparator);
    Collections.sort(lines, actionComparator);
		return lines;
  }

  public static String sortedPrefixJoin(List<String> lines, String prefix) {
    return LINE_JOINER.join(prefix(sort(lines), prefix)) + "\n";
  }
  
  /*
   * Used for sorting lines by property. 7 is taken from the
   * width of the actions (as seen below). Right now it's relying on
   * the width of the names, for simplicity.
   * This should probably change down the road
   * 
   *   |<- 7 ->|
   *   |Insert |
   *   |Delete |
   *   |Modify |
   *   
   */
  private static Comparator<String> propertyComparator = new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
      return s1.substring(7).compareTo(s2.substring(7));
    }
  };

  private static Comparator<String> actionComparator = new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
      return value(s1) - value(s2);
    }
  };

  private static int value(String s) {
    if (s.startsWith("Add")) {
      return 0;
    }
    if (s.startsWith("Remove")) {
      return 1;
    }
    return 2;
  }
}