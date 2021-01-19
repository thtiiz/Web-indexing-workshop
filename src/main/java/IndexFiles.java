import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


public class IndexFiles {
  static public void indexDocs(final IndexWriter writer, Path path) throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          try {
            indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
          } catch (IOException ignore) {

          } finally {
            return FileVisitResult.CONTINUE;
          }
        }
      });
    } else {
      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
    }
  }

  static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
    try (InputStream stream = Files.newInputStream(file)) {
      Document doc = new Document();
      doc.add(new StringField("path", file.toString(), Field.Store.YES));
      doc.add(new LongPoint("modified", lastModified));
      doc.add(new TextField("contents", new BufferedReader(
              new InputStreamReader(stream, StandardCharsets.UTF_8))));
      if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
        System.out.println("adding " + file);
        writer.addDocument(doc);
      } else {
        System.out.println("updating " + file);
        writer.updateDocument(new Term("path", file.toString()), doc);
      }
    }
  }



  public static void main(String[] args) {
    try {
      Path inputPath = Paths.get("src/resources/html");
      Path outputPath = Paths.get("src/resources/html");
      Directory dir = FSDirectory.open(inputPath);
      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
      IndexWriter writer = new IndexWriter(dir, indexWriterConfig);
      indexDocs(writer, outputPath);
      writer.close();
    } catch (IOException e) {

    }
  }
}
