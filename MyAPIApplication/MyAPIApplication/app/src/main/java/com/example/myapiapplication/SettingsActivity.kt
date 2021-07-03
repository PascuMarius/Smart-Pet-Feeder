package com.example.myapiapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private lateinit var listAdapter : EventAdapter
private lateinit var itemList : ArrayList<ListItem>
private var SHARED_PREF_EVENT = "my_list"
private var SHARED_KEY_EVENT = "events"
private lateinit var context : Context
private val URL = "https://8f9677360fc34e2eb943d737b2597c7b.us-east-1.aws.found.io:9243/marius.index/"
private val CREDENTIALS = "elastic:AWbtmGda2Q7BI2bYpdjyF4qd"

class SettingsActivity : AppCompatActivity(){
   private lateinit var floatBtn : FloatingActionButton
   private lateinit var recycler : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        context = this

        //getList(this)
        getItem()

        recycler = findViewById(R.id.recyclerView)
        listAdapter = EventAdapter(this,itemList)
        recycler.layoutManager =  LinearLayoutManager(this)
        recycler.adapter = listAdapter

        floatBtn = findViewById(R.id.floatingActionButton)
        floatBtn.setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            startActivity(intent)
        }

        val backButton = findViewById<FloatingActionButton>(R.id.backButton)
        backButton.setOnClickListener{
            finish()
        }
    }
}

fun addItem(time: String, repeat: String, food: String, water: String){
    postItem(context,time,repeat,food,water, callback = { id ->
        itemList.add(ListItem(time, repeat, food +"g", water, id))
        itemList.sortWith(compareBy { it.Time })
        listAdapter.notifyDataSetChanged()
       // saveList(itemList,context)
    })
}

 /*fun saveList(list: List<ListItem>?, context : Context) {
    val prefs = context.getSharedPreferences(SHARED_PREF_EVENT, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    val gson  = Gson()
    val json = gson.toJson(list)
    editor.putString(SHARED_KEY_EVENT, json)
    editor.apply()
}

 fun getList(context : Context) {
    val prefs = context.getSharedPreferences(SHARED_PREF_EVENT, Context.MODE_PRIVATE)
    val gson = Gson()
    val json = prefs.getString(SHARED_KEY_EVENT, emptyList<ListItem>().toString())
    val type = object : TypeToken<ArrayList<ListItem?>?>() {}.type
     itemList = gson.fromJson(json, type)
     if (itemList.isEmpty())
         itemList =  ArrayList()
}*/

private fun getItem() {
    itemList =  ArrayList()
    val currentTime: Date = Calendar.getInstance().time
    val sdf = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm")
    sdf.format(currentTime)
    val customUrl = URL + "_search"
    val paramsGet: String = "{\"query\":" + "{" + "\"match\":" + "{" + "\"event\":" + "\"start\"" + "}" + "}" + "}"
    val jsonObjectGet = JSONObject(paramsGet)
    val getRequest = CustomJsonObjectRequestBasicAuth(Request.Method.POST, customUrl, jsonObjectGet,
        Response.Listener { response ->
            try {
                // Parse the json object here
                val getFirst = response.getJSONObject("hits")
                val getArray = getFirst.getJSONArray("hits")
                for (i in 0 until getArray.length()) {
                    val jsonObject: JSONObject = getArray.getJSONObject(i)
                    val id = jsonObject.getString("_id")
                    val getSource = jsonObject.getJSONObject("_source")
                    val valueTimestamp = getSource.getString("timestamp")
                    val valueTime = valueTimestamp.substring(11, 16)
                    val valueRepeat = getSource.getString("repeat")
                    val valueFoodWeight = getSource.getString("setFoodWeight")
                    val valueWaterLevel = getSource.getString("setWaterLevel")
                    itemList.add(ListItem(valueTime, valueRepeat, valueFoodWeight +"g", valueWaterLevel, id))
                    itemList.sortWith(compareBy { it.Time })
                    listAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Parse exception : $e", Toast.LENGTH_SHORT).show()
            }
        }, Response.ErrorListener {
            Toast.makeText(context, "Volley error: $it}", Toast.LENGTH_SHORT).show()
        }, CREDENTIALS
    )
    // Add the volley request to request queue
    VolleySingleton.getInstance(context).addToRequestQueue(getRequest)
}

private fun postItem(context: Context, time : String, repeat : String, food : String, water : String, callback: (id : String) -> Unit) {
    val customUrl = URL + "_doc"
    val currentTime: Date = Calendar.getInstance().time
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'")
    sdf.format(currentTime)
    val params: String = "{" + "\"timestamp\":\"" + sdf.format(currentTime).toString()+ time + ":00Z"+ "\"," +
                                "\"event\":" + "\"start\"" + "," +
                                "\"repeat\":" + "\"$repeat\"" + "," +
                                "\"setFoodWeight\":" + food + "," +
                                "\"setWaterLevel\":" + "\"$water\"" +"}"
    val jsonObject = JSONObject(params)
    val postRequest = CustomJsonObjectRequestBasicAuth(Request.Method.POST, customUrl, jsonObject,
        Response.Listener { response ->
            try {
                // Parse the json object here
                val postInfo = JSONObject(response.toString())
                val id : String = postInfo.getString("_id")
                callback(id)
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

fun deleteItem( id : String) {
    val customUrl = URL + "_delete_by_query"
    val params: String = "{" + "\"query\":" + "{" + "\"match\":" + "{" + "\"_id\":" + "$id" + "}" + "}" + "}"
    val jsonObject = JSONObject(params)
    val postRequest = CustomJsonObjectRequestBasicAuth(Request.Method.POST, customUrl, jsonObject,
        Response.Listener { response ->
            try {
                val postInfo = JSONObject(response.toString())
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


