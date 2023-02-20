package com.here.sdkExample.joshGonzExample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.LocationDataSourceHERE;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.ftcr.FTCRLaneInformation;
import com.here.android.mpa.ftcr.FTCRManeuver;
import com.here.android.mpa.ftcr.FTCRNavigationManager;
import com.here.android.mpa.ftcr.FTCRRoute;
import com.here.android.mpa.ftcr.FTCRRouteOptions;
import com.here.android.mpa.ftcr.FTCRRoutePlan;
import com.here.android.mpa.ftcr.FTCRRouter;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.FTCRMapRoute;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.routing.RoutingZone;
import com.here.android.positioning.StatusListener;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MapFragmentView implements PositioningManager.OnPositionChangedListener, Map.OnTransformListener {
    private AndroidXMapFragment m_mapFragment;
    private final AppCompatActivity m_activity;
    private Map m_map;
    private FTCRMapRoute m_mapRoute;
    private FTCRRoute m_route;
    private FTCRNavigationManager m_navigationManager;
    private boolean m_isNavActive = false;
    // positioning manager instance
    private PositioningManager mPositioningManager;

    // HERE location data source instance
    private LocationDataSourceHERE mHereLocation;

    // flag that indicates whether maps is being transformed
    private boolean mTransforming;

    // callback that is called when transforming ends
    private Runnable mPendingUpdate;

    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;
        initMapFragment();
        /*
         * We use a button in this example to control the route calculation
         */
        initCreateRouteButton();
        initCreateNavButton();
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) m_activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = getMapFragment();
        m_mapFragment.setRetainInstance(false);

        if (m_mapFragment != null) {
            /* Initialize the AndroidXMapFragment, results will be given via the called back. */
            m_mapFragment.init(error -> {

                if (error == OnEngineInitListener.Error.NONE) {
                    /* get the map object */
                    m_map = m_mapFragment.getMap();
                    /*
                     * Set the map center to the south of Berlin.
                     */
                    assert m_map != null;
                    m_map.setCenter(new GeoCoordinate(52.406425, 13.193975, 0.0),
                            Map.Animation.NONE);

                    /* Set the zoom level to the average between min and max zoom level. */
                    m_map.setZoomLevel((m_map.getMaxZoomLevel() + m_map.getMinZoomLevel()) / 2);

                    m_map.addTransformListener(MapFragmentView.this);
                    mPositioningManager = PositioningManager.getInstance();
                    mHereLocation = LocationDataSourceHERE.getInstance(
                            new StatusListener() {
                                @Override
                                public void onOfflineModeChanged(boolean offline) {
                                    // called when offline mode changes
                                }

                                @Override
                                public void onAirplaneModeEnabled() {
                                    // called when airplane mode is enabled
                                }

                                @Override
                                public void onWifiScansDisabled() {
                                    // called when Wi-Fi scans are disabled
                                }

                                @Override
                                public void onBluetoothDisabled() {
                                    // called when Bluetooth is disabled
                                }

                                @Override
                                public void onCellDisabled() {
                                    // called when Cell radios are switch off
                                }

                                @Override
                                public void onGnssLocationDisabled() {
                                    // called when GPS positioning is disabled
                                }

                                @Override
                                public void onNetworkLocationDisabled() {
                                    // called when network positioning is disabled
                                }

                                @Override
                                public void onServiceError(ServiceError serviceError) {
                                    // called on HERE service error
                                }

                                @Override
                                public void onPositioningError(PositioningError positioningError) {
                                    // called when positioning fails
                                }

                                @Override
                                public void onWifiIndoorPositioningNotAvailable() {
                                    // called when running on Android 9.0 (Pie) or newer
                                }

                                @Override
                                public void onWifiIndoorPositioningDegraded() {
                                    // called when running on Android 9.0 (Pie) or newer
                                }
                            });

                    mPositioningManager.setDataSource(mHereLocation);
                    mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(
                            MapFragmentView.this));
                    // start position updates, accepting GPS, network or indoor positions
                    if (mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR)) {
                        Objects.requireNonNull(m_mapFragment.getPositionIndicator()).setVisible(true);
                    } else {
                        Toast.makeText(getMapFragment().getActivity(), "PositioningManager.start: failed, exiting", Toast.LENGTH_LONG).show();
                        m_activity.finish();
                    }
                } else {
                    new AlertDialog.Builder(m_activity).setMessage(
                            "Error : " + error.name() + "\n\n" + error.getDetails())
                            .setTitle(R.string.engine_init_error)
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            m_activity.finish();
                                        }
                                    }).create().show();
                }
            });
        }
    }

    private void initCreateRouteButton() {
        Button m_createNavButton = m_activity.findViewById(R.id.buttonNav);

        m_createNavButton.setOnClickListener(v -> {
            if (m_route != null) {
                if (m_isNavActive) {
                    m_navigationManager.stop();
                    m_isNavActive = false;
                } else {
                    startNavigation(m_route);
                    m_isNavActive =  true;
                }
            }
        });
    }

    private void initCreateNavButton() {
        Button m_createRouteButton = (Button) m_activity.findViewById(R.id.buttonRoute);

        m_createRouteButton.setOnClickListener(v -> {
            m_map.removeMapObject(m_mapRoute);
            m_mapRoute = null;
            createRoute(Collections.emptyList());
        });
    }

    private void createRoute(final List<RoutingZone> excludedRoutingZones) {
        /* Initialize a CoreRouter */
        FTCRRouter coreRouter = new FTCRRouter();

        /*
         * Initialize a RouteOption. HERE Mobile SDK allow users to define their own parameters for the
         * route calculation,including transport modes,route types and route restrictions etc.Please
         * refer to API doc for full list of APIs
         */
        FTCRRouteOptions routeOptions = new FTCRRouteOptions();
        /* Other transport modes are also available e.g Pedestrian */
        routeOptions.setTransportMode(FTCRRouteOptions.TransportMode.CAR);
        /* Disable highway in this route. */
        //routeOptions.setHighwaysAllowed(false);
        /* Calculate the shortest route available. */
        routeOptions.setRouteType(FTCRRouteOptions.Type.SHORTEST);
        /* Calculate 1 route. */
        //routeOptions.setRouteCount(1);
        /* Exclude routing zones. */
        /*if (!excludedRoutingZones.isEmpty()) {
            routeOptions.excludeRoutingZones(toStringIds(excludedRoutingZones));
        }*/

        /* Finally set the route option */
        //routePlan.setRouteOptions(routeOptions);

        /* Define waypoints for the route */
        /* START: South of Berlin */
        RouteWaypoint startPoint = new RouteWaypoint(m_map.getCenter());
        /* END: North of Berlin */
        RouteWaypoint destination = new RouteWaypoint(new GeoCoordinate(20.967684, -89.624133));

        /* Add both waypoints to the route plan */
        //routePlan.addWaypoint(startPoint);
        //routePlan.addWaypoint(destination);

        List<RouteWaypoint> waypoints = Arrays.asList(startPoint, destination);

        /* Initialize a RoutePlan */
        FTCRRoutePlan routePlan = new FTCRRoutePlan(waypoints, routeOptions);

        /* Trigger the route calculation,results will be called back via the listener */
        coreRouter.calculateRoute(routePlan, (list, errorResponse) -> {
            if (errorResponse.getErrorCode() == RoutingError.NONE && !list.isEmpty()) {
                m_route = list.get(0);


                /* Create a MapRoute so that it can be placed on the map */
                m_mapRoute = new FTCRMapRoute(m_route);

                /* Show the maneuver number on top of the route */
                //m_mapRoute.setManeuverNumberVisible(true);

                /* Add the MapRoute to the map */
                m_map.addMapObject(m_mapRoute);

                /*
                 * We may also want to make sure the map view is orientated properly
                 * so the entire route can be easily seen.
                 */
                m_map.zoomTo(m_route.getBoundingBox(), Map.Animation.NONE,
                        Map.MOVE_PRESERVE_ORIENTATION);
            } else {
                Toast.makeText(m_activity,
                        "Error:route calculation returned error code: " + errorResponse,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    void startNavigation(FTCRRoute route) {
        // add route to the map
        //m_mapRoute = new FTCRMapRoute(route);
        //m_map.addMapObject(m_mapRoute);
        // set first route coordinate as map center
        m_map.setCenter(route.getGeometry().get(0), Map.Animation.NONE, 15.0f, 0f, 0f);
        // show indicator on the map
        Objects.requireNonNull(m_mapFragment.getPositionIndicator()).setVisible(true);

        m_navigationManager = new FTCRNavigationManager();

        m_navigationManager.setMap(m_map);
        m_navigationManager.setMapTrackingTilt(FTCRNavigationManager.TrackingTilt.TILT3D);
        m_navigationManager.setMapTrackingMode(FTCRNavigationManager.TrackingMode.FOLLOW);
        m_navigationManager.addNavigationListener(navListener);
        // also supports simulation and navigation using PositionSimulator
        m_navigationManager.simulate(route, 60);
        //m_navigationManager.start(route);
    }

    FTCRNavigationManager.FTCRNavigationManagerListener navListener
            = new FTCRNavigationManager.FTCRNavigationManagerListener() {
        @Override
        public void onStopoverReached(int index) {
        }

        @Override
        public void onDestinationReached() {
        }

        @Override
        public void onRerouteBegin() {
        }

        @Override
        public void onLaneInformation(@NonNull List<FTCRLaneInformation> lanes) {
        }

        @Override
        public void onCurrentManeuverChanged(@Nullable FTCRManeuver currentManeuver,
                                             @Nullable FTCRManeuver nextManeuver) {
        }

        @Override
        public void onRerouteEnd(@Nullable FTCRRoute newRoute,
                                 @NonNull FTCRRouter.ErrorResponse error) {
            // We must remove old route from the map and add new one, SDK does not do that
            // automatically
            if (error.getErrorCode() == RoutingError.NONE) {
                m_map.removeMapObject(m_mapRoute);
                assert newRoute != null;
                m_mapRoute = new FTCRMapRoute(newRoute);
                m_map.addMapObject(m_mapRoute);
            }
        }
    };

    @Override
    public void onPositionUpdated(PositioningManager.LocationMethod locationMethod, @Nullable GeoPosition geoPosition, boolean b) {
        assert geoPosition != null;
        final GeoCoordinate coordinate = geoPosition.getCoordinate();
        if (mTransforming) {
            mPendingUpdate = new Runnable() {
                @Override
                public void run() {
                    onPositionUpdated(locationMethod, geoPosition, b);
                }
            };
        } else {
            m_map.setCenter(coordinate, Map.Animation.BOW);
            //updateLocationInfo(locationMethod, geoPosition);
        }
    }

    @Override
    public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {
        // ignored
    }

    @Override
    public void onMapTransformStart() {
        mTransforming = true;
    }

    @Override
    public void onMapTransformEnd(MapState mapState) {
        mTransforming = false;
        if (mPendingUpdate != null) {
            mPendingUpdate.run();
            mPendingUpdate = null;
        }
    }
}