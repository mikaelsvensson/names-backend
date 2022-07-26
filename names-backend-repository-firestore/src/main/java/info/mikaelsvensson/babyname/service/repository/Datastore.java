package info.mikaelsvensson.babyname.service.repository;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.FirebaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class Datastore {
    private static final Logger LOGGER = LoggerFactory.getLogger(Datastore.class);

    private static Firestore db;

    public static Firestore get() {
        if (db == null) {
//            try {
//                InputStream serviceAccount = new FileInputStream("habits-354408-e3a8a1ec6c72.json");
                GoogleCredentials credentials = GoogleCredentials.newBuilder().build();
//                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(credentials)
                        .setProjectId("winged-hue-356217")
                        .build();
                FirebaseApp.initializeApp(options);

                db = FirestoreClient.getFirestore();
//            } catch (IOException e) {
//                LOGGER.error("Failed to connect to Firestore", e);
//            }
        }
        return db;
    }

}