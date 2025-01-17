package DB;

import Crawler.WebCrawler;
import Logging.*;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.DBObject;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Aggregates.set;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.io.File;
import java.util.*;

public class mongoDB {
    public static int MAX_PAGES_NUM = 6000  ;
    private static MongoClient client;
    private static MongoDatabase DB;
    MongoCollection<Document> seedCollection;
    MongoCollection<Document> searchHistoryCollection;
    MongoCollection<Document> crawlerCollection;
    MongoCollection<Document> wordsCollection;

    MongoCollection<Document> paragraphsCollection;
    MongoCollection<Document> urlsCollection;

    public mongoDB(String DB_Name) {

        //if (client == null) {
//            ConnectionString connectionString = new ConnectionString("mongodb+srv://ahmedr2001:eng3469635@javasearchengine.8xarqeo.mongodb.net/?retryWrites=true&w=majority");
        ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017");
        client = MongoClients.create(connectionString);
        DB = client.getDatabase(DB_Name);
        seedCollection = DB.getCollection("Seed");
        crawlerCollection = DB.getCollection("CrawledPages");
        wordsCollection = DB.getCollection("WordsCollection");
        paragraphsCollection = DB.getCollection("ParagraphsCollection");
        urlsCollection = DB.getCollection("URLsCollection");
        searchHistoryCollection = DB.getCollection("SearchHistoryCollection");
//            crawlerCollection.drop();
//            seedCollection.drop();
        //} else {
        //  System.out.println("Already connected to the client");
        //}
    }

    public void initializeSeed() {
        if (crawlerCollection.countDocuments() >= MAX_PAGES_NUM) {
            Logging.printColored("[Warn] ", Color.YELLOW);
            System.out.println("Crawling has reached its limit which is equal to " + MAX_PAGES_NUM + " ,System is rebooting");
            crawlerCollection.drop();
            seedCollection.drop();
        }
        if (seedCollection.countDocuments() == 0) {
            try {
                File file = new File("seed.txt").getAbsoluteFile();
                Scanner cin = new Scanner(file);
                while (cin.hasNextLine()) {
                    String url = cin.nextLine();
                        org.jsoup.nodes.Document jdoc = WebCrawler.getDocument(url);
                        if (jdoc != null) {
                            Document doc = new Document("URL", url).append("KEY", WebCrawler.toHexString(WebCrawler.getSHA(jdoc.body().toString()))).append("BODY", jdoc.body().toString()).append("TITLE", jdoc.title());
                            seedCollection.insertOne(doc);
                        }
                }
                cin.close();
            } catch (Exception e) {
                Logging.printColored("[Error] ", Color.RED);
                System.out.println("Reading seed file failed :" + e);
            }
        } else {
            Logging.printColored("[Warn] ", Color.YELLOW);
            System.out.println("Crawling hasn't reached its limit yet , so System is Continued");
        }
    }

    public void addToCrawledPages(Document doc,int ID) {
        synchronized (this) {
            if (doc == null) return;
            if (getNumOfCrawledPages() < mongoDB.MAX_PAGES_NUM) {
                if (!isCrawled(doc)) {
                    doc.append("_id", crawlerCollection.countDocuments() + 1);
                    Logging.printColored("[Insertion] ", Color.GREEN);
                    System.out.println(ID + "=>Bot Received webpage with url = " + doc.get("URL") + " and the Title is : " + doc.get("TITLE"));
                    crawlerCollection.insertOne(doc);
                }
            }
        }
    }

    public boolean isCrawled(Document doc) {
        synchronized (this) {
            return crawlerCollection.find(eq("KEY", doc.get("KEY"))).cursor().hasNext() || crawlerCollection.find(eq("URL", doc.get("URL"))).cursor().hasNext();
        }
    }

    public void pushSeed(Document doc) {
        synchronized (this) {
            if (doc == null) return;
            seedCollection.insertOne(doc);
        }
    }

    public Document popSeed() {
        synchronized (this) {
            return seedCollection.findOneAndDelete(new Document());
        }
    }

    public boolean isSeeded(Document doc) {
        synchronized (this) {
            return seedCollection.find(eq("KEY", doc.get("KEY"))).cursor().hasNext() || seedCollection.find(eq("URL", doc.get("URL"))).cursor().hasNext();
        }
    }

    public long getSeedSize() {
        synchronized (this) {
            return seedCollection.countDocuments();
        }
    }

    public long getNumOfCrawledPages() {
        synchronized (this) {
            return crawlerCollection.countDocuments();
        }
    }

    // Indexing Functions

    public boolean isUrlIndexed(String url) {
//        synchronized (this) {
            return urlsCollection.find(new Document("url", url)).iterator().hasNext();
//        }
    }

    public boolean isParagraphIndexed(String paragraph, Integer paragraphId) {
        synchronized (this) {
        DBObject and_part1 = new BasicDBObject("_id", new BasicDBObject("$eq", paragraphId));
        DBObject and_part2 = new BasicDBObject("paragraph", new BasicDBObject("$eq", paragraph));
        BasicDBList and = new BasicDBList();
        and.add(and_part1);
        and.add(and_part2);
        DBObject query = new BasicDBObject("$and", and);

            return paragraphsCollection.find((Bson) query).iterator().hasNext();
        }
    }

    public static void List_All(MongoCollection collection) {
//        Listing All Mongo Documents in Collection
        FindIterable<Document> iterDoc = collection.find();
        int i = 1;
// Getting the iterator
        System.out.println("Listing All Mongo Documents");
        Iterator it = iterDoc.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
            i++;
        }
//specific document retrieving in a collection
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("name", "Manga");
        System.out.println("Retrieving specific Mongo Document");
        MongoCursor<Document> cursor = collection.find(searchQuery).iterator();
        while (cursor.hasNext()) {
            System.out.println(cursor.next());
        }
    }

    public Iterable<Document> getCrawlerCollection(int batchSize, int iteration) {
        List<Document> results = new ArrayList<>();
        FindIterable<Document> iterable = crawlerCollection.find().skip(iteration * batchSize).limit(batchSize);
        iterable.into(results);
        return results;
    }
    public MongoCollection<Document> getUrlsCollection (){
        return  urlsCollection;
    }
    public List<Document> getWordDocuments(String search_word) {
        List<Document> results = new ArrayList<>();

        //1.Create a query document
        Document query = new Document();
        query.append("word", search_word);

        //2. create cursor to resulted documents
        try (MongoCursor<Document> cursor = wordsCollection.find(query).iterator()) {
            //3. iterate through it
            while (cursor.hasNext()) {
                results.add(cursor.next()); // 4. add results
            }
        }

        return results;
    }

    public String getUrlBody(String url) {
        String result = null;

        //1.Create a query document
        Document query = new Document("URL", url);

        //2. create cursor to resulted documents
        Document res_doc = crawlerCollection.find(query).first();
        if (res_doc != null)
            result = res_doc.getString("BODY");

        return result;
    }


    public void addIndexedWord(String newWord, List<Document> newWordPages) {
//        synchronized (this) {
            if (newWordPages == null) {
                return;
            }
            Document filter = new Document("word", newWord);
            FindIterable<Document> fi = wordsCollection.find(filter);
            Iterator<Document> it = fi.iterator();
            Boolean newWordExists = it.hasNext();
            int newWordPagesCnt = newWordPages.size();
            if (newWordExists) {
                List<Document> prevWordPages = it.next().get("pages", List.class);
                int prevWordPagesCnt = prevWordPages.size();
                prevWordPages.addAll(newWordPages);
                HashSet<Document> uniquePages = new HashSet<>();
                uniquePages.addAll(prevWordPages);
                wordsCollection.findOneAndUpdate(filter, new Document("$set", new Document("word", newWord)
                        .append("IDF", Math.log(crawlerCollection.countDocuments() / (double) newWordPagesCnt + prevWordPagesCnt))
                        .append("pages", uniquePages)));
            } else {
                Document doc = new Document("word", newWord)
                        .append("IDF", Math.log(crawlerCollection.countDocuments() / (double) newWordPagesCnt))
                        .append("pages", newWordPages);
                wordsCollection.insertOne(doc);
            }
//        }
    }

    public void addIndexedUrl(Integer _id, String url, String title) {
//        synchronized (this) {
            Boolean pageExists = isUrlIndexed(url);
            if (!pageExists) {
                Document doc = new Document("_id", _id)
                        .append("url", url)
                        .append("title", title);
                urlsCollection.insertOne(doc);
            }
//        }
    }
    public void addPageRank(String url , double page_rank){
        Document filter = new Document("url", url);
        urlsCollection.findOneAndUpdate(filter, new Document("$set", new Document("url", url)
                .append("rank", page_rank)));
//        String title =" " ;
//        List<Document> docs = (List<Document>) urlsCollection.find(filter);
//        for(Document doc:docs){
//            filter = new Document("_id", doc.get("_id"));
//            urlsCollection.findOneAndDelete(filter);
//            title = doc.getString("title");
//        }
//        urlsCollection.insertOne(new Document("$set", new Document("url", url)
//                .append("rank", page_rank).append("title",title)));
    }
    public void addIndexedParagraph(String paragraph, Integer paragraphId) {
        synchronized (this) {
            Boolean paragraphExists = isParagraphIndexed(paragraph, paragraphId);
            if (paragraphExists) {
//                Document filter = new Document("paragraph", paragraph);
//                paragraphsCollection.findOneAndUpdate(filter, new Document("$set", new Document("_id", paragraphId)
//                        .append("paragraph", paragraph)));
            } else {
                Document paragraphDoc = new Document();
                paragraphDoc.append("_id", paragraphId)
                        .append("paragraph", paragraph);

                paragraphsCollection.insertOne(paragraphDoc);
            }
        }
    }

    public int getNumOfIndexedParagraphs() {
        synchronized (this) {
            return (int) paragraphsCollection.countDocuments();
        }
    }

    public int getNumberOfIndexedUrls() {
//        synchronized (this) {
            return (int) urlsCollection.countDocuments();
//        }
    }

    public Iterable<Document> getPagesWithWord(String searchWord) {
        List<Document> results = new ArrayList<>();
        FindIterable<Document> iterable = wordsCollection.find(new Document("word", searchWord));
        Document pages = wordsCollection.find(new Document("word", searchWord)).first();
        if (pages != null) {
            System.out.println(pages.get("IDF"));
        }
        // { IDF ,  Array of pages }
        System.out.println(iterable);
        iterable.into(results);

        return results;
    }


}