import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.benchmark.byTask.feeds.DemoHTMLParser.Parser;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;


public class IndexFiles {
  static public void indexDocs(final IndexWriter writer, Path path) throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          try {
            indexDoc(writer, file, attrs.lastModifiedTime().toMillis(), path);
          } catch (IOException ignore) {

          } finally {
            return FileVisitResult.CONTINUE;
          }
        }
      });
    } else {
      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis(), path);
    }
  }

  static void indexDoc(IndexWriter writer, Path file, long lastModified, Path basePath) throws IOException {
    try (InputStream stream = Files.newInputStream(file)) {
      Document doc = new Document();
      doc.add(new StringField("path", file.toString(), Field.Store.YES));
      doc.add(new LongPoint("modified", lastModified));
//      doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

      Parser parser = new Parser(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));

      String title = parser.title;
      doc.add(new TextField("title", title, Field.Store.YES));

      String content = parser.body.replaceAll("\\s+", " ");
      doc.add(new TextField("contents", content, Field.Store.YES));

      String url = "https://" + basePath.toUri().relativize(file.toUri()).getPath();

      doc.add(new StoredField("url", url));

      if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
        System.out.println("adding " + file);
        writer.addDocument(doc);
      } else {
        System.out.println("updating " + file);
        writer.updateDocument(new Term("path", file.toString()), doc);
      }
    } catch (SAXException e) {
      e.printStackTrace();
    }
  }



  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
            + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
            + "This indexes the documents in DOCS_PATH, creating a Lucene index"
            + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }

    Date start = new Date();
    try {
      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = AppAnalyzer.appAnalyzer;
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
      }
      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");
    } catch (IOException e) {

    }
  }
}
