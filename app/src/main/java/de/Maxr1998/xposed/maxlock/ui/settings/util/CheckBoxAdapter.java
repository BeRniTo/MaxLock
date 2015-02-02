/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2015  Maxr1998
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.Maxr1998.xposed.maxlock.ui.settings.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.haibison.android.lockpattern.LockPatternActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;
import de.Maxr1998.xposed.maxlock.ui.settings.KnockCodeSetupFragment;
import de.Maxr1998.xposed.maxlock.ui.settings.PinSetupFragment;

@SuppressLint("CommitPrefEdits")
public class CheckBoxAdapter extends BaseAdapter {


    private final List<Map<String, Object>> oriItemList;
    private final Fragment mFragment;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final SharedPreferences prefsPackages, prefsPerApp;
    private final Filter mFilter;
    private List<Map<String, Object>> mItemList;
    private AlertDialog dialog;

    @SuppressLint("WorldReadableFiles")
    public CheckBoxAdapter(Fragment fragment, Context context, List<Map<String, Object>> itemList) {
        mFragment = fragment;
        mContext = context;
        mInflater = LayoutInflater.from(context);
        oriItemList = mItemList = itemList;
        //noinspection deprecation
        prefsPackages = mContext.getSharedPreferences(Common.PREFS_PACKAGES, Context.MODE_WORLD_READABLE);
        prefsPerApp = mContext.getSharedPreferences(Common.PREFS_PER_APP, Context.MODE_PRIVATE);
        mFilter = new MyFilter();
    }

    @Override
    public int getCount() {
        return mItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return mItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mItemList.get(position).hashCode();
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.listview_item, null);
        }

        final TextView title = (TextView) convertView.findViewById(R.id.title);
        final TextView summary = (TextView) convertView.findViewById(R.id.summary);
        final ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
        final ToggleButton toggleLock = (ToggleButton) convertView.findViewById(R.id.toggleLock);
        final ImageButton imgEdit = (ImageButton) convertView.findViewById(R.id.edit);

        final String sTitle = (String) mItemList.get(position).get("title");
        final String sSummary = (String) mItemList.get(position).get("summary");
        final String key = (String) mItemList.get(position).get("key");
        final Drawable dIcon = (Drawable) mItemList.get(position).get("icon");

        title.setText(sTitle);
        summary.setText(sSummary);
        icon.setImageDrawable(dIcon);

        if (prefsPackages.getBoolean(key, false)) {
            toggleLock.setChecked(true);
            imgEdit.setVisibility(View.VISIBLE);
        } else {
            toggleLock.setChecked(false);
            imgEdit.setVisibility(View.GONE);
        }

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityManager am = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
                am.killBackgroundProcesses(key);
                Intent it = mContext.getPackageManager().getLaunchIntentForPackage(key);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(it);
            }
        });

        imgEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // AlertDialog View
                // Fake die checkbox
                View checkBoxView = View.inflate(mContext, R.layout.per_app_settings, null);
                CheckBox fakeDie = (CheckBox) checkBoxView.findViewById(R.id.cb_fake_die);
                fakeDie.setChecked(prefsPackages.getBoolean(key + "_fake", false));
                fakeDie.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityManager am = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
                        am.killBackgroundProcesses(key);
                        CheckBox cb = (CheckBox) v;
                        boolean value = cb.isChecked();
                        prefsPackages.edit()
                                .putBoolean(key + "_fake", value)
                                .commit();
                    }
                });
                // Custom password checkbox
                CheckBox customPassword = (CheckBox) checkBoxView.findViewById(R.id.cb_custom_pw);
                customPassword.setChecked(prefsPerApp.contains(key));
                customPassword.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;
                        boolean checked = cb.isChecked();
                        if (checked) {
                            dialog.dismiss();
                            final AlertDialog.Builder choose_lock = new AlertDialog.Builder(mContext);
                            CharSequence[] cs = new CharSequence[]{
                                    mContext.getString(R.string.pref_locking_type_password),
                                    mContext.getString(R.string.pref_locking_type_pin),
                                    mContext.getString(R.string.pref_locking_type_knockcode),
                                    mContext.getString(R.string.pref_locking_type_pattern)
                            };
                            choose_lock.setItems(cs, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    Fragment frag = new Fragment();
                                    switch (i) {
                                        case 0:
                                            Util.setPassword(mContext, key);
                                            break;
                                        case 1:
                                            frag = new PinSetupFragment();
                                            break;
                                        case 2:
                                            frag = new KnockCodeSetupFragment();
                                            break;
                                        case 3:
                                            Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null, mContext, LockPatternActivity.class);
                                            mFragment.startActivityForResult(intent, Util.getPatternCode(position));
                                            break;
                                    }
                                    if (i == 1 || i == 2) {
                                        Bundle b = new Bundle(1);
                                        b.putString(Common.INTENT_EXTRAS_CUSTOM_APP, key);
                                        frag.setArguments(b);
                                        ((SettingsActivity) mContext).getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, frag).addToBackStack(null).commit();
                                    }
                                }
                            }).show();
                        } else
                            prefsPerApp.edit().remove(key).remove(key + Common.APP_KEY_PREFERENCE).apply();

                    }
                });
                // Finish dialog
                dialog = new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.txt_settings))
                        .setIcon(dIcon)
                        .setView(checkBoxView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dlg, int id) {
                                dlg.dismiss();
                            }
                        }).show();
            }
        });

        toggleLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ActivityManager am = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
                am.killBackgroundProcesses(key);

                ToggleButton tb = (ToggleButton) v;

                boolean value = tb.isChecked();
                if (value) {
                    prefsPackages.edit()
                            .putBoolean(key, true)
                            .commit();
                    // TO-DO: Custom reveal animations
                    imgEdit.setVisibility(View.VISIBLE);
                } else {
                    prefsPackages.edit().remove(key).commit();
                    imgEdit.setVisibility(View.GONE);
                }
            }
        });

        return convertView;
    }

    public Filter getFilter() {
        return mFilter;
    }

    class MyFilter extends Filter {

        @SuppressLint("DefaultLocale")
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            constraint = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            if (constraint.length() == 0) {
                results.values = oriItemList;
                results.count = oriItemList.size();
            } else {
                List<Map<String, Object>> filteredList = new ArrayList<>();

                for (Map<String, Object> app : oriItemList) {
                    String title = ((String) app.get("title")).toLowerCase();
                    for (String part : title.split(" ")) {
                        if (part.indexOf((String) constraint) == 0) {
                            filteredList.add(app);
                        }
                    }
                }
                results.values = filteredList;
                results.count = filteredList.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

            mItemList = (List<Map<String, Object>>) results.values;
            notifyDataSetChanged();
        }


    }

}