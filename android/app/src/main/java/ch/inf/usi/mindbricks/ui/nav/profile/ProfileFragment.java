package ch.inf.usi.mindbricks.ui.nav.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ch.inf.usi.mindbricks.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment implements View.OnClickListener {

    private FragmentProfileBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate to get binding
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        // add click listener to handle local events

        // get root
        View root = binding.getRoot();

        // ... do something ...

        // return root
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    @Override
    public void onClick(View v) {
        // Do something if v == someIdWeAreInterestedIn
    }
}
