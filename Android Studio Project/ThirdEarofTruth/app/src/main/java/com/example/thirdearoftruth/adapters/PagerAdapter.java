package com.example.thirdearoftruth.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

public class PagerAdapter extends FragmentStatePagerAdapter {


    private ArrayList<Fragment> fragments;
    private ArrayList<String> titles;


    public PagerAdapter (FragmentManager fm){
        super (fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.fragments = new ArrayList<>();
        this.titles = new ArrayList<>();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }


    /**
     * add a new scrollable fragment to the tab
     * @param fragment
     * @param title
     */
    public void addFragment(Fragment fragment, String title){
        fragments.add(fragment);
        titles.add(title);

    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titles.get(position);
    }
}
