package edu.northeastern.hikerhub.hiker.fragment.home;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import edu.northeastern.hikerhub.R;
import edu.northeastern.hikerhub.stickerService.SendStickerActivity;


public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private RecyclerView topTrailsRecView;
    private RecyclerView likeTrailsRecView;
    private TrialRecViewAdapter topTrialRecViewAdapter;
    private TrialRecViewAdapter likeTrialRecViewAdapter;
    private FloatingActionButton btnLoadMap;
    private CardView cardViewTrail;
    private SearchView searchView;

    private Map<String, Trail> trailMap;

    private GoogleMap myMap;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location lastKnownLocation;
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        initiate(rootView);
        initRecViewData();

        return rootView;
    }

    private void initiate(View rootView) {
        topTrailsRecView = rootView.findViewById(R.id.topTrailsRecView);
        likeTrailsRecView = rootView.findViewById(R.id.likeTrailsRecView);
        btnLoadMap = rootView.findViewById(R.id.floatingLoadMap);
        searchView = rootView.findViewById(R.id.searchView);
        cardViewTrail = rootView.findViewById(R.id.parentTailMap);
        cardViewTrail.setVisibility(View.GONE);
        trailMap = CsvReader.readCsvFromAssets(getContext(),"trail.csv");
    }

    private void initRecViewData() {
        topTrialRecViewAdapter = new TrialRecViewAdapter(requireContext());
        topTrailsRecView.setAdapter(topTrialRecViewAdapter);
        topTrailsRecView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        likeTrialRecViewAdapter = new TrialRecViewAdapter(requireContext());
        likeTrailsRecView.setAdapter(likeTrialRecViewAdapter);
        likeTrailsRecView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        ArrayList<Trail> topTrails = new ArrayList<>();
        //TODO：get the current location for user
        double latitude = 47.5301011;
        double longitude = -122.0326191;
        Location currentLocation = new Location("");
        currentLocation.setLatitude(latitude);
        currentLocation.setLongitude(longitude);


        //location.distanceTo(previousLocation);
        //max heap
        PriorityQueue<String> nearbyTrailHeap = new PriorityQueue<>((a, b) -> {
            float dist1 = trailMap.get(a).getLocation().distanceTo(currentLocation);
            float dist2 = trailMap.get(b).getLocation().distanceTo(currentLocation);
            if (dist1 > dist2) {
                return -1;
            } else if (dist1 < dist2) {
                return 1;
            } else {
                return 0;
            }
        });
        int size = 10;
        for (String nameTrail : trailMap.keySet()) {
            nearbyTrailHeap.offer(nameTrail);
            if (nearbyTrailHeap.size() > size) {
                nearbyTrailHeap.poll();
            }
        }
        while (!nearbyTrailHeap.isEmpty()) {
            topTrails.add(0,trailMap.get(nearbyTrailHeap.poll()));
        }

        topTrialRecViewAdapter.setTrails(topTrails);

        ArrayList<Trail> likeTrails = new ArrayList<>();
        //min Heap
        PriorityQueue<Trail> mostLikeTrailHeap = new PriorityQueue<>(Comparator.comparingInt(Trail::getRecommendCount));
        for (Trail trail : trailMap.values()) {
            mostLikeTrailHeap.offer(trail);
            if (mostLikeTrailHeap.size() > size) {
                mostLikeTrailHeap.poll();
            }
        }
        while (!mostLikeTrailHeap.isEmpty()) {
            likeTrails.add(0, mostLikeTrailHeap.poll());
        }
        likeTrialRecViewAdapter.setTrails(likeTrails);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
            String apiKey = ai.metaData.getString("com.google.android.geo.API_KEY");
            Places.initialize(getContext(), apiKey);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("TAG", "Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.e("TAG", "Failed to load meta-data, NullPointer: " + e.getMessage());
        }

        // Get the SupportMapFragment from the layout file
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        // Initialize the GoogleMap object
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        btnLoadMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (topTrailsRecView.getVisibility() == View.VISIBLE) {
                    // Hide the RecyclerView and FloatingActionButton
                    topTrailsRecView.setVisibility(View.GONE);
                    likeTrailsRecView.setVisibility(View.GONE);
                    btnLoadMap.setImageResource(R.drawable.ic_list);
                    // Show the map
                    mapFragment.getView().setVisibility(View.VISIBLE);

                } else {
                    // Show the RecyclerView and FloatingActionButton
                    topTrailsRecView.setVisibility(View.VISIBLE);
                    likeTrailsRecView.setVisibility(View.VISIBLE);
                    searchView.setVisibility(View.VISIBLE);
                    btnLoadMap.setImageResource(R.drawable.ic_map);
                    // Hide the map
                    mapFragment.getView().setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // The map is ready, so it is safe to call getView() and hide the map.
        mapFragment.getView().setVisibility(View.GONE);
        myMap = googleMap;
        // Add a marker in Sydney, Australia and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        myMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        myMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        markerCurrentLocation();

        myMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (cardViewTrail.getVisibility() == View.GONE) {
                    cardViewTrail.setVisibility(View.VISIBLE);
                    TextView titleTextView = cardViewTrail.findViewById(R.id.txtTrailNameMap);
                    ImageView imgTrailMap = cardViewTrail.findViewById(R.id.imgTrailMap);
                    titleTextView.setText(marker.getTitle());
                    Glide.with(requireContext())
                            .asBitmap()
                            .load("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQA7ym5GXtH17pPLo0IvWcIONsQVInEtLeMMg&usqp=CAU")
                            .into(imgTrailMap);
                } else {
                    cardViewTrail.setVisibility(View.GONE);
                }


                return false;
            }
        });


        // Set a listener to the SearchView widget
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Use the Google Places API to search for the location
                PlacesClient placesClient = Places.createClient(getContext());
                AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
                FindAutocompletePredictionsRequest request =
                        FindAutocompletePredictionsRequest.builder()
                                .setSessionToken(token)
                                .setQuery(query)
                                .build();
                placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener((response) -> {
                            if (!response.getAutocompletePredictions().isEmpty()) {
                                AutocompletePrediction prediction = response.getAutocompletePredictions().get(0);
                                String placeId = prediction.getPlaceId();
                                List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);
                                FetchPlaceRequest placeRequest = FetchPlaceRequest.builder(placeId, placeFields).build();
                                placesClient.fetchPlace(placeRequest)
                                        .addOnSuccessListener((placeResponse) -> {
                                            Place place = placeResponse.getPlace();
                                            LatLng latLng = place.getLatLng();
                                            if (latLng != null) {
                                                // Move the camera to the new location
                                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                                        .target(latLng)
                                                        .zoom(12)
                                                        .build();
                                                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                            }
                                        });
                            }
                        });

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });



    }
    private void markerCurrentLocation() {
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Get the last known location
            mFusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        lastKnownLocation = location;
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        myMap.addMarker(new MarkerOptions().position(latLng).title("Issaquah"));
                        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
                    }
                }
            });
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }
}

