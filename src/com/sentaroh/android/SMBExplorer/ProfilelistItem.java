package com.sentaroh.android.SMBExplorer;


public class ProfilelistItem implements Comparable<ProfilelistItem>{
		private String profileType;
		private String profileName;
		private String profileActive;
		private String profileUser;
		private String profilePass;
		private String profileAddr;
		private String profileShare;
		private boolean profileIschk;
		
		public ProfilelistItem(String pft,String pfn, String pfa, 
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
		public int compareTo(ProfilelistItem o) {
			if(this.profileName != null)
				return this.profileName.toLowerCase().compareTo(o.getName().toLowerCase()) ; 
//				return this.filename.toLowerCase().compareTo(o.getName().toLowerCase()) * (-1);
			else 
				throw new IllegalArgumentException();
		}
}

