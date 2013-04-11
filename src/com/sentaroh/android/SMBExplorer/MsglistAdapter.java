package com.sentaroh.android.SMBExplorer;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MsglistAdapter extends ArrayAdapter<MsglistItem> {

	private Context c;
	private int id;
	private List<MsglistItem>items;
	private boolean msgDataChanged=false;
	@SuppressWarnings("unused")
	private int wsz_w, wsz_h;
	private Activity activity;
	
	public MsglistAdapter(Activity act,Context context, int textViewResourceId,
			List<MsglistItem> objects) {
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items = objects;
		activity=act;
	}

	public void remove(int i) {
		items.remove(i);
		msgDataChanged=true;
	}
	
	@Override
	public void add(MsglistItem mli) {
		items.add(mli);
		msgDataChanged=true;
		notifyDataSetChanged();
	}
	
	public boolean resetDataChanged() {
		boolean tmp=msgDataChanged;
		msgDataChanged=false;
		return tmp;
	};
	
	public List<MsglistItem> getAllItem() {return items;}
	
	public void setAllItem(List<MsglistItem> p) {
		items.clear();
		if (p!=null) items.addAll(p);
		notifyDataSetChanged();
	}

	
//	@Override
//	public boolean isEnabled(int idx) {
//		 return getItem(idx).getActive().equals("A");
//	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
//            holder.tv_row_cat= (TextView) v.findViewById(R.id.msg_list_view_item_cat);
            holder.tv_row_msg= (TextView) v.findViewById(R.id.msg_list_view_item_msg);
            holder.tv_row_time= (TextView) v.findViewById(R.id.msg_list_view_item_time);
            holder.config=v.getResources().getConfiguration();
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        MsglistItem o = getItem(position);
        if (o != null) {
       		wsz_w=activity.getWindow()
    					.getWindowManager().getDefaultDisplay().getWidth();
   			wsz_h=activity.getWindow()
    					.getWindowManager().getDefaultDisplay().getHeight();
    		
    		if (wsz_w>=700) 
        		holder.tv_row_time.setVisibility(TextView.VISIBLE);
        	else holder.tv_row_time.setVisibility(TextView.GONE);

        	if (o.getCat().equals("W")) {
        		holder.tv_row_time.setTextColor(Color.YELLOW);
        		holder.tv_row_msg.setTextColor(Color.YELLOW);
            	holder.tv_row_time.setText(o.getMtime());
            	holder.tv_row_msg.setText(o.getMsg());
        	} else if (o.getCat().equals("E")) {
        		holder.tv_row_time.setTextColor(Color.RED);
        		holder.tv_row_msg.setTextColor(Color.RED);
        		holder.tv_row_time.setText(o.getMtime());
            	holder.tv_row_msg.setText(o.getMsg());
        	} else {
        		holder.tv_row_time.setTextColor(Color.WHITE);
        		holder.tv_row_msg.setTextColor(Color.WHITE);
        		holder.tv_row_time.setText(o.getMtime());
            	holder.tv_row_msg.setText(o.getMsg());
        	}
       	}
        return v;
	};
	
	static class ViewHolder {
//		TextView tv_row_cat;
		TextView tv_row_time;
		TextView tv_row_msg;
		Configuration config;
	}
}

