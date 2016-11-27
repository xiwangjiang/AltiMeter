package pl.grzegorziwanek.altimeter.app;

import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

/**
 *Inner class responsible for background update, have to extend AsyncTask<params, progress, result>
 *ASyncTask <params, progress, result> -> params: given entry data to work on; progress: data to show progress; result: result of background execution
 */

public class FetchDataInfoTask extends AsyncTask<Void, Void, Void>
{
    //TODO move it somewhere else, dummy data to get adress of a place
    //TODO -> query parameter append to URI can take many locations, but as one
    //TODO -> method to parse many locations into one String
    private final String LOG_TAG = FetchDataInfoTask.class.getSimpleName();
    private final int ZIP_CODE = 94043;
    final String APPID_KEY = "AIzaSyDz8OSO03MnSdoE-0FFN9sZaIyFRlpf79Y"; // TODO move that to config
    private final double LONGITUDE = 51.797867; //dummy data
    private final double LATITUDE = 22.232552; //dummy data
    private final String LOCATIONS = Double.toString(LONGITUDE) + "," + Double.toString(LATITUDE)
            + "|" + Double.toString(54.797867) + "," + Double.toString(13.321321)
            + "|" + Double.toString(23.432432) + "," + Double.toString(15.554545)
            + "|" + Double.toString(65.745644) + "," + Double.toString(55.555555)
            + "|" + Double.toString(12.664644) + "," + Double.toString(33.235533)
            + "|" + Double.toString(11.878777) + "," + Double.toString(44.444444)
            + "|" + Double.toString(56.641212) + "," + Double.toString(76.435355);//dummy data String

    //download data from web as a background task
    @Override
    protected Void doInBackground(Void... voids)
    {
        //help class to connect to web and get data
        HttpURLConnection urlConnection = null;
        //reads data from given input stream (this case-> data from web to string format)
        BufferedReader bufferedReader = null;
        String altitudeJsonStr = null;

        try
        {
            //setp 1: construction of the URL query for google maps API, have to add personal API key to use gooogle maps API
            //TODO move it to a different subclass or abstract class
            //google maps API takes form: https://maps.googleapis.com/maps/api/elevation/outputFormat?parameters
            //example: https://maps.googleapis.com/maps/api/elevation/json?locations=39.7391536,-104.9847034&key=AIzaSyDz8OSO03MnSdoE-0FFN9sZaIyFRlpf79Y
            final int URL_LENGTH_LIMIT = 8192;
            final String GOOGLEMAPS_BASE_URL = "https://maps.googleapis.com/maps/api/elevation/json?";
            final String OUTPUT_FORMAT = "json";
            final String PARAMETERS_LOCATIONS = "locations";
            final String PARAMETERS_PATH = "path";
            final String APPID_PARAM = "key";

            //important: use android.net URI class, not JAVA!!!
            //parse base url -> build instance of builder -> append query parameters -> build url
            Uri buildUri = Uri.parse(GOOGLEMAPS_BASE_URL).buildUpon()
                    .appendQueryParameter(PARAMETERS_LOCATIONS, LOCATIONS)
                    .appendQueryParameter(APPID_PARAM, APPID_KEY)
                    .build();

            //check how generated uri looks like
            Log.v(LOG_TAG, "URI has been built: " + buildUri.toString());

            //build string url
            URL url = new URL(buildUri.toString());

            //step 2: creation of request to google maps API, opening connection with web
            //incompatible type if without (HttpURLConnection call)
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //step 3: read input stream from web by getting stream from opened connection
            // get input stream from url connection -> create StringBuffer instance ->
            // define BufferedReader here with InputStreamReader -> append strings to the StringBuffer in a loop ->
            // define string to show as StringBuffer.toString();
            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null)
            {
                //set as null if no data to show
                Log.v(LOG_TAG, "Input stream was empty, no data to shown, return null");
                return null;
            }

            StringBuffer stringBuffer = new StringBuffer();

            //define BufferedReader by got input stream
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            //have to use additional string (line) to call readLine() just once ( so lines from stream won't be losed and empty)
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                //append "line" instead calling readLine() again
                stringBuffer.append(line + "\n");
            }

            if (stringBuffer.length() == 0)
            {
                //string stream was empty, so no point in further parsing-> return null so no data is shown
                Log.v(LOG_TAG, "String stream was empty, no data to shown, return null");
                return null;
            }

            //define altitude string
            altitudeJsonStr = stringBuffer.toString();

            //log message with result string
            Log.v(LOG_TAG, "altitude string generated: " + altitudeJsonStr);
        }
        catch (IOException e)
        {
            //if error occur, code didn't get data from web so no point in performing further data parsing, return null
            Log.v(LOG_TAG, "Error, at getting data from web:", e);
            return null;
        }
        finally
        {
            //close opened url connection
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
            //try to close opened buffered reader
            if (bufferedReader != null)
            {
                try
                {
                    bufferedReader.close();
                }
                catch (IOException e)
                {
                    Log.v(LOG_TAG, "Error, at BufferedReaded closing:", e);
                    e.printStackTrace();
                }
            }
        }

        try
        {
            //TODO change num of points from fixed to method generated
            getAltitudeDataFromJson(altitudeJsonStr, 7);
        } catch (JSONException e)
        {
            Log.v(LOG_TAG, "Error occur, getting data from defined JSON string: ", e);
            e.printStackTrace();
        }



        return null;
    }

    //method to extract data in correct form from given Json String;
    // TODO-> methods to define number of points, right now fixed dummy
    public String[] getAltitudeDataFromJson(String altitudeJsonStr, int numOfPoints) throws JSONException
    {

        //JSON objects names which need to be extracted from given string
        final String OMW_RESULTS = "results";
        final String OMW_ELEVATION = "elevation";
        final String OMW_LOCATION = "location";
        final String OMW_LATITUDE = "lat";
        final String OMW_LONGITUDE = "lng";
        final String OMW_RESOLUTION = "resolution";
        final String OMW_STATUS = "status";

        //create Json object and array with data, assign given Json string parameter to object
        JSONObject altitudeJson = new JSONObject(altitudeJsonStr);
        JSONArray altitudeJsonArray = altitudeJson.getJSONArray(OMW_RESULTS);
        String[] resultArray = new String[numOfPoints];

        //hatch data from Json array into result String array
        for (int i=0; i<altitudeJsonArray.length(); i++)
        {
            //get JSONObject representing the single point on a map
            JSONObject pointData = altitudeJsonArray.getJSONObject(i);

            //there is only one main array (RESULTS) and all of points are inside that array;
            //each cell consist elevation, location which is subarray and consist lan and lng, and resolution

            //elevation extraction
            Double pointDataElevation = pointData.getDouble(OMW_ELEVATION);
//            mCurrentEleValue = pointDataElevation;

            //location extraction and assignation lat and lng
            JSONObject pointDataLocation = pointData.getJSONObject(OMW_LOCATION);
            Double pointDataLatitude = pointDataLocation.getDouble(OMW_LATITUDE);
            Double pointDataLongitude = pointDataLocation.getDouble(OMW_LONGITUDE);
            //mCurrentLatValue = pointDataLatitude;
            //mCurrentLngValue = pointDataLongitude;

            //resolution extraction
            Double pointDataResolution = pointData.getDouble(OMW_RESOLUTION);

            resultArray[i] = "Elevation: " + pointDataElevation.toString() + ", " + pointDataLatitude.toString() + ", " + pointDataLongitude.toString();

            //TODO move from here to separated method/class
            DecimalFormat df = new DecimalFormat("#.###");
            String currentElevation = df.format(pointDataElevation);

//            //min and max elevation
//            if (pointDataElevation <= mMinElevValue)
//            {
//                mMinElevValue = pointDataElevation;
//            }
//            else if (pointDataElevation >= mMaxElevValue)
//            {
//                mMaxElevValue = pointDataElevation;
//            }

            //mCurrElevationTextView.setText(currentElevation);
            //TODO method to convert Double to format of lng and lat with degrees/minutes/seconds
            //mCurrLongitudeTextView.setText(pointDataLongitude.toString());
            //mCurrLatitudeTextView.setText(pointDataLatitude.toString());
        }

        for (String pointEntry : resultArray)
        {
            Log.v(LOG_TAG, "Point entry created: " + pointEntry);
        }

        return resultArray;
    }

    public String convertLocationFormat(Double lat, Double lng)
    {
        //mCurrLongitudeTextView.setText(Location.convert(lng, Location.FORMAT_SECONDS));
        //mCurrLatitudeTextView.setText(Location.convert(lat, Location.FORMAT_SECONDS));
        String lngText = replaceDelimiters(lng, false);
        String latText = replaceDelimiters(lat, true);
//        mCurrLatitudeTextView.setText(latText);
//        mCurrLongitudeTextView.setText(lngText);
        return null;
    }

    //consist code responsible for replacing format (23:32:32:3223132 -> 23°xx'xx")
    public String replaceDelimiters(Double coordinate, boolean isLat)
    {
        //replace ":" with symbols
        String str = Location.convert(coordinate, Location.FORMAT_SECONDS);
        str = str.replaceFirst("-", "");
        str = str.replaceFirst(":", "°");
        str = str.replaceFirst(":", "'");

        //get index of point, define end index of the given string
        int pointIndex = str.indexOf(".");
        int endIndex = pointIndex;

        //subtract string if is longer than end index
        if (endIndex < str.length())
        {
            str = str.substring(0, endIndex);
        }

        //add "''" at the end
        str = str + "\"";

        //define direction (N, W, E, S)
        str = defineDirection(str, coordinate, isLat);

        return str;
    }

    public String defineDirection(String str, Double coordinate, boolean isLatitude)
    {
        //if is latitude -> add S/N
        if (isLatitude == true)
        {
            if (coordinate < 0)
            {
                str = str + "S";
            }
            else
            {
                str = str + "N";
            }
        }
        //if is longitude -> add E/W
        else
        {
            if (coordinate >0 )
            {
                str = str + "E";
            }
            else
            {
                str = str + "W";
            }
        }

        return str;
    }

    @Override
    protected void onPostExecute(Void aVoid)
    {
//        DecimalFormat df = new DecimalFormat("#.##");
//        String currentElevation = df.format(mCurrentEleValue);
//        mCurrElevationTextView.setText(currentElevation);
//        mMaxElevTextView.setText(df.format(mMaxElevValue));
//        mMinElevTextView.setText(df.format(mMinElevValue));
//        convertLocationFormat(mCurrentLatValue, mCurrentLngValue);
    }
}