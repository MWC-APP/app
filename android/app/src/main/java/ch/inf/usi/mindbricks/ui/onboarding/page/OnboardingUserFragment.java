package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.ui.onboarding.OnboardingStepValidator;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.Tags;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OnboardingUserFragment extends Fragment implements OnboardingStepValidator {

    private TextInputLayout nameLayout;
    private TextInputLayout sprintLengthLayout;

    private TextInputEditText editName;
    private TextInputEditText editSprintLength;
    private ChipGroup tagChipGroup;
    private MaterialButton addTagButton;
    private View tagEmptyState;

    private final List<Tag> tags = new ArrayList<>();
    private PreferencesManager prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_user, container, false);

        prefs = new PreferencesManager(requireContext());

        // NOTE: get fields

        // name + container
        nameLayout = view.findViewById(R.id.layoutName);
        editName = view.findViewById(R.id.editName);

        // sprint length + container
        sprintLengthLayout = view.findViewById(R.id.layoutSprintLength);
        editSprintLength = view.findViewById(R.id.editSprintLength);

        // tag management
        tagChipGroup = view.findViewById(R.id.chipGroupTags);
        addTagButton = view.findViewById(R.id.buttonAddTag);
        tagEmptyState = view.findViewById(R.id.textTagsEmptyState);

        // set handler to pick photo
        MaterialButton choosePhoto = view.findViewById(R.id.buttonChoosePhoto);
        choosePhoto.setOnClickListener(v -> launchPhotoPicker());

        // show dialog on "add a tag"
        addTagButton.setOnClickListener(v -> showAddTagDialog());

        // preload if already stored
        editName.setText(prefs.getUserName());
        editSprintLength.setText(prefs.getUserSprintLengthMinutes());

        // load + render the list of tags stored inside preferences
        loadTagsFromPrefs();
        renderTags();

        // input validation callback -> when user focus changes = trigger field validation
        editName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateNameField();
        });
        editSprintLength.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateSprintLengthField();
        });

        return view;
    }

    @Override
    public void onPause() {
        // on pause: store field values
        super.onPause();
        persistUserData(readText(editName), readText(editSprintLength));
    }

    @Override
    public boolean validateStep() {
        clearErrors();

        // get data from fields
        String name = readText(editName);
        String sprintLength = readText(editSprintLength);

        boolean isValid = true;

        // 1. ensure that all fields are filed

        if (!validateNameField()) isValid = false;
        if (tags.isEmpty()) {
            showTagValidationError();
            isValid = false;
        }
        if (!validateSprintLengthField()) isValid = false;

        // if all valid: store the result in app preferences
        if (isValid) {
            persistUserData(name, sprintLength);
        }

        return isValid;
    }

    /**
     * Stores the user data in preferences
     * @param name User name to store
     * @param sprintLength Sprint length to store
     */
    private void persistUserData(String name, String sprintLength) {
        prefs.setUserName(name);
        prefs.setUserSprintLengthMinutes(sprintLength);
        prefs.setUserTagsJson(serializeTags());
    }

    private void launchPhotoPicker() {
        // FIXME: we need to implement this!
    }

    /**
     * Reads the text from a {@link TextInputEditText}
     * @param editText {@link TextInputEditText} to read from
     * @return Text read from the text input
     */
    private String readText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    /**
     * Clears errors from fields in this fragment
     */
    private void clearErrors() {
        nameLayout.setError(null);
        sprintLengthLayout.setError(null);
    }

    /**
     * Validates the name field ensuring that user input is not empty
     * @return true if the name is valid, false otherwise
     */
    private boolean validateNameField() {
        String name = readText(editName);
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.onboarding_error_name_required));
            return false;
        }
        nameLayout.setError(null);
        return true;
    }

    /**
     * Validates the sprint length field ensuring that user input is not empty and is a valid number
     * @return true if the sprint length is valid, false otherwise
     */
    private boolean validateSprintLengthField() {
        String sprintLength = readText(editSprintLength);
        if (TextUtils.isEmpty(sprintLength)) {
            sprintLengthLayout.setError(getString(R.string.onboarding_error_sprint_required));
            return false;
        }
        try {
            int sprintMinutes = Integer.parseInt(sprintLength);
            if (sprintMinutes <= 0) {
                sprintLengthLayout.setError(getString(R.string.onboarding_error_sprint_invalid));
                return false;
            }
        } catch (NumberFormatException e) {
            sprintLengthLayout.setError(getString(R.string.onboarding_error_sprint_invalid));
            return false;
        }

        sprintLengthLayout.setError(null);
        return true;
    }

    /**
     * Shows a dialog to add a new tag to the list
     *
     * NOTE: this solution is inspired from this tutorial:
     * <https://www.geeksforgeeks.org/android/how-to-create-a-custom-alertdialog-in-android/>
     */
    private void showAddTagDialog() {
        // loads the dialog view from the layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_tag, null);

        // extract fields from the view
        TextInputLayout tagNameLayout = dialogView.findViewById(R.id.layoutTagName);
        TextInputEditText editTagName = dialogView.findViewById(R.id.editTagName);
        ChipGroup colorGroup = dialogView.findViewById(R.id.chipTagColors);

        // loads a tag color selector for each available colors
        int[] palette = Tags.getTagColorPalette(requireContext());
        for (int i = 0; i < palette.length; i++) {
            // load chip component view + update settings
            Chip chip = (Chip) LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_color_chip, colorGroup, false);
            chip.setId(View.generateViewId());
            chip.setChipBackgroundColor(ColorStateList.valueOf(palette[i]));
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            colorGroup.addView(chip);
        }

        // create dialog with custom view
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_tags_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.onboarding_tags_dialog_add, null);

        // show dialog + listen for positive button click
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    // validate tag name -> ensure it is not empty
                    String title = readText(editTagName);
                    if (TextUtils.isEmpty(title)) {
                        // show error if not valid
                        tagNameLayout.setError(getString(R.string.onboarding_error_tag_name_required));
                        return;
                    } else {
                        tagNameLayout.setError(null);
                    }

                    // get the selected color from the list
                    int checkedChipId = colorGroup.getCheckedChipId();
                    if (checkedChipId == View.NO_ID) {
                        // NOTE: this shouldn't be possible as the UI already enforces that a color is selected
                        // but still check for it
                        Snackbar.make(requireView(), R.string.onboarding_error_tag_color_required, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    Chip selected = colorGroup.findViewById(checkedChipId);

                    // get the color to use from the selected chip
                    ColorStateList chipBgColor = Objects.requireNonNull(selected.getChipBackgroundColor());
                    int color = chipBgColor.getDefaultColor();

                    // create tag and add it to the list + re-render
                    tags.add(new Tag(title, color));
                    renderTags();

                    // store the new tags in preferences
                    prefs.setUserTagsJson(serializeTags());
                    dialog.dismiss();
                }));

        dialog.show();
    }

    /**
     * Renders the list of created tags in the chip group
     */
    private void renderTags() {
        tagChipGroup.removeAllViews();
        if (tags.isEmpty()) {
            tagEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tagEmptyState.setVisibility(View.GONE);

        for (Tag tag : tags) {
            Chip chip = (Chip) LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_tag_chip, tagChipGroup, false);
            chip.setText(tag.title());
            chip.setChipBackgroundColor(ColorStateList.valueOf(tag.color()));
            chip.setChipIconTint(ColorStateList.valueOf(tag.color()));
            chip.setOnCloseIconClickListener(v -> {
                tags.remove(tag);
                renderTags();
                prefs.setUserTagsJson(serializeTags());
            });
            tagChipGroup.addView(chip);
        }
    }

    /**
     * Shows the error message for missing tags
     */
    private void showTagValidationError() {
        if (tagEmptyState instanceof android.widget.TextView) {
            ((android.widget.TextView) tagEmptyState).setText(getString(R.string.onboarding_error_tags_required));
        }
    }

    /**
     * Loads the list of tags from the preferences as JSON entries
     */
    private void loadTagsFromPrefs() {
        tags.clear();
        try {
            JSONArray array = new JSONArray(prefs.getUserTagsJson());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Tag t = Tag.fromJson(obj);
                if (t != null) tags.add(t);
            }
        } catch (JSONException e) {
            // ignore and start empty
        }
    }

    /**
     * Serializes the list of tags into a JSON array
     * @return JSON array of tags as raw String
     */
    private String serializeTags() {
        JSONArray array = new JSONArray();
        for (Tag tag : tags) {
            array.put(tag.toJson());
        }
        return array.toString();
    }
}
