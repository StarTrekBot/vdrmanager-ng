/*
 * Copyright 2010 Google Inc.
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.bjusystems.vdrmanager.ng.data.db.DBAccess;


/**
 * Handler for writing or reading single-file backups.
 * 
 * @author Rodrigo Damazio
 */
class ExternalFileBackup {
	// Filename format - in UTC
	private static final SimpleDateFormat BACKUP_FILENAME_FORMAT = new SimpleDateFormat(
			"'backup-'yyyy-MM-dd_HH-mm-ss'.zip'");
	static {
		BACKUP_FILENAME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private static final String BACKUPS_SUBDIR = "backups";
	private static final int BACKUP_FORMAT_VERSION = 1;
	private static final String ZIP_ENTRY_NAME = "backup.mybillingbuddy.v"
			+ BACKUP_FORMAT_VERSION;
	private static final int COMPRESSION_LEVEL = 8;

	private final Context context;
	
	public ExternalFileBackup(Context context) {
		this.context = context;
	}

	/**
	 * Returns whether the backups directory is (or can be made) available.
	 * 
	 * @param create
	 *            whether to try creating the directory if it doesn't exist
	 */
	public boolean isBackupsDirectoryAvailable(boolean create) {
		return getBackupsDirectory(create) != null;
	}

	/**
	 * Returns the backup directory, or null if not available.
	 * 
	 * @param create
	 *            whether to try creating the directory if it doesn't exist
	 */
	private File getBackupsDirectory(boolean create) {
		String dirName = FileUtils.buildExternalDirectoryPath(BACKUPS_SUBDIR);
		final File dir = new File(dirName);
		Log.d(Constants.TAG, "Dir: " + dir.getAbsolutePath());
		if (create) {
			// Try to create - if that fails, return null
			return FileUtils.ensureDirectoryExists(dir) ? dir : null;
		} else {
			// Return it if it already exists, otherwise return null
			return dir.isDirectory() ? dir : null;
		}
	}

	/**
	 * Returns a list of available backups to be restored.
	 */
	public Date[] getAvailableBackups() {
		File dir = getBackupsDirectory(false);
		if (dir == null) {
			return null;
		}
		String[] fileNames = dir.list();

		List<Date> backupDates = new ArrayList<Date>(fileNames.length);
		for (int i = 0; i < fileNames.length; i++) {
			String fileName = fileNames[i];
			try {
				backupDates.add(BACKUP_FILENAME_FORMAT.parse(fileName));
			} catch (ParseException e) {
				// Not a backup file, ignore
			}
		}

		return backupDates.toArray(new Date[backupDates.size()]);
	}

	/**
	 * Writes the backup to the default file.
	 */
	public void writeToDefaultFile() throws IOException {
		 writeToFile(getFileForDate(new Date()));
	}

	/**
	 * Restores the backup from the given date.
	 */
	public void restoreFromDate(Date when) throws IOException {
		 restoreFromFile(getFileForDate(when));
	}

	public void restoreFromFile(String path ) throws IOException {
		 restoreFromFile(new File(path));
	}

	/**
	 * Produces the proper file descriptor for the given backup date.
	 */
	private File getFileForDate(Date when) {
		File dir = getBackupsDirectory(false);
		String fileName = BACKUP_FILENAME_FORMAT.format(when);
		File file = new File(dir, fileName);
		return file;
	}

	/**
	 * Synchronously writes a backup to the given file.
	 */
	public void writeToFile(File outputFile) throws IOException {
		Log.d(Constants.TAG,
				"Writing backup to file " + outputFile.getAbsolutePath());
		writeToStream(new FileOutputStream(outputFile));
	}

	public void writeToStream(OutputStream outputStream) throws IOException {
		ZipOutputStream compressedStream = new ZipOutputStream(outputStream);
		compressedStream.setLevel(COMPRESSION_LEVEL);
		compressedStream.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
		DataOutputStream outWriter = new DataOutputStream(compressedStream);

		try {
			// Dump preferences
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			PreferenceBackupHelper preferencesHelper = new PreferenceBackupHelper();
			preferencesHelper.exportPreferences(preferences, outWriter);

			File f = new File(DBAccess.getDataBaseFile(context));
			if(f.exists()){
				compressedStream.putNextEntry(new ZipEntry(DBAccess.DATABASE_NAME));
				IOUtils.copy(new FileInputStream(DBAccess.getDataBaseFile(context)), outWriter);
			}

		} catch (IOException e) {
			throw e;
		} finally {
			compressedStream.closeEntry();
			compressedStream.close();
		}
	}

	/**
	 * Synchronously restores the backup from the given file.
	 */
	private void restoreFromFile(File inputFile) throws IOException {
		Log.d(Constants.TAG,
				"Restoring from file " + inputFile.getAbsolutePath());

		ZipFile zipFile = new ZipFile(inputFile, ZipFile.OPEN_READ);
		ZipEntry zipEntry = zipFile.getEntry(ZIP_ENTRY_NAME);
		if (zipEntry == null) {
			zipFile.close();
			throw new IOException("Invalid backup ZIP file");
		}

		InputStream compressedStream = zipFile.getInputStream(zipEntry);
		restoreFromStreams(compressedStream, zipFile);
	}

	public void restoreFromUri(android.net.Uri uri) throws IOException {
		// SAF Uris need to be handled carefully.
		// For ZipFile, we might need a local file.
		// Or we can use ZipInputStream if we don't need random access.
		// But here we need multiple entries.
		
		// Let's use ZipInputStream.
		InputStream inputStream = context.getContentResolver().openInputStream(uri);
		java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(inputStream);
		java.util.zip.ZipEntry entry;
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		PreferenceBackupHelper preferencesHelper = new PreferenceBackupHelper();

		while ((entry = zis.getNextEntry()) != null) {
			if (entry.getName().equals(ZIP_ENTRY_NAME)) {
				DataInputStream reader = new DataInputStream(zis);
				preferencesHelper.importPreferences(reader, preferences);
			} else if (entry.getName().equals(DBAccess.DATABASE_NAME)) {
				IOUtils.copy(zis, new FileOutputStream(DBAccess.getDataBaseFile(context)));
				deleteJournal(DBAccess.getDataBaseFile(context));
			}
			zis.closeEntry();
		}
		zis.close();
	}

	private void restoreFromStreams(InputStream compressedStream, ZipFile zipFile) throws IOException {
		DataInputStream reader = new DataInputStream(compressedStream);
		try {
			// Restore preferences
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			PreferenceBackupHelper preferencesHelper = new PreferenceBackupHelper();
			preferencesHelper.importPreferences(reader, preferences);

			ZipEntry zipEntry = zipFile.getEntry(DBAccess.DATABASE_NAME);
			if (zipEntry != null) {
				IOUtils.copy(zipFile.getInputStream(zipEntry), new FileOutputStream(DBAccess.getDataBaseFile(context)));
				deleteJournal(DBAccess.getDataBaseFile(context));
			}
		} finally {
			compressedStream.close();
			zipFile.close();
		}
	}

	
	private static void deleteJournal(String db){
		if(db == null){
			return;
		}
		new File(db+"-journal").delete();
	}
}
