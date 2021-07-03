package com.example.myapiapplication

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(val c : Context, val eventList : ArrayList<ListItem>) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val item = inflater.inflate(R.layout.recycler_layout,parent,false)
        return EventViewHolder(item)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val newList = eventList[position]
        holder.title.text = newList.Time
        holder.subTitle.text = newList.Repeat
        holder.food.text = newList.Food
        holder.water.text = newList.Water
    }

    override fun getItemCount() = eventList.size

    inner class EventViewHolder ( item : View) : RecyclerView.ViewHolder(item){
         var title : TextView
         var subTitle : TextView
         var water : TextView
         var food : TextView
         private var btnDelete : Button

        init {
             title = item.findViewById(R.id.Title)
             subTitle = item.findViewById(R.id.subTitle)
             water = item.findViewById(R.id.waterLevel)
             food = item.findViewById(R.id.foodWeight)
             btnDelete = item.findViewById(R.id.btnRecycler)

            btnDelete.setOnClickListener{
                AlertDialog.Builder(c).setTitle("Delete")
                    .setIcon(R.drawable.ic_warning)
                    .setMessage("Are you sure delete this event?")
                    .setPositiveButton("Yes"){
                        dialog, _->
                        deleteItem(eventList.get(adapterPosition).ID)
                        eventList.removeAt(adapterPosition)
                        notifyDataSetChanged()
                       // saveList(eventList,c)
                        dialog.dismiss()
                        Toast.makeText(c, "Event removed", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No"){
                            dialog, _-> dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }
}