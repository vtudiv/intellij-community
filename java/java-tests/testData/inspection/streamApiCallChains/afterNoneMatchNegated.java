// "Replace !Stream.noneMatch(...) with anyMatch(...)" "true"

import java.util.*;

class Test {
  public boolean testAnyMatch(List<List<String>> data) {
    if(data.stream().flatMap(Collection::stream).anyMatch(str -> str.isEmpty()))
      return true;
  }
}