package com.thuantan.farshutter.Fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.victor.ringbutton.RingButton;
import com.thuantan.farshutter.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment {

    //region Fields

    private MainFragmentListener mListener;

    private RingButton mRingButton;

    //endregion

    //region Properties

    //endregion

    //region Constructors

    public MainFragment() {
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    //endregion

    //region Functions

    //endregion

    //region Methods

    public void setFragmentListener(MainFragmentListener listener){
        mListener = listener;
    }

    //endregion

    //region EventListeners

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_main, container, false);

        mRingButton = (RingButton) view.findViewById(R.id.ringButton);

        mRingButton.setOnClickListener(new RingButton.OnClickListener() {
            @Override
            public void clickUp() {
                if (mListener != null){
                    mListener.onCamera();
                }
            }

            @Override
            public void clickDown() {
                if (mListener != null){
                    mListener.onControl();
                }
            }
        });

        return view;
    }

    //endregion

    //region ChildClasses

    //endregion

    //region Interfaces

    public interface MainFragmentListener{
        void onCamera();
        void onControl();
    }

    //endregion
}
