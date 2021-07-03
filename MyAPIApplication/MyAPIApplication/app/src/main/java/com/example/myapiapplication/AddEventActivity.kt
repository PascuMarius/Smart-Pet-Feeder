package com.example.myapiapplication

import android.app.TimePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*

class AddEventActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener {
    private lateinit var spinWeight : Spinner
    private lateinit var spinWater : Spinner
    private lateinit var spinRepeat : Spinner
    private lateinit var pickTime : TextView
    private var hour = 0
    private var minute = 0
    private var savedHour = ""
    private var savedMinute = ""
    private var repeat = ""
    private var waterLevel = ""
    private var foodWeight = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

        spinWeight = findViewById(R.id.spinWeight)
        spinWater = findViewById(R.id.spinWater)
        spinRepeat = findViewById(R.id.spinRepeat)

        populateSpinnerWeight()
        populateSpinnerWater()
        populateSpinnerRepeat()

        spinWeight.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(adapterView?.getItemAtPosition(position).toString().equals("Food[g]")) {
                }else {
                    foodWeight = adapterView?.getItemAtPosition(position).toString()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinWater.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(adapterView?.getItemAtPosition(position).toString().equals("Level")) {
                }else{
                    waterLevel = adapterView?.getItemAtPosition(position).toString()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinRepeat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                repeat = adapterView?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        pickTime = findViewById(R.id.textPickTime)
        pickTime.setOnClickListener{
            TimePickerDialog(this,this,hour,minute,true).show()
        }

        val btnCancel = findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener{
            Toast.makeText(this@AddEventActivity, "Canceled", Toast.LENGTH_LONG).show()
            finish()
        }

        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.setOnClickListener{
            if(pickTime.text.toString() != "" && repeat != "" && foodWeight != "Food[g]" && waterLevel != "Level") {
                Toast.makeText(this@AddEventActivity, "Event created", Toast.LENGTH_LONG).show()
                addItem(pickTime.text.toString(), repeat,foodWeight, waterLevel)
                finish()
            }else
                Toast.makeText(this@AddEventActivity, "Pick all fields!", Toast.LENGTH_LONG).show()
        }
    }

    private fun populateSpinnerWeight(){
        val weightAdapter : ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(this, R.array.weight,  R.layout.spin_layout)
        weightAdapter.setDropDownViewResource( R.layout.spin_layout)
        spinWeight.adapter = weightAdapter
    }

    private fun populateSpinnerWater(){
        val waterAdapter : ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(this, R.array.water, R.layout.spin_layout)
        waterAdapter.setDropDownViewResource(R.layout.spin_layout)
        spinWater.adapter = waterAdapter
    }

    private fun populateSpinnerRepeat(){
        val repeatAdapter : ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(this, R.array.repeat, R.layout.spin_layout)
        repeatAdapter.setDropDownViewResource(R.layout.spin_layout)
        spinRepeat.adapter = repeatAdapter
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        if (hourOfDay < 10)
            savedHour = "0" + hourOfDay.toString()
        else
            savedHour = hourOfDay.toString()
        if(minute < 10)
            savedMinute = "0" + minute.toString()
        else
            savedMinute = minute.toString()

        pickTime.text = "$savedHour:$savedMinute"
    }
}