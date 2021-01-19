import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;

public class AppAnalyzer {
  public static Analyzer appAnalyzer = new ThaiAnalyzer();
}
