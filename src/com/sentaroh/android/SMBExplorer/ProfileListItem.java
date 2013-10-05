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

public class ProfileListItem implements Comparable<ProfileListItem>{
		private String profileType;
		private String profileName;
		private String profileActive;
		private String profileUser;
		private String profilePass;
		private String profileAddr;
		private String profileShare;
		private boolean profileIschk;
		
		public ProfileListItem(String pft,String pfn, String pfa, 
				String pf_user, String pf_pass, String pf_addr, 
				String pf_share, boolean ic){
			profileType = pft;
			profileName = pfn;
			profileActive=pfa;
			profileUser = pf_user;
			profilePass = pf_pass;
			profileAddr = pf_addr;
			profileShare = pf_share;
			profileIschk = ic;
			
		}

		public String getType(){return profileType;}
		public String getName(){return profileName;}
		public String getActive(){return profileActive;}
		public String getUser(){return profileUser;}
		public String getPass(){return profilePass;}
		public String getAddr(){return profileAddr;}
		public String getShare(){return profileShare;}
		public boolean isChk(){return profileIschk;}
		public void setChk(boolean p){profileIschk=p;}
		
		@Override
		public int compareTo(ProfileListItem o) {
			if(this.profileName != null)
				return this.profileName.toLowerCase().compareTo(o.getName().toLowerCase()) ; 
//				return this.filename.toLowerCase().compareTo(o.getName().toLowerCase()) * (-1);
			else 
				throw new IllegalArgumentException();
		}
}

