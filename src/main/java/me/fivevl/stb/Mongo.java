package me.fivevl.stb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Mongo {
    private Mongo() {}
    private static MongoDatabase database;
    public static void init() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb+srv://" + Main.config.mongoUsername + ":" + Main.config.mongoPassword + "@" + Main.config.mongoHost + "/?retryWrites=true&w=majority"))
                .build();
        database = MongoClients.create(settings).getDatabase(Main.config.mongoDatabase);
    }

    public static Document get(String id) {
        return database.getCollection(Main.config.mongoCollection).find(new Document("id", id)).first();
    }

    public static void set(Document document) {
        database.getCollection(Main.config.mongoCollection).insertOne(document);
    }
}
