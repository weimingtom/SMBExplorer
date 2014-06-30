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
	@SuppressWarnings("unused")
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
        	  if (o.getType().equals("L")) 
        		  holder.iv_image1.setImageResource(R.drawable.ic_32_mobile);
        	  	else holder.iv_image1.setImageResource(R.drawable.ic_32_server);
        	  holder.tv_name.setText(o.getName());
        	  holder.tv_active.setText(o.getActive());
        	  if (o.getActive().equals("I")) {
            	  holder.tv_name.setEnabled(false);
            	  holder.tv_active.setEnabled(false);
        	  } else {
            	  holder.tv_name.setEnabled(true);
            	  holder.tv_active.setEnabled(true);
        	  }
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
				o.setChk(isChecked);
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

class ProfileListItem implements Comparable<ProfileListItem>{
	private String profileType;
	private String profileName;
	private String profileActive;
	private String profileUser;
	private String profilePass;
	private String profileAddr;
	private String profilePort="";
	private String profileShare;
	private boolean profileIschk;
	
	public ProfileListItem(String pft,String pfn, String pfa, 
			String pf_user, String pf_pass, String pf_addr, String pf_port, 
			String pf_share, boolean ic){
		profileType = pft;
		profileName = pfn;
		profileActive=pfa;
		profileUser = pf_user;
		profilePass = pf_pass;
		profileAddr = pf_addr;
		profilePort = pf_port;
		profileShare = pf_share;
		profileIschk = ic;
		
	}

	public String getType(){return profileType;}
	public String getName(){return profileName;}
	public String getActive(){return profileActive;}
	public String getUser(){return profileUser;}
	public String getPass(){return profilePass;}
	public String getAddr(){return profileAddr;}
	public String getPort(){return profilePort;}
	public String getShare(){return profileShare;}
	public void setActive(String p){profileActive=p;}
	public boolean isChk(){return profileIschk;}
	public void setChk(boolean p){profileIschk=p;}
	
	@Override
	public int compareTo(ProfileListItem o) {
		if(this.profileName != null)
			return this.profileName.toLowerCase().compareTo(o.getName().toLowerCase()) ; 
//			return this.filename.toLowerCase().compareTo(o.getName().toLowerCase()) * (-1);
		else 
			throw new IllegalArgumentException();
	}
}