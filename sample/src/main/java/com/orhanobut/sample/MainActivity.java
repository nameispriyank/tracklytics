package com.orhanobut.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.orhanobut.tracklytics.Event;
import com.orhanobut.tracklytics.TrackEvent;
import com.orhanobut.tracklytics.TrackingAdapter;
import com.orhanobut.tracklytics.Tracklytics;

import java.util.Map;

public class MainActivity extends Activity {

  @Override @TrackEvent("Event Java")
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Tracklytics.init(new TrackingAdapter() {
      @Override public void trackEvent(Event event, Map<String, Object> superAttributes) {
        Log.d("Tracker", event.eventName);
      }
    });

    new Foo().trackFoo();
  }

  @Override protected void onResume() {
    super.onResume();

    new FooKotlin().trackFoo();
  }
}
