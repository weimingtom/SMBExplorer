package com.sentaroh.android.SMBExplorer;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ProfileListAdapter extends ArrayAdapter<ProfileListItem> {

	private Context c;
	private int id;
	private List<ProfileListItem>items;
		
	public ProfileListAdapter(Context context, int textViewResourceId,
			List<ProfileListItem> objects) {
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items = objects;
	}
	public ProfileListItem getItem(int i) {
		 return items.get(i);
	}
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder = null;
        if (v == null) {
           	LayoutInflater vi=(LayoutInflater)
            			c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
           	v = vi.inflate(id, null);
           	
            holder=new ViewHolder();
            holder.tv_name=(TextView) v.findViewById(R.id.profile_list_name);
            holder.tv_active=(TextView) v.findViewById(R.id.profile_list_active);
            holder.iv_image1=(ImageView) v.findViewById(R.id.profile_list_image1);
            holder.cb_cb1=(CheckBox) v.findViewById(R.id.profile_list_cb1);
            v.setTag(holder);
        } else {
      	   holder= (ViewHolder)v.getTag();
        }
        final ProfileListItem o = items.get(position);
        
        if (o != null) {
              if(holder.iv_image1!=null) 
            	  if (o.getType().equals("L")) 
            		  holder.iv_image1.setImageResource(R.drawable.ic_32_mobile);
            	  	else holder.iv_image1.setImageResource(R.drawable.ic_32_server);
              if(holder.tv_name!=null)
            	  holder.tv_name.setText(o.getName());
              if(holder.tv_active!=null)
            	  holder.tv_active.setText(o.getActive());
              if (o.getName().equals("Local")) {
            	  holder.cb_cb1.setEnabled(false);
              } else {
            	  holder.cb_cb1.setEnabled(true);
              }
               
        }
        final int p = position;
		// 必ずsetChecked前にリスナを登録(convertView != null の場合は既に別行用のリスナが登録されている！)
        holder.cb_cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
				items.set(p, //"L", "Local", "A", "", "", "", "",false)
						new ProfileListItem(o.getType(), o.getName(),
								o.getActive(),o.getUser(), o.getPass(),
								o.getAddr(),o.getShare(),
								isChecked));
				}
			});
        holder.cb_cb1.setChecked(items.get(position).isChk());
       
       return v;
	}
	static class ViewHolder {
		 TextView tv_name, tv_active;
		 ImageView iv_image1;
		 CheckBox cb_cb1;
	}
}

