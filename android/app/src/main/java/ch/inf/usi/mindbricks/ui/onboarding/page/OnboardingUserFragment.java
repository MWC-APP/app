package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.onboarding_error_name_required));
            isValid = false;
        }
        if (tags.isEmpty()) {
            showTagValidationError();
            isValid = false;
        }
        if (TextUtils.isEmpty(sprintLength)) {
            sprintLengthLayout.setError(getString(R.string.onboarding_error_sprint_required));
            isValid = false;
        } else {
            // 2. ensure that the sprint length is a valid number (integer + non-negative)
            try {
                int sprintMinutes = Integer.parseInt(sprintLength);
                if (sprintMinutes <= 0) {
                    sprintLengthLayout.setError(getString(R.string.onboarding_error_sprint_invalid));
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                sprintLengthLayout.setError(getString(R.string.onboarding_error_sprint_invalid));
                isValid = false;
            }
        }

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
     * Shows a dialog to add a new tag to the list
     */
    private void showAddTagDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_tag, null);
        TextInputLayout tagNameLayout = dialogView.findViewById(R.id.layoutTagName);
        TextInputEditText editTagName = dialogView.findViewById(R.id.editTagName);
        ChipGroup colorGroup = dialogView.findViewById(R.id.chipTagColors);

        int[] palette = Tags.getTagColorPalette(requireContext());
        for (int i = 0; i < palette.length; i++) {
            Chip chip = (Chip) LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_color_chip, colorGroup, false);
            chip.setId(View.generateViewId());
            chip.setChipBackgroundColor(ColorStateList.valueOf(palette[i]));
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            colorGroup.addView(chip);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_tags_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.onboarding_tags_dialog_add, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = readText(editTagName);
                    if (TextUtils.isEmpty(title)) {
                        tagNameLayout.setError(getString(R.string.onboarding_error_tag_name_required));
                        return;
                    } else {
                        tagNameLayout.setError(null);
                    }

                    int checkedChipId = colorGroup.getCheckedChipId();
                    if (checkedChipId == View.NO_ID) {
                        return;
                    }
                    Chip selected = colorGroup.findViewById(checkedChipId);

                    ColorStateList chipBgColor = Objects.requireNonNull(selected.getChipBackgroundColor());
                    int color = chipBgColor.getDefaultColor();

                    tags.add(new Tag(title, color));
                    renderTags();
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
