package com.wordpress.httpspandareaktor.scrapetest;

import java.util.HashSet;

/**
 * Created by brian on 4/23/17.
 */

public interface HunterSeeker {

    void onFinishPage(HashSet<String> set, String html);
        //called and received when a WebView page is finished and processHTML is called

    void onFinishPull(boolean finished);

}
