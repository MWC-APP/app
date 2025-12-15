package ch.inf.usi.mindbricks.ui.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.DetailedQuestionsDialogFragment;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.EmotionSelectDialogFragment;
import ch.inf.usi.mindbricks.util.ProfileViewModel;
import ch.inf.usi.mindbricks.util.database.TestDataGenerator;

/**
 * Debug settings fragment for development and testing purposes.
 * Provides UI controls for generating test data and managing the database.
 */
public class SettingsDebugFragment extends Fragment {

    private ProfileViewModel profileViewModel;

    private MaterialButton btnGenerateBasic;
    private MaterialButton btnGenerateLarge;
    private MaterialButton btnGenerateEdgeCases;
    private MaterialButton btnTestQuestionnaire;
    private MaterialButton btnGenerateLibrary;
    private MaterialButton btnGenerateCafe;
    private MaterialButton btnGenerateDarkRoom;
    private MaterialButton btnGenerateOutdoor;
    private MaterialButton btnGenerateHome;
    private MaterialButton btnGenerateCommute;
    private MaterialButton btnVerifyDatabase;
    private MaterialButton btnClearDatabase;
    private MaterialButton btnAddCoins;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_debug, container, false);
        
        // Initialize ViewModel
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Initialize buttons
        btnGenerateBasic = view.findViewById(R.id.btn_generate_basic);
        btnGenerateLarge = view.findViewById(R.id.btn_generate_large);
        btnGenerateEdgeCases = view.findViewById(R.id.btn_generate_edge_cases);
        btnTestQuestionnaire = view.findViewById(R.id.btn_test_questionnaire);
        btnGenerateLibrary = view.findViewById(R.id.btn_generate_library);
        btnGenerateCafe = view.findViewById(R.id.btn_generate_cafe);
        btnGenerateDarkRoom = view.findViewById(R.id.btn_generate_dark_room);
        btnGenerateOutdoor = view.findViewById(R.id.btn_generate_outdoor);
        btnGenerateHome = view.findViewById(R.id.btn_generate_home);
        btnGenerateCommute = view.findViewById(R.id.btn_generate_commute);
        btnVerifyDatabase = view.findViewById(R.id.btn_verify_database);
        btnClearDatabase = view.findViewById(R.id.btn_clear_database);
        btnAddCoins = view.findViewById(R.id.btn_add_coins);

        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        // Add 1000 coins
        btnAddCoins.setOnClickListener(v -> {
            profileViewModel.addCoins(1000);
            showToast("Added 1000 coins!");
            showCompletionToast("Balance updated!");
        });

        // Test Questionnaire
        btnTestQuestionnaire.setOnClickListener(v -> showEmotionDialog());

        // Generate basic test data (50 sessions)
        btnGenerateBasic.setOnClickListener(v -> {
            showToast("Generating 50 test sessions...");
            TestDataGenerator.addTestSessions(requireContext(), 50);
            showCompletionToast("50 sessions generated!");
        });

        // Generate large dataset (500 sessions)
        btnGenerateLarge.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Generate Large Dataset")
                    .setMessage("This will generate 500 sessions with full data. This may take a few seconds.")
                    .setPositiveButton("Generate", (dialog, which) -> {
                        showToast("Generating 500 sessions... Please wait.");
                        TestDataGenerator.addTestSessions(requireContext(), 500);
                        showCompletionToast("500 sessions generated!");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Generate edge case sessions
        btnGenerateEdgeCases.setOnClickListener(v -> {
            showToast("Generating edge case sessions...");
            TestDataGenerator.addEdgeCaseSessions(requireContext());
            showCompletionToast("Edge cases generated!");
        });

        // Generate library environment sessions
        btnGenerateLibrary.setOnClickListener(v -> {
            showToast("Generating library sessions...");
            TestDataGenerator.addEnvironmentTestSessions(
                    requireContext(),
                    TestDataGenerator.EnvironmentCondition.QUIET_LIBRARY
            );
            showCompletionToast("Library sessions generated!");
        });

        // Generate cafe environment sessions
        btnGenerateCafe.setOnClickListener(v -> {
            showToast("Generating cafe sessions...");
            TestDataGenerator.addEnvironmentTestSessions(
                    requireContext(),
                    TestDataGenerator.EnvironmentCondition.BUSY_CAFE
            );
            showCompletionToast("Cafe sessions generated!");
        });

        // Generate dark room sessions
        btnGenerateDarkRoom.setOnClickListener(v -> {
            showToast("Generating dark room sessions...");
            TestDataGenerator.addEnvironmentTestSessions(
                    requireContext(),
                    TestDataGenerator.EnvironmentCondition.DARK_ROOM
            );
            showCompletionToast("Dark room sessions generated!");
        });

        // Generate outdoor park sessions
        btnGenerateOutdoor.setOnClickListener(v -> {
            showToast("Generating outdoor sessions...");
            TestDataGenerator.addEnvironmentTestSessions(
                    requireContext(),
                    TestDataGenerator.EnvironmentCondition.OUTDOOR_PARK
            );
            showCompletionToast("Outdoor sessions generated!");
        });

        // Generate home evening sessions
        btnGenerateHome.setOnClickListener(v -> {
            showToast("Generating home sessions...");
            TestDataGenerator.addEnvironmentTestSessions(
                    requireContext(),
                    TestDataGenerator.EnvironmentCondition.HOME_EVENING
            );
            showCompletionToast("Home sessions generated!");
        });

        // Generate commute sessions
        btnGenerateCommute.setOnClickListener(v -> {
            showToast("Generating commute sessions...");
            TestDataGenerator.addEnvironmentTestSessions(
                    requireContext(),
                    TestDataGenerator.EnvironmentCondition.COMMUTE
            );
            showCompletionToast("Commute sessions generated!");
        });

        // Verify database
        btnVerifyDatabase.setOnClickListener(v -> {
            showToast("Verifying database... Check Logcat with tag 'TestDataGenerator'");
            TestDataGenerator.verifyDatabase(requireContext());
        });

        // Clear database with confirmation
        btnClearDatabase.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Clear Database")
                    .setMessage("Are you sure you want to delete ALL sessions?\n\nThis includes:\n• All study sessions\n• All sensor logs\n• All questionnaire responses\n\nThis cannot be undone!")
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        TestDataGenerator.clearAllSessions(requireContext());
                        showToast("All sessions cleared");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showEmotionDialog() {
        EmotionSelectDialogFragment dialog = new EmotionSelectDialogFragment();
        dialog.setListener((emotionIndex, wantsDetailedQuestions) -> {
            if (wantsDetailedQuestions) showDetailedQuestionnaire(emotionIndex);
            else showToast("Quick questionnaire completed! (Emotion: " + emotionIndex + ")");
        });
        dialog.show(getChildFragmentManager(), "emotion_dialog");
    }

    private void showDetailedQuestionnaire(int emotionIndex) {
        DetailedQuestionsDialogFragment dialog = DetailedQuestionsDialogFragment.newInstance(emotionIndex);
        dialog.setListener(new DetailedQuestionsDialogFragment.OnQuestionnaireCompleteListener() {
            @Override
            public void onQuestionnaireComplete(int emotion, int enthusiasm, int energy,
                                                int engagement, int satisfaction, int anticipation) {
                showToast("Detailed questionnaire completed! (Score calculated)");
            }

            @Override
            public void onQuestionnaireSkipped(int emotionIndex) {
                showToast("Detailed questionnaire skipped. Quick response saved.");
            }
        });
        dialog.show(getChildFragmentManager(), "detailed_questionnaire");
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showCompletionToast(String message) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getContext() != null) {
                Toast.makeText(requireContext(), "✓ " + message, Toast.LENGTH_LONG).show();
            }
        }, 2500);
    }
}