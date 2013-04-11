package com.sentaroh.android.SMBExplorer;

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


public class ProfilelistAdapter extends ArrayAdapter<ProfilelistItem> {

	private Context c;
	private int id;
	private List<ProfilelistItem>items;
		
	public ProfilelistAdapter(Context context, int textViewResourceId,
			List<ProfilelistItem> objects) {
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items = objects;
	}
	public ProfilelistItem getItem(int i) {
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
        final ProfilelistItem o = items.get(position);
        
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
						new ProfilelistItem(o.getType(), o.getName(),
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

