package org.ushastoe.fluffy.maps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.IMapsProvider;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class OsmMapsProvider implements IMapsProvider {

    @Override
    public void initializeMaps(Context context) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        Configuration.getInstance().setUserAgentValue(context.getPackageName());
    }

    @Override
    public IMapView onCreateMapView(Context context) {
        return new OsmMapViewImpl(context);
    }

    @Override
    public IMarkerOptions onCreateMarkerOptions() {
        return new OsmMarkerOptions();
    }

    @Override
    public ICircleOptions onCreateCircleOptions() {
        return new OsmCircleOptions();
    }

    @Override
    public ILatLngBoundsBuilder onCreateLatLngBoundsBuilder() {
        return new OsmLatLngBoundsBuilder();
    }

    @Override
    public ICameraUpdate newCameraUpdateLatLng(LatLng latLng) {
        return new OsmCameraUpdate(latLng, Float.NaN, null, 0);
    }

    @Override
    public ICameraUpdate newCameraUpdateLatLngZoom(LatLng latLng, float zoom) {
        return new OsmCameraUpdate(latLng, zoom, null, 0);
    }

    @Override
    public ICameraUpdate newCameraUpdateLatLngBounds(ILatLngBounds bounds, int padding) {
        return new OsmCameraUpdate(null, Float.NaN, (OsmLatLngBounds) bounds, padding);
    }

    @Override
    public IMapStyleOptions loadRawResourceStyle(Context context, int resId) {
        return null;
    }

    @Override
    public String getMapsAppPackageName() {
        return ApplicationLoader.applicationContext.getPackageName();
    }

    @Override
    public int getInstallMapsString() {
        return R.string.InstallGoogleMaps;
    }

    private static final class OsmMapViewImpl implements IMapView {
        private final InterceptableMapView mapView;
        private final OsmMapImpl map;
        private ITouchInterceptor dispatchInterceptor;
        private ITouchInterceptor interceptInterceptor;
        private Runnable onLayoutListener;

        private OsmMapViewImpl(Context context) {
            mapView = new InterceptableMapView(context);
            map = new OsmMapImpl(mapView);
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.setTilesScaledToDpi(true);
            mapView.setOnTouchCallback(map::onTouchEvent);
        }

        @Override
        public View getView() {
            return mapView;
        }

        @Override
        public void getMapAsync(Consumer<IMap> callback) {
            mapView.post(() -> callback.accept(map));
        }

        @Override
        public void onResume() {
            mapView.onResume();
            map.onResume();
        }

        @Override
        public void onPause() {
            map.onPause();
            mapView.onPause();
        }

        @Override
        public void onCreate(Bundle savedInstance) {
            map.dispatchLoaded();
        }

        @Override
        public void onDestroy() {
            map.destroy();
            mapView.onDetach();
        }

        @Override
        public void onLowMemory() {
        }

        @Override
        public void setOnDispatchTouchEventInterceptor(ITouchInterceptor touchInterceptor) {
            dispatchInterceptor = touchInterceptor;
        }

        @Override
        public void setOnInterceptTouchEventInterceptor(ITouchInterceptor touchInterceptor) {
            interceptInterceptor = touchInterceptor;
        }

        @Override
        public void setOnLayoutListener(Runnable callback) {
            onLayoutListener = callback;
        }

        private final class InterceptableMapView extends MapView {
            private Consumer<MotionEvent> onTouchCallback;

            private InterceptableMapView(Context context) {
                super(context);
            }

            private void setOnTouchCallback(Consumer<MotionEvent> callback) {
                onTouchCallback = callback;
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (onTouchCallback != null) {
                    onTouchCallback.accept(ev);
                }
                if (dispatchInterceptor != null) {
                    return dispatchInterceptor.onInterceptTouchEvent(ev, super::dispatchTouchEvent);
                }
                return super.dispatchTouchEvent(ev);
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (interceptInterceptor != null) {
                    return interceptInterceptor.onInterceptTouchEvent(ev, super::onInterceptTouchEvent);
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                map.dispatchLoaded();
                if (onLayoutListener != null) {
                    onLayoutListener.run();
                }
            }
        }
    }

    private static final class OsmMapImpl implements IMap, MapListener {
        private static final long IDLE_DELAY_MS = 180L;

        private final MapView mapView;
        private final MapController controller;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final WeakHashMap<Marker, OsmMarker> markerMap = new WeakHashMap<>();
        private final WeakHashMap<Polygon, OsmCircle> circleMap = new WeakHashMap<>();
        private final WeakHashMap<Marker, Object> markerTags = new WeakHashMap<>();
        private final LocationManager locationManager;
        private final LocationListener locationListener;
        private Runnable cameraIdleListener;
        private Runnable cameraMoveListener;
        private Runnable mapLoadedCallback;
        private Consumer<Location> myLocationCallback;
        private OnMarkerClickListener markerClickListener;
        private OnCameraMoveStartedListener cameraMoveStartedListener;
        private final Runnable idleNotifier = () -> {
            if (cameraIdleListener != null) {
                cameraIdleListener.run();
            }
        };

        private MyLocationNewOverlay locationOverlay;
        private boolean mapLoadedDispatched;
        private boolean myLocationEnabled;
        private boolean locationRegistered;

        private OsmMapImpl(MapView mapView) {
            this.mapView = mapView;
            this.controller = (MapController) mapView.getController();
            this.locationManager = (LocationManager) mapView.getContext().getSystemService(Context.LOCATION_SERVICE);
            this.locationListener = location -> {
                if (myLocationCallback != null && location != null) {
                    myLocationCallback.accept(location);
                }
            };
            mapView.addMapListener(this);
            mapView.setMultiTouchControls(true);
            mapView.setBuiltInZoomControls(false);
            mapView.setMinZoomLevel(2.0);
            mapView.setMaxZoomLevel(20.0);
        }

        @Override
        public void setMapType(int mapType) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
        }

        @Override
        public void animateCamera(ICameraUpdate update) {
            applyCameraUpdate((OsmCameraUpdate) update, true, 300, null);
        }

        @Override
        public void animateCamera(ICameraUpdate update, ICancelableCallback callback) {
            applyCameraUpdate((OsmCameraUpdate) update, true, 300, callback);
        }

        @Override
        public void animateCamera(ICameraUpdate update, int duration, ICancelableCallback callback) {
            applyCameraUpdate((OsmCameraUpdate) update, true, duration, callback);
        }

        @Override
        public void moveCamera(ICameraUpdate update) {
            applyCameraUpdate((OsmCameraUpdate) update, false, 0, null);
        }

        @Override
        public float getMaxZoomLevel() {
            Double zoom = mapView.getMaxZoomLevel();
            return zoom != null ? zoom.floatValue() : 20.0f;
        }

        @Override
        public float getMinZoomLevel() {
            Double zoom = mapView.getMinZoomLevel();
            return zoom != null ? zoom.floatValue() : 2.0f;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void setMyLocationEnabled(boolean enabled) {
            myLocationEnabled = enabled;
            if (enabled) {
                ensureLocationOverlay();
                locationOverlay.enableMyLocation();
            } else if (locationOverlay != null) {
                locationOverlay.disableMyLocation();
            }
            updateLocationRegistration();
            mapView.invalidate();
        }

        @Override
        public IUISettings getUiSettings() {
            return new OsmUiSettings(mapView);
        }

        @Override
        public void setOnCameraIdleListener(Runnable callback) {
            cameraIdleListener = callback;
        }

        @Override
        public void setOnCameraMoveStartedListener(OnCameraMoveStartedListener onCameraMoveStartedListener) {
            cameraMoveStartedListener = onCameraMoveStartedListener;
        }

        @Override
        public CameraPosition getCameraPosition() {
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            return new CameraPosition(new LatLng(center.getLatitude(), center.getLongitude()), (float) mapView.getZoomLevelDouble());
        }

        @Override
        public void setOnMapLoadedCallback(Runnable callback) {
            mapLoadedCallback = callback;
            dispatchLoaded();
        }

        @Override
        public IProjection getProjection() {
            return latLng -> mapView.getProjection().toPixels(new GeoPoint(latLng.latitude, latLng.longitude), new Point());
        }

        @Override
        public void setPadding(int left, int top, int right, int bottom) {
            mapView.setPadding(left, top, right, bottom);
        }

        @Override
        public void setMapStyle(IMapStyleOptions style) {
        }

        @Override
        public IMarker addMarker(IMarkerOptions markerOptions) {
            OsmMarkerOptions options = (OsmMarkerOptions) markerOptions;
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(options.position.latitude, options.position.longitude));
            marker.setAnchor(options.anchorU, options.anchorV);
            marker.setTitle(options.title);
            marker.setSubDescription(options.snippet);
            if (options.iconBitmap != null) {
                marker.setIcon(new BitmapDrawable(mapView.getResources(), options.iconBitmap));
            } else if (options.iconResId != 0) {
                marker.setIcon(ContextCompat.getDrawable(mapView.getContext(), options.iconResId));
            }
            marker.setOnMarkerClickListener((clickedMarker, ignoredMapView) -> {
                OsmMarker abs = markerMap.get(clickedMarker);
                if (abs == null) {
                    abs = new OsmMarker(clickedMarker);
                    markerMap.put(clickedMarker, abs);
                }
                return markerClickListener != null && markerClickListener.onClick(abs);
            });
            mapView.getOverlays().add(marker);
            OsmMarker osmMarker = new OsmMarker(marker);
            markerMap.put(marker, osmMarker);
            mapView.invalidate();
            return osmMarker;
        }

        @Override
        public void setOnMyLocationChangeListener(Consumer<Location> callback) {
            myLocationCallback = callback;
            updateLocationRegistration();
        }

        @Override
        public void setOnMarkerClickListener(OnMarkerClickListener markerClickListener) {
            this.markerClickListener = markerClickListener;
        }

        @Override
        public void setOnCameraMoveListener(Runnable callback) {
            cameraMoveListener = callback;
        }

        @Override
        public ICircle addCircle(ICircleOptions circleOptions) {
            OsmCircleOptions options = (OsmCircleOptions) circleOptions;
            Polygon polygon = new Polygon();
            polygon.setPoints(Polygon.pointsAsCircle(new GeoPoint(options.center.latitude, options.center.longitude), options.radius));
            polygon.setStrokeColor(options.strokeColor);
            polygon.setFillColor(options.fillColor);
            polygon.setStrokeWidth(options.strokeWidth);
            mapView.getOverlays().add(polygon);
            OsmCircle circle = new OsmCircle(polygon, options.center, options.radius);
            circleMap.put(polygon, circle);
            mapView.invalidate();
            return circle;
        }

        @Override
        public boolean onScroll(ScrollEvent event) {
            dispatchMoveCallbacks();
            return false;
        }

        @Override
        public boolean onZoom(ZoomEvent event) {
            dispatchMoveCallbacks();
            return false;
        }

        private void dispatchMoveCallbacks() {
            if (cameraMoveListener != null) {
                cameraMoveListener.run();
            }
            handler.removeCallbacks(idleNotifier);
            handler.postDelayed(idleNotifier, IDLE_DELAY_MS);
        }

        private void applyCameraUpdate(OsmCameraUpdate update, boolean animated, int duration, ICancelableCallback callback) {
            if (update == null) {
                return;
            }
            if (cameraMoveStartedListener != null) {
                cameraMoveStartedListener.onCameraMoveStarted(animated
                        ? OnCameraMoveStartedListener.REASON_API_ANIMATION
                        : OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION);
            }

            Runnable finishCallback = callback == null ? null : callback::onFinish;
            if (update.bounds != null) {
                Runnable apply = () -> {
                    mapView.zoomToBoundingBox(update.bounds.boundingBox, animated, update.padding);
                    dispatchMoveCallbacks();
                    if (finishCallback != null) {
                        handler.postDelayed(finishCallback, Math.max(duration, 200));
                    }
                };
                if (mapView.getWidth() == 0 || mapView.getHeight() == 0) {
                    mapView.post(apply);
                } else {
                    apply.run();
                }
                return;
            }

            GeoPoint geoPoint = new GeoPoint(update.target.latitude, update.target.longitude);
            if (!Float.isNaN(update.zoom)) {
                mapView.getController().setZoom((double) update.zoom);
            }
            if (animated) {
                controller.animateTo(geoPoint);
            } else {
                controller.setCenter(geoPoint);
            }
            dispatchMoveCallbacks();
            if (finishCallback != null) {
                handler.postDelayed(finishCallback, Math.max(duration, 200));
            }
        }

        private void dispatchLoaded() {
            if (mapLoadedDispatched || mapLoadedCallback == null || mapView.getWidth() == 0 || mapView.getHeight() == 0) {
                return;
            }
            mapLoadedDispatched = true;
            mapView.post(mapLoadedCallback);
        }

        private void onResume() {
            updateLocationRegistration();
        }

        private void onPause() {
            unregisterLocationUpdates();
        }

        private void destroy() {
            unregisterLocationUpdates();
            if (locationOverlay != null) {
                locationOverlay.disableMyLocation();
                mapView.getOverlays().remove(locationOverlay);
                locationOverlay = null;
            }
            handler.removeCallbacksAndMessages(null);
            mapView.getOverlays().clear();
        }

        private void onTouchEvent(MotionEvent ev) {
            if (cameraMoveStartedListener != null && ev != null && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                cameraMoveStartedListener.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_GESTURE);
            }
        }

        private void ensureLocationOverlay() {
            if (locationOverlay != null) {
                return;
            }
            locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(mapView.getContext()), mapView);
            mapView.getOverlays().add(locationOverlay);
        }

        @SuppressLint("MissingPermission")
        private void updateLocationRegistration() {
            boolean needUpdates = myLocationEnabled || myLocationCallback != null;
            if (!needUpdates || locationManager == null) {
                unregisterLocationUpdates();
                return;
            }
            if (locationRegistered) {
                return;
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener, Looper.getMainLooper());
            } catch (Throwable ignore) {
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, locationListener, Looper.getMainLooper());
            } catch (Throwable ignore) {
            }
            locationRegistered = true;
        }

        private void unregisterLocationUpdates() {
            if (!locationRegistered || locationManager == null) {
                return;
            }
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Throwable ignore) {
            }
            locationRegistered = false;
        }

        private final class OsmMarker implements IMarker {
            private final Marker marker;

            private OsmMarker(Marker marker) {
                this.marker = marker;
            }

            @Override
            public Object getTag() {
                return markerTags.get(marker);
            }

            @Override
            public void setTag(Object tag) {
                markerTags.put(marker, tag);
            }

            @Override
            public LatLng getPosition() {
                GeoPoint point = marker.getPosition();
                return new LatLng(point.getLatitude(), point.getLongitude());
            }

            @Override
            public void setPosition(LatLng latLng) {
                marker.setPosition(new GeoPoint(latLng.latitude, latLng.longitude));
                mapView.invalidate();
            }

            @Override
            public void setRotation(int rotation) {
                marker.setRotation(rotation);
                mapView.invalidate();
            }

            @Override
            public void setIcon(Bitmap bitmap) {
                marker.setIcon(new BitmapDrawable(mapView.getResources(), bitmap));
                mapView.invalidate();
            }

            @Override
            public void setIcon(int resId) {
                marker.setIcon(ContextCompat.getDrawable(mapView.getContext(), resId));
                mapView.invalidate();
            }

            @Override
            public void remove() {
                mapView.getOverlays().remove(marker);
                markerMap.remove(marker);
                markerTags.remove(marker);
                mapView.invalidate();
            }
        }

        private final class OsmCircle implements ICircle {
            private final Polygon polygon;
            private LatLng center;
            private double radius;

            private OsmCircle(Polygon polygon, LatLng center, double radius) {
                this.polygon = polygon;
                this.center = center;
                this.radius = radius;
            }

            @Override
            public void setStrokeColor(int color) {
                polygon.setStrokeColor(color);
                mapView.invalidate();
            }

            @Override
            public void setFillColor(int color) {
                polygon.setFillColor(color);
                mapView.invalidate();
            }

            @Override
            public void setRadius(double radius) {
                this.radius = radius;
                updatePoints();
            }

            @Override
            public double getRadius() {
                return radius;
            }

            @Override
            public void setCenter(LatLng latLng) {
                center = latLng;
                updatePoints();
            }

            @Override
            public void remove() {
                mapView.getOverlays().remove(polygon);
                circleMap.remove(polygon);
                mapView.invalidate();
            }

            private void updatePoints() {
                polygon.setPoints(Polygon.pointsAsCircle(new GeoPoint(center.latitude, center.longitude), radius));
                mapView.invalidate();
            }
        }
    }

    private static final class OsmUiSettings implements IUISettings {
        private final MapView mapView;

        private OsmUiSettings(MapView mapView) {
            this.mapView = mapView;
        }

        @Override
        public void setZoomControlsEnabled(boolean enabled) {
            mapView.setBuiltInZoomControls(enabled);
        }

        @Override
        public void setMyLocationButtonEnabled(boolean enabled) {
        }

        @Override
        public void setCompassEnabled(boolean enabled) {
        }
    }

    private static final class OsmMarkerOptions implements IMarkerOptions {
        private LatLng position = new LatLng(0, 0);
        private Bitmap iconBitmap;
        private int iconResId;
        private float anchorU = 0.5f;
        private float anchorV = 1.0f;
        private String title;
        private String snippet;

        @Override
        public IMarkerOptions position(LatLng latLng) {
            position = latLng;
            return this;
        }

        @Override
        public IMarkerOptions icon(Bitmap bitmap) {
            iconBitmap = bitmap;
            iconResId = 0;
            return this;
        }

        @Override
        public IMarkerOptions icon(int resId) {
            iconBitmap = null;
            iconResId = resId;
            return this;
        }

        @Override
        public IMarkerOptions anchor(float lat, float lng) {
            anchorU = lat;
            anchorV = lng;
            return this;
        }

        @Override
        public IMarkerOptions title(String title) {
            this.title = title;
            return this;
        }

        @Override
        public IMarkerOptions snippet(String snippet) {
            this.snippet = snippet;
            return this;
        }

        @Override
        public IMarkerOptions flat(boolean flat) {
            return this;
        }
    }

    private static final class OsmCircleOptions implements ICircleOptions {
        private LatLng center = new LatLng(0, 0);
        private double radius;
        private int strokeColor;
        private int fillColor;
        private int strokeWidth = 1;

        @Override
        public ICircleOptions center(LatLng latLng) {
            center = latLng;
            return this;
        }

        @Override
        public ICircleOptions radius(double radius) {
            this.radius = radius;
            return this;
        }

        @Override
        public ICircleOptions strokeColor(int color) {
            strokeColor = color;
            return this;
        }

        @Override
        public ICircleOptions fillColor(int color) {
            fillColor = color;
            return this;
        }

        @Override
        public ICircleOptions strokePattern(List<PatternItem> patternItems) {
            return this;
        }

        @Override
        public ICircleOptions strokeWidth(int width) {
            strokeWidth = width;
            return this;
        }
    }

    private static final class OsmLatLngBoundsBuilder implements ILatLngBoundsBuilder {
        private final List<LatLng> points = new ArrayList<>();

        @Override
        public ILatLngBoundsBuilder include(LatLng latLng) {
            points.add(latLng);
            return this;
        }

        @Override
        public ILatLngBounds build() {
            double minLat = Double.MAX_VALUE;
            double maxLat = -Double.MAX_VALUE;
            double minLon = Double.MAX_VALUE;
            double maxLon = -Double.MAX_VALUE;
            for (LatLng point : points) {
                minLat = Math.min(minLat, point.latitude);
                maxLat = Math.max(maxLat, point.latitude);
                minLon = Math.min(minLon, point.longitude);
                maxLon = Math.max(maxLon, point.longitude);
            }
            if (points.isEmpty()) {
                minLat = maxLat = minLon = maxLon = 0;
            }
            return new OsmLatLngBounds(minLat, maxLat, minLon, maxLon);
        }
    }

    private static final class OsmLatLngBounds implements ILatLngBounds {
        private final BoundingBox boundingBox;

        private OsmLatLngBounds(double minLat, double maxLat, double minLon, double maxLon) {
            this.boundingBox = new BoundingBox(maxLat, maxLon, minLat, minLon);
        }

        @Override
        public LatLng getCenter() {
            GeoPoint center = boundingBox.getCenterWithDateLine();
            return new LatLng(center.getLatitude(), center.getLongitude());
        }
    }

    private static final class OsmCameraUpdate implements ICameraUpdate {
        private final LatLng target;
        private final float zoom;
        private final OsmLatLngBounds bounds;
        private final int padding;

        private OsmCameraUpdate(LatLng target, float zoom, OsmLatLngBounds bounds, int padding) {
            this.target = target;
            this.zoom = zoom;
            this.bounds = bounds;
            this.padding = padding;
        }
    }
}
