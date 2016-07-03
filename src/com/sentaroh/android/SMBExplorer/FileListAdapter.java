package com.sentaroh.android.SMBExplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.ThemeColorList;

public class FileListAdapter extends BaseAdapter {
	private Context mContext;
	private ArrayList<FileListItem>mDataItems=null;
	private boolean mSingleSelectMode=false;
	private boolean mShowLastModified=true;
	private int[] mIconImage= new int[] {R.drawable.cc_expanded,
			R.drawable.cc_collapsed,
			R.drawable.cc_folder,
			R.drawable.cc_sheet,
			R.drawable.cc_blank};
	
	private ThemeColorList mThemeColorList;

	public FileListAdapter(Context context) {
		mContext = context;
		mDataItems=new ArrayList<FileListItem>();
		mThemeColorList=ThemeUtil.getThemeColorList(mContext);
	};

	public FileListAdapter(Context context,
			boolean singleSelectMode, boolean showLastModified) {
		mContext = context;
		this.mSingleSelectMode=singleSelectMode;
		this.mShowLastModified=showLastModified;
		mDataItems=new ArrayList<FileListItem>();
		mThemeColorList=ThemeUtil.getThemeColorList(mContext);
	};
	
	@Override
	public int getCount() {return mDataItems.size();}

	@Override
	public FileListItem getItem(int arg0) {return mDataItems.get(arg0);}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	public void setShowLastModified(boolean p) {
		mShowLastModified=p;
	};

	public void setSingleSelectMode(boolean p) {
		mSingleSelectMode=p;
	};

	public void setSelected(int pos, boolean selected) {
		if (mSingleSelectMode) setAllItemChecked(false);
		mDataItems.get(pos).setChecked(selected);
	};

	public boolean isSelected(int pos) {
		boolean result=mDataItems.get(pos).isChecked();
		return result;
	};
	
	public boolean isItemSelected() {
		boolean result=false;
		for(FileListItem fli:mDataItems) {
			if (fli.isChecked()) {
				result=true;
				break;
			}
		}
		return result;
	};
	
	public void setAllItemChecked(boolean selected) {
		for (int i=0;i<mDataItems.size();i++) 
			mDataItems.get(i).setChecked(selected); 
	};
	
	public boolean isSingleSelectMode() {
		return mSingleSelectMode;
	};

	public void removeItem(int dc) {
		mDataItems.remove(dc);
	};

	public void removeItem(FileListItem fi) {
		mDataItems.remove(fi);
	};

	public void replaceItem(int i, FileListItem fi) {
		mDataItems.set(i,fi);
		notifyDataSetChanged();
	};

	public void add(FileListItem fi) {
		mDataItems.add(fi);
	};

	public void insert(int i, FileListItem fi) {
		mDataItems.add(i,fi);
	};

	public ArrayList<FileListItem> getDataList() {
		return mDataItems;
	};

	public void setDataList(ArrayList<FileListItem> fl) {
		mDataItems=fl;
	};

	public void clear() {
		mDataItems.clear();
	};
	
	public void sort() {
		Collections.sort(mDataItems, new Comparator<FileListItem>(){
			@Override
			public int compare(FileListItem l, FileListItem r) {
				String l_d=l.isDir()?"0":"1"; 
				String r_d=r.isDir()?"0":"1";
				return (l_d+l.getName()).compareToIgnoreCase((r_d+r.getName()));
			}
		});
	};
	
	private NotifyEvent cb_ntfy=null;
	public void setCbCheckListener(NotifyEvent ntfy) {
		cb_ntfy=ntfy;
	}

	public void unsetCbCheckListener() {
		cb_ntfy=null;
	}

	private boolean enableListener=true;
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		 	final ViewHolder holder;
		 	
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                v = vi.inflate(R.layout.file_list_item, null);
                v = vi.inflate(R.layout.file_list_item, parent, false);
                holder=new ViewHolder();
                
            	holder.cb_cb1=(CheckBox)v.findViewById(R.id.file_list_checkbox);
            	holder.rb_rb1=(RadioButton)v.findViewById(R.id.file_list_radiobtn);
            	holder.iv_image1=(ImageView)v.findViewById(R.id.file_list_icon);
            	holder.tv_name=(TextView)v.findViewById(R.id.file_list_name);
            	holder.tv_size=(TextView)v.findViewById(R.id.file_list_size);
            	holder.tv_moddate=(TextView)v.findViewById(R.id.file_list_date);
            	holder.tv_modtime=(TextView)v.findViewById(R.id.file_list_time);
            	holder.tv_select=(LinearLayout)v.findViewById(R.id.file_list_select_view);
            	
            	v.setTag(holder); 
            } else {
         	   holder= (ViewHolder)v.getTag();
            }
            v.setEnabled(true);
            final FileListItem o = mDataItems.get(position);
//            Log.v("","data_items pos="+show_items.get(position)+", pos="+position);
            if (o != null) {
            	if (o.isEnableItem()) {
	            	holder.cb_cb1.setEnabled(true);
            		holder.rb_rb1.setEnabled(true);
	            	holder.iv_image1.setEnabled(true);
	            	holder.tv_name.setEnabled(true);
	            	holder.tv_size.setEnabled(true);
            	} else {
            		holder.cb_cb1.setEnabled(false);
            		holder.rb_rb1.setEnabled(true);
	            	holder.iv_image1.setEnabled(false);
	            	holder.tv_name.setEnabled(false);
	            	holder.tv_size.setEnabled(false);
            	}
        		if (mSingleSelectMode) {
        			holder.cb_cb1.setVisibility(CheckBox.GONE);
        			holder.rb_rb1.setVisibility(RadioButton.VISIBLE);
        		} else {
        			holder.cb_cb1.setVisibility(CheckBox.VISIBLE);
        			holder.rb_rb1.setVisibility(RadioButton.GONE);
        		}
            	if (o.getName().startsWith("---")) {
            		//空処理
            		holder.cb_cb1.setVisibility(CheckBox.GONE);
            		holder.iv_image1.setVisibility(ImageView.GONE);
            		holder.tv_name.setText(o.getName());
            	} else {
                	holder.tv_name.setText(o.getName());
                	if (mShowLastModified) {
    		            if (!o.getCap().equals("") && !o.getCap().equals(" ")) {
    		            	String[] cap1 = new String[3];
    		            	cap1=o.getCap().split(",");
//    		            	Log.v("","cap="+cap1[1]);
    		            	holder.tv_size.setText(cap1[1]);
    		            	holder.tv_moddate.setText(cap1[0].substring(0,10));
    		            	holder.tv_modtime.setText(cap1[0].substring(11));
    		            } else {
    		            	holder.tv_size.setText("");
    		            	holder.tv_moddate.setText("");
    		            	holder.tv_modtime.setText("");
    		            }
                	} else {
    	            	holder.tv_size.setVisibility(TextView.GONE);
    	            	holder.tv_moddate.setVisibility(TextView.GONE);
    	            	holder.tv_modtime.setVisibility(TextView.GONE);
                	}
                   	if(o.isDir()) {
                   		holder.iv_image1.setImageResource(mIconImage[2]); //folder
                   		String ic=""+o.getSubDirItemCount()+" Item";
                   		holder.tv_size.setText(ic);
                   	} else {
                   		holder.iv_image1.setImageResource(mIconImage[3]); //sheet
                   	}
                   	if (o.isHidden() || o.hasExtendedAttr()) {
                   		if (o.hasExtendedAttr()) {
                       		holder.tv_name.setTextColor(Color.GREEN);
    		            	holder.tv_size.setTextColor(Color.GREEN);
    		            	holder.tv_moddate.setTextColor(Color.GREEN);
    		            	holder.tv_modtime.setTextColor(Color.GREEN);
                   		} else {
                       		holder.tv_name.setTextColor(Color.GRAY);
    		            	holder.tv_size.setTextColor(Color.GRAY);
    		            	holder.tv_moddate.setTextColor(Color.GRAY);
    		            	holder.tv_modtime.setTextColor(Color.GRAY);
                   		}
                   	} else {
                   		holder.tv_name.setTextColor(mThemeColorList.text_color_primary);
		            	holder.tv_size.setTextColor(mThemeColorList.text_color_primary);
		            	holder.tv_moddate.setTextColor(mThemeColorList.text_color_primary);
		            	holder.tv_modtime.setTextColor(mThemeColorList.text_color_primary);
                   	}
            	}
               	final int p = position;
             // 必ずsetChecked前にリスナを登録
             //	(convertView != null の場合は既に別行用のリスナが登録されている！)
               	holder.tv_select.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						if (mSingleSelectMode) holder.rb_rb1.setChecked(true); 
		           		else holder.cb_cb1.setChecked(!mDataItems.get(p).isChecked());
					}
               	});
           		holder.cb_cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						setButton(o,p,isChecked);
						notifyDataSetChanged();
  					}
   				});
           		holder.rb_rb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						setButton(o,p,isChecked);
						notifyDataSetChanged();
  					}
   				});
           		if (mSingleSelectMode) holder.rb_rb1.setChecked(mDataItems.get(p).isChecked()); 
           		else holder.cb_cb1.setChecked(mDataItems.get(p).isChecked());
       			
            }
            return v;
    };

    private void setButton(FileListItem o,int p, boolean isChecked) {
		if (enableListener) {
			enableListener=false;
				if (mSingleSelectMode) {
					if (isChecked) {
						FileListItem fi;
						for (int i=0;i<mDataItems.size();i++) {
							fi=mDataItems.get(i);
							if (fi.isChecked()&&p!=i) {
								fi.setChecked(false);
//								replaceDataItem(i,fi);
							}
						}
					}
				}
				enableListener=true;
		}
		boolean c_chk=o.isChecked();
		o.setChecked(isChecked);
		if (cb_ntfy!=null) 
				cb_ntfy.notifyToListener(isChecked, 
						new Object[]{p, c_chk});

    };
    
	static class ViewHolder {
		 TextView tv_name, tv_moddate, tv_modtime, tv_size;
		 LinearLayout tv_select;
		 ImageView iv_image1;
		 CheckBox cb_cb1;
		 RadioButton rb_rb1;
	}

}
