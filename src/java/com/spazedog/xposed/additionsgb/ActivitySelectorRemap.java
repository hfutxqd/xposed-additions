/*
 * This file is part of the Xposed Additions Project: https://github.com/spazedog/xposed-additions
 *  
 * Copyright (c) 2014 Daniel Bergløv
 *
 * Xposed Additions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Xposed Additions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Xposed Additions. If not, see <http://www.gnu.org/licenses/>
 */

package com.spazedog.xposed.additionsgb;

import net.dinglisch.android.tasker.TaskerIntent;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.spazedog.xposed.additionsgb.Common.AppBuilder;
import com.spazedog.xposed.additionsgb.Common.AppBuilder.BuildAppView;
import com.spazedog.xposed.additionsgb.Common.RemapAction;
import com.spazedog.xposed.additionsgb.backend.service.XServiceManager;
import com.spazedog.xposed.additionsgb.configs.Actions;
import com.spazedog.xposed.additionsgb.tools.views.IWidgetPreference;
import com.spazedog.xposed.additionsgb.tools.views.WidgetPreference;

public class ActivitySelectorRemap extends PreferenceActivity implements OnPreferenceClickListener {
	
	final static int REQUEST_SELECT_TASKER = 1;
	final static int REQUEST_SELECT_APPSHORTCUT = 2;
	final static int REQUEST_CREATE_APPSHORTCUT = 3;
	
	private XServiceManager mPreferences;
	
	private Boolean mSetup = false;
	
	private String mAction;
	
	private AppBuilder mAppBuilder;
	
	private Intent getShortcutSelectIntent() {
		Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
		intent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
		intent.putExtra(Intent.EXTRA_TITLE, R.string.preference_title_select_shortcut);
		
		return intent;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.activity_selector_remap);
		
		mAction = getIntent().getStringExtra("action");
		mAppBuilder = new AppBuilder( getListView() );
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		if (Build.VERSION.SDK_INT >= 14) {
			LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
			Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.actionbar_v14_layout, root, false);

			if ("add_condition".equals(mAction)) {
				bar.setTitle(R.string.preference_add_condition);
				
			} else if ("add_action".equals(mAction)) {
				bar.setTitle(R.string.preference_add_action);
			}
			
			root.addView(bar, 0);
			
		} else {
			if ("add_condition".equals(mAction)) {
				setTitle(R.string.preference_add_condition);
				
			} else if ("add_action".equals(mAction)) {
				setTitle(R.string.preference_add_action);
			}
		}
	}
	
    @Override
    protected void onStart() {
    	super.onStart();
    	
    	mPreferences = XServiceManager.getInstance();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	if (mPreferences == null) {
    		onBackPressed();
    		
    	} else {
    		setup();
    	}
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	
    	mPreferences = null;
    }
    
    @Override
    public void onBackPressed() {
    	setResult(RESULT_CANCELED);
    	
    	finish();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	mAppBuilder.destroy();
    }
    
    private void setup() {
    	if (mSetup != (mSetup = true)) {
    		PreferenceScreen preferenceScreen = getPreferenceScreen();
    		
    		if ("add_condition".equals(mAction)) {
				preferenceScreen.removePreference(findPreference("custom_group"));
				preferenceScreen.addPreference(getSelectPreference(getResources().getString(R.string.condition_type_on), null, "on", null, null));
				preferenceScreen.addPreference(getSelectPreference(getResources().getString(R.string.condition_type_off), null, "off", null, null));
				preferenceScreen.addPreference(getSelectPreference(getResources().getString(R.string.condition_type_guard), null, "guard", null, null));
				
    		} else if ("add_action".equals(mAction)) {
    			PreferenceCategory category = (PreferenceCategory) findPreference("custom_group");
    			String condition = getIntent().getStringExtra("condition");
    			
    			for (RemapAction current : Actions.COLLECTION) {
    				Boolean isValid = current.isValid(this, condition);
    				Boolean displayAlert = current.hasAlert(this);
    				String alertMsg = current.getAlert(this);
    				String noticeMsg = current.getNotice(this);
    				
    				if (isValid || displayAlert) {
    					PreferenceGroup preferenceGroup = null;
    					Preference preference = null;
    					
    					if (current.isDispatchAction()) {
    						preferenceGroup = preferenceScreen;
    						preference = getSelectPreference(current.getLabel(this), getResources().getString(R.string.text_key, current.getAction()), current.getAction(), null, null);
    						
    					} else {
    						preferenceGroup = category;
    						preference = getSelectPreference(current.getLabel(this), current.getDescription(this), current.getAction(), null, null);
    					}
    					
    					if (!isValid && displayAlert) {
    						setDialog(null, alertMsg, preference, false);
    						
    					} else if (noticeMsg != null) {
    						setDialog(null, noticeMsg, preference, true);
    					}
    					
    					preferenceGroup.addPreference(preference);
    				}
    			}
    		}
    		
    		if (mPreferences.isPackageUnlocked()) {
    			findPreference("load_apps_preference").setOnPreferenceClickListener(this);
    			
    			if ("add_action".equals(mAction)) {
    				findPreference("select_shortcut_preference").setOnPreferenceClickListener(this);
    				
    			} else {
    				((PreferenceGroup) findPreference("application_group")).removePreference(findPreference("select_shortcut_preference"));
    			}
    			
				if ("add_action".equals(mAction) && TaskerIntent.testStatus(this).equals(TaskerIntent.Status.OK)) {
					findPreference("select_tasker_preference").setOnPreferenceClickListener(this);
				
				} else {
					((PreferenceGroup) findPreference("application_group")).removePreference(findPreference("select_tasker_preference"));
				}
    			
    		} else {
    			preferenceScreen.removePreference(findPreference("application_group"));
    		}
    	}
    }
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if ("load_apps_preference".equals(preference.getKey())) {
			final PreferenceCategory category = (PreferenceCategory) findPreference("application_group");
			category.removePreference(preference);
			
			Drawable tmpIcon = new ColorDrawable(android.R.color.transparent);
			
			try {
				tmpIcon = getPackageManager().getApplicationIcon("android");
				
			} catch (NameNotFoundException e) {}
			
			final Drawable defaultIcon = tmpIcon;
			
			mAppBuilder.build(new BuildAppView(){
				@Override
				public void onBuildAppView(ListView view, String name, String label) {
					category.addPreference(
						getSelectPreference(
							label,
							name,
							name,
							android.os.Build.VERSION.SDK_INT >= 11 ? defaultIcon : null,
							null
						)
					);
				}
			});
			
		} else if ("select_tasker_preference".equals(preference.getKey())) {
			startActivityForResult(TaskerIntent.getTaskSelectIntent(), REQUEST_SELECT_TASKER);

		} else if ("select_shortcut_preference".equals(preference.getKey())) {
			startActivityForResult(getShortcutSelectIntent(), REQUEST_SELECT_APPSHORTCUT);
			
		} else {
			Intent intent = getIntent();
			intent.putExtra("result", (String) ((IWidgetPreference) preference).getTag());
			
			setResult(RESULT_OK, intent);
			
			finish();
		}
		
		return false;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (intent != null) {
			Intent returnIntent = null;
			
			switch (requestCode) {
				case REQUEST_SELECT_TASKER:
					returnIntent = getIntent();
					returnIntent.putExtra("result", "tasker:" + intent.getDataString());
					
					break;
					
				case REQUEST_SELECT_APPSHORTCUT:
					startActivityForResult(intent, REQUEST_CREATE_APPSHORTCUT); break;
					
				case REQUEST_CREATE_APPSHORTCUT:
					String name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME).replace(':', '_');
					Intent appIntent = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
					
					returnIntent = getIntent();
					returnIntent.putExtra("result", "shortcut:" + name + ":" + appIntent.toUri(Intent.URI_INTENT_SCHEME));
					
					break;
			}
			
			if (returnIntent != null) {
				setResult(RESULT_OK, returnIntent);
				finish();
			}
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private Preference getSelectPreference(String title, String summary, String tag, Drawable icon, Intent intent) {
		WidgetPreference preference = new WidgetPreference(this);
		preference.setTitle(title);
		preference.setTag(tag);
		preference.setOnPreferenceClickListener(this);
		
		if (summary != null) {
			preference.setSummary(summary);
		}
		
		if (icon != null) {
			preference.setIcon(icon);
		}
		
		return preference;
	}
	
	private void setDialog(final String headline, final String message, Preference preference, final Boolean dispatchEvent) {
		preference.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				new AlertDialog.Builder(ActivitySelectorRemap.this)
				.setTitle(headline)
				.setMessage(message)
				.setCancelable(false)
				.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
		            public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						
						if (dispatchEvent) {
							ActivitySelectorRemap.this.onPreferenceClick(preference);
						}
					}
				})
				.show();
				
				return false;
			}
		});
	}
}
