package com.vtv.farshutter.Activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.vtv.farshutter.Fragment.CameraFragment;
import com.vtv.farshutter.Fragment.ControlFragment;
import com.vtv.farshutter.Fragment.MainFragment;
import com.vtv.farshutter.R;

public class MainActivity extends AppCompatActivity {

    //region Fields

    private MainFragment mFgMain;
    private CameraFragment mFgCamera;
    private ControlFragment mFgControl;

    private FragmentManager mFragmentManager;

    //endregion

    //region Properties

    //endregion

    //region Constructors

    //endregion

    //region Functions

    //endregion

    //region Methods

    //endregion

    //region EventListeners

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFragmentManager = getSupportFragmentManager();

        mFgMain = MainFragment.newInstance();
        mFgCamera = CameraFragment.newInstance();
        mFgControl = ControlFragment.newInstance();

        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.fragment, mFgMain);
        transaction.commit();

        mFgMain.setFragmentListener(new MainFragment.MainFragmentListener() {
            @Override
            public void onCamera() {
                FragmentTransaction transaction = mFragmentManager.beginTransaction();

                transaction.replace(R.id.fragment, mFgCamera);
                transaction.addToBackStack(null);

                transaction.commit();
            }

            @Override
            public void onControl() {
                FragmentTransaction transaction = mFragmentManager.beginTransaction();

                transaction.replace(R.id.fragment, mFgControl);
                transaction.addToBackStack(null);

                transaction.commit();
            }
        });
    }

    //endregion

    //region ChildClasses

    //endregion

    //region Interfaces

    //endregion

}
