package ch.inf.usi.mindbricks.ui.nav.home;

import android.app.Application;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.util.SessionRecordingManager;

public class HomeViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final SessionRecordingManager sessionRecordingManager;
    private CountDownTimer countDownTimer;
    private StudySession currentStudySession;
    private long startTimeInMillis = 0;
    private long remainingTimeInMillis = 0;

    private final MutableLiveData<Long> _currentTime = new MutableLiveData<>(0L);
    public final LiveData<Long> currentTime = _currentTime;

    private final MutableLiveData<Boolean> _isTimerRunning = new MutableLiveData<>(false);
    public final LiveData<Boolean> isTimerRunning = _isTimerRunning;

    private final MutableLiveData<Integer> _earnedCoinsEvent = new MutableLiveData<>();
    public final LiveData<Integer> earnedCoinsEvent = _earnedCoinsEvent;

    private final MutableLiveData<Void> _sessionCompleteEvent = new MutableLiveData<>();
    public final LiveData<Void> sessionCompleteEvent = _sessionCompleteEvent;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        // Initialize dependencies using the application context
        db = AppDatabase.getInstance(application.getApplicationContext());
        sessionRecordingManager = new SessionRecordingManager(application.getApplicationContext());
    }

    /**
     * Starts a new study session. This is the main entry point from the Fragment.
     */
    public void startTimer(int minutes) {
        if (Boolean.TRUE.equals(_isTimerRunning.getValue())) {
            return; // Timer is already running
        }

        startTimeInMillis = TimeUnit.MINUTES.toMillis(minutes);
        remainingTimeInMillis = startTimeInMillis;
        _isTimerRunning.setValue(true);

        // Perform database operations on a background thread
        new Thread(() -> {
            currentStudySession = new StudySession(
                    System.currentTimeMillis(),
                    minutes,
                    "General", // FIXME: Hard-coded tag
                    android.graphics.Color.GRAY // FIXME: Hard-coded color
            );
            long newId = db.studySessionDao().insert(currentStudySession);
            currentStudySession.setId(newId);

            // Once the session is in the DB, start recording and the countdown on the main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                sessionRecordingManager.startSession(newId);
                startCountdown(remainingTimeInMillis);
            });
        }).start();
    }

    /**
     * Stops the timer early and saves the session data.
     */
    public void stopTimerAndReset() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (currentStudySession != null) {
            sessionRecordingManager.stopSession(currentStudySession);
            currentStudySession = null;
        }

        // Reset all state variables
        startTimeInMillis = 0;
        remainingTimeInMillis = 0;
        _isTimerRunning.setValue(false);
        _currentTime.setValue(0L);
    }

    /**
     * Called by the Fragment when its view is recreated to ensure the timer visually resumes.
     */
    public void activityRecreated() {
        if (Boolean.TRUE.equals(_isTimerRunning.getValue()) && remainingTimeInMillis > 0) {
            // Re-trigger the running state for observers
            _isTimerRunning.setValue(true);
            // Re-start the countdown on the UI thread
            startCountdown(remainingTimeInMillis);
        }
    }

    /**
     * Called by the Fragment after an event has been handled.
     */
    public void onCoinsAwarded() {
        _earnedCoinsEvent.setValue(null);
    }

    public void onSessionCompleted() {
        _sessionCompleteEvent.setValue(null);
    }

    private void startCountdown(long duration) {
        // Always cancel any existing timer before starting a new one
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeInMillis = millisUntilFinished;
                _currentTime.postValue(millisUntilFinished);

                // Calculate elapsed minutes to award coins
                long elapsedMillis = startTimeInMillis - millisUntilFinished;
                // Check on the boundary of each minute
                if (elapsedMillis > 0 && (elapsedMillis / 1000) % 60 == 0) {
                    _earnedCoinsEvent.postValue(1);
                }
            }

            @Override
            public void onFinish() {
                // Award one last coin for completing the final minute
                if (startTimeInMillis > 0) {
                    _earnedCoinsEvent.postValue(1);
                }
                // Trigger the session complete event
                _sessionCompleteEvent.postValue(null);
                // Stop and reset everything
                stopTimerAndReset();
            }
        }.start();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Ensure the timer is stopped and the session is saved if the ViewModel is destroyed
        if (Boolean.TRUE.equals(_isTimerRunning.getValue())) {
            stopTimerAndReset();
        }
    }
}
