package com.example.android.safetyalert;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ishmita on 11-07-2016.
 */
public class CustomAdapter extends BaseAdapter {

    ArrayList<Person> list = null;
    private static final String TAG = "CustomAdapter";
    int res;
    Person person1;
    Activity context;


    CustomAdapter(Activity context, int res, ArrayList<Person> list){
        this.context = context;
        this.res = res;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if(convertView == null) {

            convertView = LayoutInflater.from(context).inflate(res, parent, false);

            holder = new ViewHolder();
            holder.name = (TextView)convertView.findViewById(R.id.name_textView);
            holder.map = (ImageButton) convertView.findViewById(R.id.map_imageButton);
            holder.phone = (TextView)convertView.findViewById(R.id.phone_number);
            holder.call = (ImageButton) convertView.findViewById(R.id.call_button);
            holder.background = (LinearLayout) convertView.findViewById(R.id.linear_layout_person);
            convertView.setTag(holder);

        }else{
            holder = (ViewHolder)convertView.getTag();
        }

        person1 = list.get(position);
        Log.d(TAG, "list: " + person1.getName());
        if(person1.isGeoStatus()){
            Log.d(TAG, "isGeoStatus:name "+person1.getName());
            holder.background.setBackgroundColor(person1.getColor());
        }
        holder.name.setText(person1.getName());
        holder.phone.setText(person1.getPhone());


        holder.map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "on map click, latitude: "+list.get(position).getLatitude() + "longitude: "+ list.get(position).getLongitude());
                String uri = "geo:" + list.get(position).getLatitude() + "," + list.get(position).getLongitude();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                context.startActivity(intent);

            }
        });

        holder.call.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "in call click");
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:"+list.get(position).getPhone()));
                context.startActivity(intent);
            }
        });


        return convertView;
    }

    static class ViewHolder{
        static TextView name;
        static TextView phone;
        static ImageButton map;
        static ImageButton call;
        static LinearLayout background;
    }
}
