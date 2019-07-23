package com.snowplowanalytics.snowplowtrackerdemo.uitest;

import android.content.Intent;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.snowplowanalytics.snowplowtrackerdemo.Demo;
import com.snowplowanalytics.snowplowtrackerdemo.R;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.snowplowanalytics.snowplowtrackerdemo.BuildConfig.MICRO_ENDPOINT;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UITest {

    private String micro_uri;
    private String micro_all_url;
    private String micro_good_url;
    private String micro_bad_url;
    private String micro_reset_url;

    private OkHttpClient client;
    private Request resetRequest;

    @Rule
    public ActivityTestRule<Demo> activityRule
            = new ActivityTestRule<>(Demo.class);

    @Before
    public void beforeAll() {

        // unlock screen
        final Demo activity = activityRule.getActivity();
        Runnable wakeUpDevice = new Runnable() {
            public void run() {
                activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        };
        activity.runOnUiThread(wakeUpDevice);

        // set micro endpoints
        micro_uri = MICRO_ENDPOINT;
        micro_all_url = "https://".concat(MICRO_ENDPOINT.concat("/micro/all"));
        micro_good_url = "https://".concat(MICRO_ENDPOINT.concat("/micro/good"));
        micro_bad_url = "https://".concat(MICRO_ENDPOINT.concat("/micro/bad"));
        micro_reset_url = "https://".concat(MICRO_ENDPOINT.concat("/micro/reset"));

        // init okhttp client
        client = new OkHttpClient();

        resetRequest = new Request.Builder()
                .url(micro_reset_url)
                .build();
    }

    @Test
    public void sendDemoEvents() throws InterruptedException, IOException, JSONException {

        client.newCall(resetRequest).execute();

        Espresso.closeSoftKeyboard();
        onView(withId(R.id.emitter_uri_field)).check(matches(withHint("Enter endpoint here…")));
        onView(withId(R.id.emitter_uri_field)).perform(replaceText(micro_uri), closeSoftKeyboard());
        onView(withId(R.id.emitter_uri_field)).check(matches(withText(micro_uri)));
        onView(withId(R.id.btn_lite_start)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_lite_start)).perform(click());

        // TODO this is not best practice for idling resources
        Thread.sleep(20000);

        Request request = new Request.Builder()
                .url(micro_all_url)
                .build();

        Response response = client.newCall(request).execute();
        String body = response.body().string();
        JSONObject jsonObject = new JSONObject(body);
        assertEquals(jsonObject.getString("total"), "16");
        assertEquals(jsonObject.getString("good"), "16");
        assertEquals(jsonObject.getString("bad"), "0");
    }
}
