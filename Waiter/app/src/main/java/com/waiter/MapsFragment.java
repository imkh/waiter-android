package com.waiter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.waiter.models.Event;

import java.util.ArrayList;
import java.util.List;

public class MapsFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private static final int PERMISSIONS_REQUEST_LOCATION = 1;
    private boolean mPermissionDenied = false;

    private MapView mMapView;
    private GoogleMap mGoogleMap;

    private OnFragmentInteractionListener mListener;

    private CoordinatorLayout mCoordinatorLayout;
    private View mBottomSheet;
    private BottomSheetBehavior mBottomSheetBehavior;

    private TextView mEventTitle;
    private TextView mEventPrice;
    private TextView mEventDescription;
    private TextView mEventAddress;
    private TextView mEventDate;
    private TextView mEventWaitersAvailable;
    private FloatingActionButton mFAB;
    private boolean showFAB = false;

    private Animation mGrowAnimation;
    private Animation mShrinkAnimation;

    public MapsFragment() {
        // Required empty public constructor
    }

    public static MapsFragment newInstance() {
        MapsFragment fragment = new MapsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_maps, container, false);

        /*
        ** Start Init Maps
         */
        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume();

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(this);
        // End Init Maps


        /*
        ** Start Bottom Sheet
         */
        mCoordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.coordinator);
        initializeUI();
        // End BottomSheet

        return rootView;
    }

    private void initializeUI() {
        mBottomSheet = mCoordinatorLayout.findViewById(R.id.event_bottom_sheet);
        mEventTitle = (TextView) mBottomSheet.findViewById(R.id.eventTitle);
        mEventPrice = (TextView) mBottomSheet.findViewById(R.id.eventPrice);
        mEventDescription = (TextView) mBottomSheet.findViewById(R.id.eventDescription);
        mEventAddress = (TextView) mBottomSheet.findViewById(R.id.eventAddress);
        mEventDate = (TextView) mBottomSheet.findViewById(R.id.eventDate);
        mEventWaitersAvailable = (TextView) mBottomSheet.findViewById(R.id.eventWaitersAvailable);
        mFAB = (FloatingActionButton) mCoordinatorLayout.findViewById(R.id.fab);
        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> listOfWaiters = new ArrayList<>();
                MainActivity.mEventList.add(new Event("58fc51e531087c0011378ebc",
                        "Event" + (MainActivity.mEventList.size() + 1),
                        "Description",
                        "Address",
                        48.8584 + MainActivity.mEventList.size(),
                        2.2945 + MainActivity.mEventList.size(),
                        "Date",
                        1,
                        listOfWaiters));
            }
        });

        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        mGrowAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.simple_grow);
        mShrinkAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.simple_shrink);

        mGrowAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mFAB.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) { }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

        mShrinkAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                mFAB.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Not used.
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset >= -1 && slideOffset <= 0) {
                    mFAB.animate().scaleX(slideOffset + 1).scaleY(slideOffset + 1).setDuration(0).start();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        checkLocationPermission();
        mGoogleMap.setOnMyLocationButtonClickListener(this);

        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.custom_marker_resized);

        LatLng latLng = new LatLng(48.8151239, 2.3631254); // Epitech Paris location
        for (int i = 0; i < MainActivity.mEventList.size(); i++) {
            latLng = new LatLng(MainActivity.mEventList.get(i).getLong(), MainActivity.mEventList.get(i).getLat());
            mGoogleMap.addMarker(new MarkerOptions().position(latLng).title(String.valueOf(i)).icon(icon));
        }
        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(14).build();
        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnMapClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (mFAB.getVisibility() == View.GONE) {
            mFAB.setScaleX(0);
            mFAB.setScaleY(0);
            mFAB.setVisibility(View.VISIBLE);
        }
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        int eventID = Integer.parseInt(marker.getTitle());

        mEventTitle.setText(MainActivity.mEventList.get(eventID).getName());
        //mEventPrice.setText();
        mEventDescription.setText(MainActivity.mEventList.get(eventID).getDescription());
        mEventAddress.setText(MainActivity.mEventList.get(eventID).getAddress());
        mEventDate.setText(MainActivity.mEventList.get(eventID).getDate());
        mEventWaitersAvailable.setText(getString(R.string.waiters_available, MainActivity.mEventList.get(eventID).getListOfWaiters().size()));
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        bottomSheetToPreviousState();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        } else {
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkLocationPermission();
                } else {
                    Toast.makeText(getActivity(), "Location permission denied.", Toast.LENGTH_LONG).show();
                    mPermissionDenied = true;
                }
            }
        }
    }

    private boolean bottomSheetToPreviousState() {
        mBottomSheet.scrollTo(0, 0);
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            mFAB.setVisibility(View.GONE);
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onResume() {
        mMapView.onResume();
        super.onResume();
//        if (mPermissionDenied) {
//            // Permission was not granted, display error dialog.
//            Toast.makeText(getActivity(), "Location permission denied.", Toast.LENGTH_LONG).show();
//            mPermissionDenied = false;
//        }
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mMapView.onLowMemory();
        super.onLowMemory();
    }

    public boolean onBackPressed() {
        return bottomSheetToPreviousState();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteractionMaps(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteractionMaps(Uri uri);
    }
}
