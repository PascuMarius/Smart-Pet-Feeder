package com.example.myapiapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// constants
private const val  CREDENTIALS = "elastic:AWbtmGda2Q7BI2bYpdjyF4qd"
private const val URL = "https://8f9677360fc34e2eb943d737b2597c7b.us-east-1.aws.found.io:9243/marius.index/"
private var SHARED_PREF_NAME: String = "my_app"
private var SHARED_PREF_TIME_KEY: String = "time"
private var SHARED_PREF_FOODWEIGHT_KEY: String = "foodweight"
private var SHARED_PREF_WATERLEVEL_KEY: String = "waterlevel"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textTime = findViewById<TextView>(R.id.textTime)
        val textWeight = findViewById<TextView>(R.id.textWeight)
        val textWater = findViewById<TextView>(R.id.textWater)

        val savedTime = getStringSharedPreference(this, SHARED_PREF_TIME_KEY)
        val savedFood = getStringSharedPreference(this, SHARED_PREF_FOODWEIGHT_KEY)
        val savedWater = getStringSharedPreference(this, SHARED_PREF_WATERLEVEL_KEY)
        updateView(textTime,textWeight,textWater, savedTime, savedFood, savedWater)

        getStatus(this, callback = { time, food, water ->
            updateView(textTime,textWeight,textWater, time, food, water)
            saveData(this, time, food, water)
            Toast.makeText(this, "Status Updated", Toast.LENGTH_SHORT).show()
        })

        val btnFeed = findViewById<Button>(R.id.btnFeed)
        // click listener
        btnFeed.setOnClickListener {
            postRequest(this)
        }

        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        btnSettings.setOnClickListener{
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateView(textTime: TextView,textWeight: TextView,textWater: TextView, time: String, food: String, water: String) {
        if(time == "" && food == "" && water == ""){
            textTime.text = "Status: Loading.."
            textWeight.text = "Loading.."
            textWater.text = "Loading.."
        }else{
            textTime.text = "Status: " + time
            textWeight.text = food +" grams"
            textWater.text = water
        }
    }
}

private fun postRequest(context: Context) {
    val customUrl = URL + "_doc"
    val currentTime: Date = Calendar.getInstance().time
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    sdf.format(currentTime)
    val params: String = "{" + "\"timestamp\":\"" + sdf.format(currentTime).toString() + "\"," + "\"event\":" + "\"start\"" + "," +
                               "\"repeat\":" + "\"Once\"" + "," + "\"setFoodWeight\":" + "10" + "," + "\"setWaterLevel\":" + "\"Low\"" + "}"
    val jsonObject = JSONObject(params)
    val postRequest = CustomJsonObjectRequestBasicAuth(Request.Method.POST, customUrl, jsonObject,
            Response.Listener { response ->
                try {
                    // Parse the json object here
                    val postInfo = JSONObject(response.toString())
                    val status: String = postInfo.getString("result")
                    Toast.makeText(context, "Event $status", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Parse exception : $e", Toast.LENGTH_SHORT).show()
                }
            }, Response.ErrorListener {
        Toast.makeText(context, "Volley error: $it}", Toast.LENGTH_SHORT).show()
    }, CREDENTIALS
    )
    // Add the volley request to request queue
    VolleySingleton.getInstance(context).addToRequestQueue(postRequest)
}

private fun getStatus(context: Context, callback: (time: String, food: String, water: String) -> Unit) {
    var valueFoodWeight = ""
    var valueWaterLevel = ""
    var valueTime = ""
    var valueDate = ""
    val customUrl = URL + "_search"
    val paramsGet: String = "{\"query\":" + "{" + "\"match\":" + "{" + "\"event\":" + "\"done\"" + "}" + "}," +
                            "\"size\":" + "1," + "\"sort\":" + "[{" + "\"timestamp\":" + "{" + "\"order\":" + "\"desc\"" + "}" + "}" + "]" + "}"
    val jsonObjectGet = JSONObject(paramsGet)
    val getRequest = CustomJsonObjectRequestBasicAuth(Request.Method.POST, customUrl, jsonObjectGet,
            Response.Listener { response ->
                try {
                    // Parse the json object here
                    val getFirst = response.getJSONObject("hits")
                    val getSecond = getFirst.getJSONArray("hits")
                    val getArray = getSecond.getJSONObject(0)
                    val getSource = getArray.getJSONObject("_source")
                    val timestamp = getSource.getString("timestamp")
                    valueTime = timestamp.substring(11, 16)
                    valueDate = timestamp.substring(0, 10)
                    val time = valueDate + " at " + valueTime
                    valueFoodWeight = getSource.getString("food_weight[g]")
                    valueWaterLevel = getSource.getString("water_level")
                    callback(time, valueFoodWeight, valueWaterLevel)
                    //Toast.makeText(context, "Timestamp: $valueTime" + "\nEvent: $valueEvent" + "\nFoodWeight[g]:" + valueFoodWeight + "\nWaterLevel:" + valueWaterLevel, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    //Toast.makeText(context, "Parse exception : $e", Toast.LENGTH_SHORT).show()
                }
            }, Response.ErrorListener {
        //Toast.makeText(context, "Volley error: $it}", Toast.LENGTH_SHORT).show()
    }, CREDENTIALS
    )
    // Add the volley request to request queue
    VolleySingleton.getInstance(context).addToRequestQueue(getRequest)
}

private fun saveData(context: Context, valTime: String, valFoodWeight : String , valWaterLevel : String) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
    val editor: SharedPreferences.Editor = sharedPreferences.edit()
    editor.putString(SHARED_PREF_TIME_KEY, valTime)   //timestamp
    editor.putString(SHARED_PREF_FOODWEIGHT_KEY, valFoodWeight)   //foodweight
    editor.putString(SHARED_PREF_WATERLEVEL_KEY, valWaterLevel)  // waterlevel
    editor.apply()
}

private fun getStringSharedPreference(context: Context, key: String): String {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, AppCompatActivity.MODE_PRIVATE)
    var pref = sharedPreferences.getString(key, "")
    if (pref == null) {
        pref = ""
    }
    return pref
}




