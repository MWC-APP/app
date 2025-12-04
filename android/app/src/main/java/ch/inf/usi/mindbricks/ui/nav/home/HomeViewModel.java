package ch.inf.usi.mindbricks.ui.nav.home;

import android.app.Application;
import android.os.CountDownTimer;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.concurrent.TimeUnit;

public class HomeViewModel extends AndroidViewModel {

        // Pomodoro states
    public enum PomodoroState {
        IDLE,
        STUDY,
        PAUSE,
        LONG_PAUSE
    }

    private CountDownTimer timer;

    public final MutableLiveData<Long> currentTime = new MutableLiveData<>(0L);
    public final MutableLiveData<PomodoroState> currentState = new MutableLiveData<>(PomodoroState.IDLE);
    public final MutableLiveData<Integer> earnedCoinsEvent = new MutableLiveData<>();

    private int sessionCounter = 0;

    public HomeViewModel(Application application) {
        super(application);
    }

    // The main entry point to start a new Pomodoro cycle
    public void pomodoroTechnique(int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes) {
        // Prevent starting a new timer if one is already running.
        if (currentState.getValue() != PomodoroState.IDLE) {
            return;
        }
        // Reset the session counter at the beginning of a new cycle
        this.sessionCounter = 0;
        // Start the first study session
        startStudySession(studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
    }

    // Starts a study session
    private void startStudySession(int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes) {
        this.sessionCounter++;
        currentState.setValue(PomodoroState.STUDY); // Set the state to STUDY
        long studyDurationMillis = TimeUnit.MINUTES.toMillis(studyDurationMinutes);

        timer = new CountDownTimer(studyDurationMillis, 1000) {
            private long lastMinute;

            @Override
            public void onTick(long millisUntilFinished) {
                currentTime.postValue(millisUntilFinished);

                // Award 1 coin for each completed minute of studying.
                long currentMinute = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                if (lastMinute > currentMinute) {
                    earnedCoinsEvent.postValue(1);
                }
                lastMinute = currentMinute; // Update the last minute tracker.
            }

            @Override
            public void onFinish() {
                // Award 3 bonus coins at the successful end of the session
                earnedCoinsEvent.postValue(3);

                // Decide whether to start a short pause or a long pause
                if (sessionCounter < 4) {
                    startPauseSession(false, studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
                } else {
                    startPauseSession(true, studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
                }
            }
        }.start();
    }

    // Starts a pause session
    private void startPauseSession(boolean isLongPause, int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes) {
        long pauseDurationMillis;
        if (isLongPause) {
            currentState.setValue(PomodoroState.LONG_PAUSE);
            pauseDurationMillis = TimeUnit.MINUTES.toMillis(longPauseDurationMinutes);
        } else {
            currentState.setValue(PomodoroState.PAUSE);
            pauseDurationMillis = TimeUnit.MINUTES.toMillis(pauseDurationMinutes);
        }

        // Create and start a new countdown timer for the pause
        timer = new CountDownTimer(pauseDurationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTime.postValue(millisUntilFinished); // Update the UI with  remaining time
            }

            @Override
            public void onFinish() {
                // end the cycle if longpause
                if (isLongPause) {
                    stopTimerAndReset();
                } else {
                    // Otherwise, continue to the next study session.
                    startStudySession(studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
                }
            }
        }.start();
    }

    // Stops the timer and resets the state to IDLE.
    public void stopTimerAndReset() {
        // Cancel the current timer if it exists.
        if (timer != null) {
            timer.cancel();
        }
        this.sessionCounter = 0;
        currentState.setValue(PomodoroState.IDLE);
        currentTime.setValue(0L);
    }

    // Resets the coin event LiveData to prevent it from re-firing
    public void onCoinsAwarded() {
        earnedCoinsEvent.setValue(null);
    }

    // Resets the timer display if the activity is recreated while the timer is idle
    public void activityRecreated() {
        if (currentState.getValue() == PomodoroState.IDLE) {
            currentTime.setValue(0L);
        }
    }

    public int getSessionCounter() {
        return sessionCounter;
    }
}
