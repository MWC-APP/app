package ch.inf.usi.mindbricks.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;
import ch.inf.usi.mindbricks.util.database.DatabaseSeeder;


/**
 * Room database for MindBricks app
 */
@Database(entities = {
            StudySession.class,
            SessionSensorLog.class,
            SessionQuestionnaire.class,
            CalendarEvent.class
        },
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;
    private static Context appContext;

    public abstract StudySessionDao studySessionDao();
    public abstract SessionSensorLogDao sessionSensorLogDao();
    public abstract SessionQuestionnaireDao sessionQuestionnaireDao();
    public abstract CalendarEventDao calendarEventDao();
    private static final RoomDatabase.Callback DB_CALLBACK = new RoomDatabase.Callback(){
        // called on the database thread -> safe
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db){
            super.onCreate(db);
            if (INSTANCE != null && appContext != null){
                DatabaseSeeder.seedDatabaseOnCreate(appContext, INSTANCE);
            }
        }

        // called every time database is opened
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db){
            super.onOpen(db);
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            appContext = context.getApplicationContext();

            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "mindbricks_database"
                    )
                    .addCallback(DB_CALLBACK)
                    //.fallbackToDestructiveMigrationOnDowngrade(true)
                    // For development: destroys and recreates DB on version change
                    // For production: use proper migrations instead
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }

    // WARNING
    // must call getInstance() before using the database again!!!
    public static void closeDatabase(){
        if(INSTANCE != null && INSTANCE.isOpen()){
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}