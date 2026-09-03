/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.bjusystems.vdrmanager.ng.backup;


import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.gui.Utils;

/**
 * An activity for accessing the backup settings.
 *
 * @author Jimmy Shih
 */
public class BackupSettingsActivity extends AbstractSettingsActivity {

    private static final int DIALOG_CONFIRM_RESTORE_ID = 0;

    private static final int REQUEST_CODE_BACKUP = 101;
    private static final int REQUEST_CODE_RESTORE = 102;


    /**
     * The Backup preference.
     */
    Preference backupPreference;
    /**
     * The Restore preference.
     */
    Preference restorePreference;

    Preference backupToFilePreference;
    Preference restoreFromFilePreference;

    /*
     * Note that sharedPreferenceChangeListenr cannot be an anonymous inner class.
     * Anonymous inner class will get garbage collected.
     */
    private final OnSharedPreferenceChangeListener
            sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            // Note that key can be null
            //if (PreferencesUtils.getKey(BackupSettingsActivity.this, R.string.recording_track_id_key)
            //  .equals(key)) {
            //updateUi();
            //}
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        final boolean permissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        addPreferencesFromResource(R.xml.backup_settings);
        backupPreference = findPreference(getString(R.string.settings_backup_now_key));
        backupPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (permissionGranted == false) {
                    Utils.say(getApplication(), R.string.permission_rationale, Toast.LENGTH_LONG);
                    return false;
                }
                Intent intent = IntentUtils.newIntent(BackupSettingsActivity.this, BackupActivity.class);
                startActivity(intent);
                return true;
            }
        });
        restorePreference = findPreference(getString(R.string.settings_backup_restore_key));
        restorePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (permissionGranted == false) {
                    Utils.say(getApplication(), R.string.permission_rationale, Toast.LENGTH_LONG);
                    return false;
                }
                showDialog(DIALOG_CONFIRM_RESTORE_ID);
                return true;
            }
        });

        backupToFilePreference = findPreference(getString(R.string.settings_backup_to_file_key));
        if (backupToFilePreference != null) {
            backupToFilePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startSAFBackup();
                    return true;
                }
            });
        }

        restoreFromFilePreference = findPreference(getString(R.string.settings_restore_from_file_key));
        if (restoreFromFilePreference != null) {
            restoreFromFilePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startSAFRestore();
                    return true;
                }
            });
        }
    }

    private void startSAFBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        String fileName = "vdrmanager-backup-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".zip";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, REQUEST_CODE_BACKUP);
    }

    private void startSAFRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        startActivityForResult(intent, REQUEST_CODE_RESTORE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        final Uri uri = data.getData();
        if (requestCode == REQUEST_CODE_BACKUP) {
            performSAFBackup(uri);
        } else if (requestCode == REQUEST_CODE_RESTORE) {
            performSAFRestore(uri);
        }
    }

    private void performSAFBackup(final Uri uri) {
        new AsyncTask<Void, Void, Boolean>() {
            private ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                progressDialog = DialogUtils.createSpinnerProgressDialog(BackupSettingsActivity.this,
                        R.string.settings_backup_now_progress_message, null);
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                OutputStream os = null;
                try {
                    os = getContentResolver().openOutputStream(uri);
                    ExternalFileBackup backup = new ExternalFileBackup(BackupSettingsActivity.this);
                    backup.writeToStream(os);
                    return true;
                } catch (IOException e) {
                    Log.e("Backup", "Error during SAF backup", e);
                    return false;
                } finally {
                    IOUtils.closeQuietly(os);
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(BackupSettingsActivity.this,
                        success ? R.string.sd_card_save_success : R.string.sd_card_save_error,
                        Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void performSAFRestore(final Uri uri) {
        DialogUtils.createConfirmationDialog(this,
                R.string.settings_backup_restore_confirm_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeSAFRestore(uri);
                    }
                }).show();
    }

    private void executeSAFRestore(final Uri uri) {
        new AsyncTask<Void, Void, Boolean>() {
            private ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                progressDialog = DialogUtils.createSpinnerProgressDialog(BackupSettingsActivity.this,
                        R.string.settings_backup_restore_progress_message, null);
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    ExternalFileBackup backup = new ExternalFileBackup(BackupSettingsActivity.this);
                    backup.restoreFromUri(uri);
                    return true;
                } catch (IOException e) {
                    Log.e("Restore", "Error during SAF restore", e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(BackupSettingsActivity.this,
                        success ? R.string.sd_card_import_success : R.string.sd_card_import_error,
                        Toast.LENGTH_SHORT).show();
                if (success) {
                    Intent intent = new Intent(BackupSettingsActivity.this, de.bjusystems.vdrmanager.ng.gui.PreferencesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }

        }.execute();
    }



    @Override
    protected Dialog onCreateDialog(int id) {
        if (id != DIALOG_CONFIRM_RESTORE_ID) {
            return null;
        }
        return DialogUtils.createConfirmationDialog(this,
                R.string.settings_backup_restore_confirm_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = IntentUtils.newIntent(
                                BackupSettingsActivity.this, RestoreChooserActivity.class);
                        startActivity(intent);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //updateUi();
    }

//  /**
//   * Updates the UI based on the recording state.
//   */
//  private void updateUi() {
//    boolean isRecording = PreferencesUtils.getLong(this, R.string.recording_track_id_key)
//        != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
//    backupPreference.setEnabled(!isRecording);
//    restorePreference.setEnabled(!isRecording);
//    backupPreference.setSummary(isRecording ? R.string.settings_not_while_recording
//        : R.string.settings_backup_now_summary);
//    restorePreference.setSummary(isRecording ? R.string.settings_not_while_recording
//        : R.string.settings_backup_restore_summary);
//  }
}
