package info.mikaelsvensson.babyname.service.util.importer;

import info.mikaelsvensson.babyname.service.model.ActionStatus;
import info.mikaelsvensson.babyname.service.model.ActionType;
import info.mikaelsvensson.babyname.service.repository.names.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;

class actions {
    String id;
    String created_by;
    ActionType action;
    String parameters;
    ActionStatus status;
    Instant created_at;

    public actions(String[] values) {
        this.id = DatabaseSnapshot.isDbNull(values[0]) ? null : values[0];
        this.created_by = DatabaseSnapshot.isDbNull(values[1]) ? null : values[1];
        this.action = DatabaseSnapshot.isDbNull(values[2]) ? null : ActionType.valueOf(values[2]);
        this.parameters = DatabaseSnapshot.isDbNull(values[3]) ? null : values[3];
        this.status = DatabaseSnapshot.isDbNull(values[4]) ? null : ActionStatus.valueOf(values[4]);
        this.created_at = DatabaseSnapshot.isDbNull(values[5]) ? null : Instant.ofEpochMilli(Long.parseLong(values[5]));
    }
}

class anonymousauthenticator_identifiers {
    String id;
    Instant created_at;
    Instant used_at;

    public anonymousauthenticator_identifiers(String[] values) {
        this.id = DatabaseSnapshot.isDbNull(values[0]) ? null : values[0];
        this.created_at = DatabaseSnapshot.isDbNull(values[1]) ? null : Instant.ofEpochMilli(Long.parseLong(values[1]));
        this.used_at = DatabaseSnapshot.isDbNull(values[2]) ? null : Instant.ofEpochMilli(Long.parseLong(values[2]));
    }
}

class name_owners {
    String name_id;
    String user_id;
    Instant created_at;

    public name_owners(String[] values) {
        this.name_id = DatabaseSnapshot.isDbNull(values[0]) ? null : values[0];
        this.user_id = DatabaseSnapshot.isDbNull(values[1]) ? null : values[1];
        this.created_at = DatabaseSnapshot.isDbNull(values[2]) ? null : Instant.ofEpochMilli(Long.parseLong(values[2]));
    }
}

class names {
    String id;
    String name;

    public names(String[] values) {
        this.id = DatabaseSnapshot.isDbNull(values[0]) ? null : values[0];
        this.name = DatabaseSnapshot.isDbNull(values[1]) ? null : values[1];
    }
}

class names_demographics {
    String name_id;
    Double percent_of_population;
    Double percent_women;
    Country country;

    public names_demographics(String[] values) {
        this.name_id = DatabaseSnapshot.isDbNull(values[0]) ? null : values[0];
        this.percent_of_population = DatabaseSnapshot.isDbNull(values[1]) ? null : Double.parseDouble(values[1]);
        this.percent_women = DatabaseSnapshot.isDbNull(values[2]) ? null : Double.parseDouble(values[2]);
        this.country = DatabaseSnapshot.isDbNull(values[3]) ? null : Country.fromCountryCode(values[3]);
    }
}

class names_metrics {
    String name_id;
    Integer syllable_count;

    public names_metrics(String[] values) {
        this.name_id = DatabaseSnapshot.isDbNull(values[0]) ? null : values[0];
        this.syllable_count = DatabaseSnapshot.isDbNull(values[1]) ? null : Integer.parseInt(values[1]);
    }
}

class relationships {
    String user_id;
    String related_user_id;

    public relationships(String[] values) {
        this.user_id = DatabaseSnapshot.isDbNull(values[0]) ? null : values[0];
        this.related_user_id = DatabaseSnapshot.isDbNull(values[1]) ? null : values[1];
    }
}

class users {
    String id;
    String system_name;
    Instant created_at;

    public users(String[] values) {
        this.id = DatabaseSnapshot.isDbNull(values[0]) ? null : values[0];
        this.system_name = DatabaseSnapshot.isDbNull(values[1]) ? null : values[1];
        this.created_at = DatabaseSnapshot.isDbNull(values[2]) ? null : Instant.ofEpochMilli(Long.parseLong(values[2]));
    }
}

class votes {
    String user_id;
    String name_id;
    Integer value;
    Instant created_at;
    Instant updated_at;

    public votes(String[] values) {
        this.user_id = DatabaseSnapshot.isDbNull(values[0]) ? null : values[0];
        this.name_id = DatabaseSnapshot.isDbNull(values[1]) ? null : values[1];
        this.value = DatabaseSnapshot.isDbNull(values[2]) ? null : Integer.parseInt(values[2]);
        this.created_at = DatabaseSnapshot.isDbNull(values[3]) ? null : Instant.ofEpochMilli(Long.parseLong(values[3]));
        this.updated_at = DatabaseSnapshot.isDbNull(values[4]) ? null : Instant.ofEpochMilli(Long.parseLong(values[4]));
    }
}

public class DatabaseSnapshot {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSnapshot.class);

    public ArrayList<actions> actions = new ArrayList<>();
    public ArrayList<anonymousauthenticator_identifiers> anonymousauthenticator_identifiers = new ArrayList<>();
    public ArrayList<name_owners> name_owners = new ArrayList<>();
    public ArrayList<names> names = new ArrayList<>();
    public ArrayList<names_demographics> names_demographics = new ArrayList<>();
    public ArrayList<names_metrics> names_metrics = new ArrayList<>();
    public ArrayList<relationships> relationships = new ArrayList<>();
    public ArrayList<users> users = new ArrayList<>();
    public ArrayList<votes> votes = new ArrayList<>();

    static boolean isDbNull(String value) {
        return "null".equals(value) || "\\N".equals(value);
    }

    public static DatabaseSnapshot readSnapshot(Path sqlExportFile) throws IOException {
        String currentTable = null;
        Class<?> tableClass = null;
        var snapshot = new DatabaseSnapshot();

        var lines = Files.readAllLines(sqlExportFile);
        for (String line : lines) {
            if (line.startsWith("COPY public")) {
                currentTable = line.substring("COPY public".length() + 1, line.indexOf(' ', "COPY public".length()));
                try {
                    tableClass = ClassLoader.getSystemClassLoader().loadClass("info.mikaelsvensson.babyname.service.util.importer." + currentTable);
                } catch (ClassNotFoundException e) {
                    LOGGER.error(String.format("Skipping %s", currentTable));
                    currentTable = null;
                    tableClass = null;
                }
            } else if (line.equals("\\.")) {
                currentTable = null;
                tableClass = null;
            } else {
                if (tableClass != null && currentTable != null) {
                    try {
                        var values = line.split("\\t");
                        Object tableObject = tableClass.getConstructor(new Class[]{String[].class}).newInstance((Object) values);
                        ((ArrayList) (DatabaseSnapshot.class.getField(currentTable).get(snapshot))).add(tableObject);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
                        LOGGER.error(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        return snapshot;
    }
}
