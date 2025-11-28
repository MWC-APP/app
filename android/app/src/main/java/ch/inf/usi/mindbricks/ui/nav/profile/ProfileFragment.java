package ch.inf.usi.mindbricks.ui.nav.profile;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.databinding.FragmentProfileBinding;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;

    private PreferencesManager prefs;

    // Base URL for the avatar API
    private static final String DICEBEAR_BASE_URL = "https://api.dicebear.com/9.x/pixel-art/png";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Initialize the shared ViewModel
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Initialize PreferencesManager
        prefs = new PreferencesManager(requireContext());

        // Inflate the layout using view binding
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe the shared coin balance from the ViewModel
        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                binding.profileCoinBalance.setText(String.valueOf(balance));
            }
        });

        loadAndDisplayUserData();
    }

    /**
     * ADDED: Reads all user data from PreferencesManager and updates the UI elements.
     */
    private void loadAndDisplayUserData() {
        // Load and set the user's name
        binding.profileUserName.setText(prefs.getUserName());

        // Load, format, and set the sprint length
        String sprintLength = prefs.getUserSprintLengthMinutes();
        binding.profileSprintLength.setText(String.format("%s minutes", sprintLength));

        // Load and set the user's avatar using the saved seed
        String avatarSeed = prefs.getUserAvatarSeed();
        if (avatarSeed != null && !avatarSeed.isEmpty()) {
            loadRandomizedProfilePicture(avatarSeed);
        }

        loadAndRenderTags();
    }

    /**
     * ADDED: Loads the avatar from DiceBear using the saved seed and Glide.
     * @param seed The unique seed string for the avatar.
     */
    private void loadRandomizedProfilePicture(String seed) {
        Uri avatarUri = Uri.parse(DICEBEAR_BASE_URL)
                .buildUpon()
                .appendQueryParameter("seed", seed)
                .build();

        Glide.with(this)
                .load(avatarUri)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .centerCrop()
                .into(binding.profileImageView);
    }

    /**
     * ADDED: Loads tags from preferences and renders them in the ChipGroup.
     */
    private void loadAndRenderTags() {
        binding.profileTagsChipGroup.removeAllViews();
        List<Tag> tags = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(prefs.getUserTagsJson());
            for (int i = 0; i < array.length(); i++) {
                Tag t = Tag.fromJson(array.getJSONObject(i));
                if (t != null) {
                    tags.add(t);
                }
            }
        } catch (JSONException e) {
        }

        if (tags.isEmpty()) {
            binding.profileTagsEmptyState.setVisibility(View.VISIBLE);
            binding.profileTagsChipGroup.setVisibility(View.GONE);
        } else {
            binding.profileTagsEmptyState.setVisibility(View.GONE);
            binding.profileTagsChipGroup.setVisibility(View.VISIBLE);

            for (Tag tag : tags) {
                Chip chip = new Chip(getContext());
                chip.setText(tag.title());
                chip.setChipBackgroundColor(ColorStateList.valueOf(tag.color()));

                // Add the chip to the group
                binding.profileTagsChipGroup.addView(chip);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Set binding to null to avoid memory leaks
        binding = null;
    }
}
