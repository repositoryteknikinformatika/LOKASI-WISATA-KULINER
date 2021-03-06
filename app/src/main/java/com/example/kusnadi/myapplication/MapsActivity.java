package com.example.kusnadi.myapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.example.kusnadi.myapplication.data.DatabaseHandler;
import com.example.kusnadi.myapplication.utils.PermissionUtil;
import com.example.kusnadi.myapplication.utils.Tools;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;

import java.util.HashMap;
import java.util.List;

import com.example.kusnadi.myapplication.data.Constant;
import com.example.kusnadi.myapplication.model.Category;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;


/**
 * Created by Kusnadi on 12/9/2016.
 */

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback{

    public static final String EXTRA_OBJ = "key.EXTRA_OBJ";

    private GoogleMap mMap;
    private Toolbar toolbar;
    private ActionBar actionBar;
    private DatabaseHandler db;
    private ClusterManager<com.example.kusnadi.myapplication.model.Place> mClusterManager;
    private View parent_view;
    private int cat[];
    private PlaceMarkerRenderer placeMarkerRenderer;

    private com.example.kusnadi.myapplication.model.Place ext_place = null;
    private boolean isSinglePlace;
    HashMap<String, com.example.kusnadi.myapplication.model.Place> hashMapPlaces = new HashMap<>();

    // id category
    private int cat_id = -1;

    private Category cur_category;

    private ImageView icon, marker_bg;
    private View marker_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        parent_view = findViewById(R.id.main_content);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        marker_view = inflater.inflate(R.layout.maps_marker, null);
        icon = (ImageView) marker_view.findViewById(R.id.marker_icon);
        marker_bg = (ImageView) marker_view.findViewById(R.id.marker_bg);

        ext_place = (com.example.kusnadi.myapplication.model.Place) getIntent().getSerializableExtra(EXTRA_OBJ);
        isSinglePlace = (ext_place != null);

        db = new DatabaseHandler(this);
        initMapFragment();
        initToolbar();

        cat = getResources().getIntArray(R.array.id_category);

        Tools.systemBarLolipop(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = Tools.configActivityMaps(googleMap);
        CameraUpdate location;
        if (isSinglePlace) {
            marker_bg.setColorFilter(getResources().getColor(R.color.marker_secondary));
            MarkerOptions markerOptions = new MarkerOptions().title(ext_place.name).position(ext_place.getPosition());
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Tools.createBitmapFromView(MapsActivity.this, marker_view)));
            mMap.addMarker(markerOptions);
            location = CameraUpdateFactory.newLatLngZoom(ext_place.getPosition(), 12);
            actionBar.setTitle(ext_place.name);
        } else {
            location = CameraUpdateFactory.newLatLngZoom(new LatLng(Constant.city_lat, Constant.city_lng), 9);
            mClusterManager = new ClusterManager<>(this, mMap);
            placeMarkerRenderer = new PlaceMarkerRenderer(this, mMap, mClusterManager);
            mClusterManager.setRenderer(placeMarkerRenderer);
            mMap.setOnCameraChangeListener(mClusterManager);
        }
        mMap.animateCamera(location);

        showMyLocation();
    }

    private void showMyLocation() {
        if (PermissionUtil.isLocationGranted(this)) {
            // Enable / Disable my location button
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    try {
                        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            showAlertDialogGps();
                        } else {
                            Location loc = Tools.getLastKnownLocation(MapsActivity.this);
                            CameraUpdate myCam = CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 12);
                            mMap.animateCamera(myCam);
                        }
                    } catch (Exception e) {
                    }
                    return true;
                }
            });
        }
    }

    private void loadClusterManager(List<com.example.kusnadi.myapplication.model.Place> places) {
        mClusterManager.clearItems();
        mClusterManager.addItems(places);
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.activity_title_maps);
        Tools.setActionBarColor(this, actionBar);
    }

    private void initMapFragment() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    private class PlaceMarkerRenderer extends DefaultClusterRenderer<com.example.kusnadi.myapplication.model.Place> {
        public PlaceMarkerRenderer(Context context, GoogleMap map, ClusterManager<com.example.kusnadi.myapplication.model.Place> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(com.example.kusnadi.myapplication.model.Place item, MarkerOptions markerOptions) {
            if (cat_id == -1) { // all place
                icon.setImageResource(R.drawable.round_shape);
            } else {
                icon.setImageResource(cur_category.icon);
            }
            marker_bg.setColorFilter(getResources().getColor(R.color.marker_primary));
            markerOptions.title(item.name);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Tools.createBitmapFromView(MapsActivity.this, marker_view)));
            if (ext_place != null && ext_place.place_id == item.place_id) {
                markerOptions.visible(false);
            }

        }

        @Override
        protected void onClusterItemRendered(com.example.kusnadi.myapplication.model.Place item, Marker marker) {
            hashMapPlaces.put(marker.getId(), item);
            super.onClusterItemRendered(item, marker);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        } else {
            String category_text;
            if (item.getItemId() != R.id.menu_category) {
                category_text = item.getTitle().toString();
                switch (item.getItemId()) {
                    case R.id.nav_all:
                        cat_id = -1;
                        break;
                    case R.id.nav_featured:
                        cat_id = cat[10];
                        break;
                    case R.id.nav_tour:
                        cat_id = cat[0];
                        break;
                    case R.id.nav_food:
                        cat_id = cat[1];
                        break;
                    case R.id.nav_hotels:
                        cat_id = cat[2];
                        break;
                    case R.id.nav_ent:
                        cat_id = cat[3];
                        break;
                    case R.id.nav_sport:
                        cat_id = cat[4];
                        break;
                    case R.id.nav_shop:
                        cat_id = cat[5];
                        break;
                    case R.id.nav_transport:
                        cat_id = cat[6];
                        break;
                    case R.id.nav_religion:
                        cat_id = cat[7];
                        break;
                    case R.id.nav_public:
                        cat_id = cat[8];
                        break;
                    case R.id.nav_money:
                        cat_id = cat[9];
                        break;
                }

                // get category object when menu click
                cur_category = db.getCategory(cat_id);

                if (isSinglePlace) {
                    isSinglePlace = false;
                    mClusterManager = new ClusterManager<>(this, mMap);
                    mMap.setOnCameraChangeListener(mClusterManager);
                }

                List<com.example.kusnadi.myapplication.model.Place> places = db.getAllPlaceByCategory(cat_id);
                loadClusterManager(places);
                if (places.size() == 0) {
                    Snackbar.make(parent_view, getString(R.string.no_item_at) + " " + item.getTitle().toString(), Snackbar.LENGTH_LONG).show();
                }
                placeMarkerRenderer = new PlaceMarkerRenderer(this, mMap, mClusterManager);
                mClusterManager.setRenderer(placeMarkerRenderer);

                actionBar.setTitle(category_text);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAlertDialogGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_content_gps);
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }


}
