package com.wordpress.httpspandareaktor.scrapetest;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    //this is the webview browser
    private WebView browser;

    //arraylists to store visited and unvisited urls
    private HashSet<String> visitedLinks = new HashSet<>();
    private HashSet<String> collectedLinks = new HashSet<>();
    private HashSet<String> masterEmailSet = new HashSet<>();

    //bucket string for holding the full html
    String bucket = "";

    //the search term if it exists
    private String mSearchTerm;

    //first link visited, and the last HTML result received by WebView
    private String firstLinkAsString = "";
    private String lastResult;


    //this textview shows emails found, start as view.gone
    TextView emailDisplay;
    byte emailsFound = 0;

    //this EditText is where the user's URL input goes, queried URL is another store (created URL) of the input
    EditText inputURL;
    URL queriedURL;

    //the progress bar that starts invisible but is revealed after search begins, and the search term if it exists
    LinearLayout progressBar;
    EditText searchTermField;
    String searchTerm;

    //the top section contains the search term and URL edit text boxes
    LinearLayout topSection;

    //text below progress bar
    TextView progressText;

    //is the WebView already crawling?
    boolean crawlComplete;

    //page loaded
    boolean currentPageFinished = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchTermField = (EditText) findViewById(R.id.searchTermField);

        inputURL = (EditText) findViewById(R.id.inputURL);

        progressBar = (LinearLayout) findViewById(R.id.progressBar);
        topSection = (LinearLayout) findViewById(R.id.topSection);

        progressText = (TextView) findViewById(R.id.progressText);
        emailDisplay = (TextView) findViewById(R.id.emailDisplay);

        //not current crawling:
        crawlComplete = false;

        browser = (WebView) findViewById(R.id.browser);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.addJavascriptInterface(new MyJavaScriptInterface(this), "HtmlOut");

        browser.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //call processHTML from custom HtmlOut JS interface onPageFinished
                browser.loadUrl("javascript:HtmlOut.processHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");

            }
        });

    }

    private class MyJavaScriptInterface {
        //This JS interface is called from WebView after onFinish

        private Context ctx;

        MyJavaScriptInterface(Context ctx) {
            //this interface will allow javascript to communicate with Android
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void processHTML(String html) {
            //called when browser finished loading a page

            //below is an alert dialog to show the html directly
//            new AlertDialog.Builder(ctx).setTitle("HTML").setMessage(html)
//                    .setPositiveButton(android.R.string.ok, null).setCancelable(false).create().show();

            //every time we call purify, we get a hashset, we want to copy that set into master
            Log.v("processHTML", " just received html: " + html);
            lastResult = html;
            HashSet<String> tempHash = RegexUtils.purify(html, searchTerm);

            if (tempHash.size() != 0) {
                for (String string : tempHash) {
                    emailsFound++;
                    masterEmailSet.add(string);
                    Log.v("masterEmailSet length", " +1 , total length: " + masterEmailSet.size());
                }
            }

            if (lastResult.equals("")) {
                //if the last result was empty, log the event
                Log.v("processHTML", " No result on the most recent page!");
            }

            pullLinks(lastResult);
            cleanCollectedLinks();


            if (masterEmailSet.size() > 20){
                //one of the "win conditions" has been met, email collection maxed
                crawlComplete = true;

            }


            if (collectedLinks.iterator().hasNext() && !crawlComplete){
                //if there's another link and crawl isn't deemed completed already, hit next URL
                browser.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.v("processHTML", " loading page on browser:" + collectedLinks.iterator().next());
                        browser.loadUrl(collectedLinks.iterator().next());
                        visitedLinks.add(collectedLinks.iterator().next());
                    }
                });
            }

            sleepMilliseconds(1000);




        }
    }

    public void extractButton(View view) {
        Log.v("extractButton", " initialized with URL field as:" + inputURL.getText().toString());
        firstLinkAsString = inputURL.getText().toString();

        if (!networkAvailable()) {
            //Error message if the network is unavailable
            Toast.makeText(this, "Network unavailable!", Toast.LENGTH_SHORT).show();
            return;
        }


        //User just typed in a URL and requested fetch
        if (!inputURL.getText().toString().equals("")) {
            //if not empty, try to build URL, makeURL shoudld catch MalformedURLException
            URL currentURL = NetworkUtils.makeURL(inputURL.getText().toString());

            if (currentURL != null) {
                Log.v("extractButton", " says URL field is acceptable");
                searchTerm = searchTermField.getText().toString();
                //if the currentlyRunning boolean says there are no current tasks going, make a new one and reference it

                //set up the UI while the user waits, show the WebView as well
                browser.setVisibility(View.VISIBLE);
                topSection.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
//                emailDisplay.setVisibility(View.VISIBLE);

                //hit URL for an initial pull
                hitURL(currentURL.toString());




            } else {
                Toast.makeText(this, "Bad URL! Try again", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Cannot extract from an empty URL!", Toast.LENGTH_SHORT).show();
        }
    }

    private void hitURL(String URL){
        // Simplest usage: note that an exception will NOT be thrown
        // if there is an error loading this page (see below).

        //add the lastest url to visited URLs arraylist, increment links hit counter, add to bucket
        visitedLinks.add(URL);
        browser.loadUrl(URL);

        Log.v("hitURL", " -- reached end of the method");

        // OR, you can also load from an HTML string:
//        String summary = "<html><body>You scored <b>192</b> points.</body></html>";
//        webview.loadData(summary, "text/html", null);
        // ... although note that there are restrictions on what this HTML can do.
        // See the JavaDocs for loadData() and loadDataWithBaseURL() for more info.
    }

    private void pullLinks(String htmlPage) {
        //this method pulls links from a page, if they haven't been visited, add into unvisited ArrayList<URL>

        Document doc = Jsoup.parse(htmlPage);
        Elements links = doc.select("a[href]");

        for (Element link : links) {

            String possibleUrl = link.attr("abs:href");

            if (!possibleUrl.equals("")) {
                Log.v("pullLinks", " will try to make URL from" + possibleUrl);
                //if the link attr isn't empty, make a URL
                URL theUrl = NetworkUtils.makeURL(possibleUrl);

                if (RegexUtils.urlDomainNameMatch(firstLinkAsString, theUrl.toString())) {
                    //if the url is within the same domain as original query
                    if (!visitedLinks.contains(theUrl)) {
                        Log.v("DLAsyncTask.pullLinks", " thinks that " + theUrl.toString() + " wasn't visited, add into collected...");
                        collectedLinks.add(theUrl.toString());
                    }
                }
            }

        }
    }

    private void cleanCollectedLinks() {
        //iterator to go over and clean out collectedLinks HashSet
        for (Iterator itr = visitedLinks.iterator(); itr.hasNext(); ) {
            String thisURL = (String) itr.next();
            if (urlInHashSet(NetworkUtils.makeURL(thisURL), collectedLinks)) {
                collectedLinks.remove(thisURL.toString());
                Log.v("DLasync.cleanCollected", " from CollectedLinks, just cleaned: " + thisURL);
                Log.v(".cleanCollected", " collected set is now:" + collectedLinks.toString());
            }
        }

    }

    private void sleepMilliseconds(int time) {
        //try sleeping randomly up to time milliseconds
        //prevent repeated suspicious activity from server
        int multipliedParam = (int) (Math.random() * time + 1);

        try {
            TimeUnit.MILLISECONDS.sleep(multipliedParam);
            Log.v("DLASync.sleep", " sleep " + multipliedParam + " milliseconds...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean urlInHashSet(URL url, HashSet<String> set){
        //checks if the URL is in a provided HashSet with an improved for loop
        boolean returnBoolean = false;

        for (String setItem : set){
            if (NetworkUtils.urlHostPathMatch(NetworkUtils.makeURL(setItem), url)) {
                Log.v("DLAsync.urlInHashSet", " just found " + url.toString() + " in " + set.toString());
                returnBoolean = true;
            }
        }
        return returnBoolean;
    }

    private void displayMasterEmails(){
        String masterEmails = "";
        for (String email : masterEmailSet){
            masterEmails.concat(email + "\n");
        }
        emailDisplay.setText(masterEmails);
    }

    public void killTask(View view) {
        //user wants to kill the AsyncTask
        Log.v("MainActivity.killTask", " triggered");
        if (!crawlComplete) {
            crawlComplete = true;
            setPostCrawlUI();
        }
    }

    public void setPostCrawlUI(){
        progressBar.setVisibility(View.GONE);
        browser.setVisibility(View.GONE);
        topSection.setVisibility(View.VISIBLE);
        displayMasterEmails();
    }

    public boolean networkAvailable() {
        //returns boolean to determine whether network is available, requires ACCESS_NETWORK_STATE permission
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if network is off, networkInfo will be null
        //otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

}
