package com.wordpress.httpspandareaktor.scrapetest;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by brian on 3/23/17.
 */

public class NetworkUtils {



    public static URL makeURL(String string) {

        //remove the trailing slash if there is one, for uniformity's sake
        if (string.endsWith("/")){
            string = string.substring(0, string.length() - 1);
        }

        if (!string.startsWith("http")){
            //if the user forgot to put in the protocol, guess http and put it in
            //TODO: find a way to see if the real URL is http or https and append the correct one
            string = "http://" + string;
            Log.v("NetworkUtils.makeURl", " appended (guessed) http:// protocol to make " + string);
        }

        URL returnURL = null;

        try {
            //make the URL out of the string
            returnURL = new URL(string);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.v("NetworkUtils", " I failed to make url from: " + string);
        }
        return returnURL;
    }

    public static boolean urlHostPathMatch(URL urlA, URL urlB) {
        //check if the paths match of built URL objects

        //build a url to make string for A, then B

        String protocolA = urlA.getProtocol();
        String protocolB = urlB.getProtocol();

        String hostA = urlA.getHost();
        String hostB = urlB.getHost();
        //strip the www. out of the host A/B if it exists
        if (hostA.substring(0, 4).equals("www.")){
            hostA = hostA.substring(4, hostA.length());
        }
        if (hostB.substring(0, 4).equals("www.")){
            hostB = hostB.substring(4, hostB.length());
        }

        String pathA = urlA.getPath();
        String pathB = urlB.getPath();

        String rebuiltUrlA = protocolA + hostA + pathA;

        if (rebuiltUrlA.equals(protocolB + hostB + pathB)){
            return true;
        } else {
            return false;
        }

    }



}
