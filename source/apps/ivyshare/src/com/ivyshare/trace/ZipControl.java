package com.ivyshare.trace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipControl
{
	public static final int BUFFER_SIZE = 1024;
	public static int zipFile(String sourceFile, String targetZipFile) {

		ZipOutputStream zipOut = null;
		try {
			byte buff[] = new byte[BUFFER_SIZE];

			File fOut = new File(targetZipFile);
			if (fOut.exists()) {
				fOut.delete();
			}

			FileOutputStream target = new FileOutputStream(targetZipFile);
			zipOut = new ZipOutputStream(new BufferedOutputStream(target));

			File file = new File(sourceFile);
			if (!file.exists()) {
			    return -1;
			}
			File files[];
			if (file.isFile()) {
				files = new File[]{file};
			} else {
				files = file.listFiles();
			}
			for (int i = 0; i < files.length; i++) {
				FileInputStream fis = new FileInputStream(files[i]);
				BufferedInputStream origin = new BufferedInputStream(fis);

				ZipEntry entry = new ZipEntry(files[i].getName());
				zipOut.putNextEntry(entry);
				int count;
				while ((count = origin.read(buff)) != -1) {
					zipOut.write(buff, 0, count);
				}
				origin.close();
			}

		} catch (FileNotFoundException e) {
			File file = new File(targetZipFile);
			if (file != null) {
				file.deleteOnExit();
			}
			return -1;
		} catch (IOException e) {
			File file = new File(targetZipFile);
			if (file != null) {
				file.deleteOnExit();
			}
			return -1;
		} finally {
			try {
				if (zipOut != null) {
					zipOut.close();
				}
			} catch (IOException e) {
				
			}
		}
		return 0;
	}

	public static int unZipFile(String sourceZipFile, String targetDirectory) {
		File file = new File(sourceZipFile);
		String folderPath = targetDirectory + File.separator + file.getName();
		File fOut = new File(folderPath);
		if (fOut.exists()) {
			fOut.delete();
		}
		fOut.mkdir();

		try {
			ZipFile zipFile = new ZipFile(file);

	        for (Enumeration<?> entries = zipFile.entries(); entries.hasMoreElements();) {

	        	ZipEntry entry = ((ZipEntry)entries.nextElement());
	        	InputStream in = zipFile.getInputStream(entry);

	        	String str = folderPath + File.separator + entry.getName();
				File desFile = new File(str);
				if (!desFile.exists()) {
					File fileParentDir = desFile.getParentFile();
					if (!fileParentDir.exists()) {
						fileParentDir.mkdirs();
					}
					desFile.createNewFile();
				}

				FileOutputStream out = new FileOutputStream(desFile);
				byte buffer[] = new byte[BUFFER_SIZE];
				int count;
				while ((count = in.read(buffer)) > 0) {
					out.write(buffer, 0, count);
				}
				
				in.close();
				out.close();
			}
		} catch (IOException e) {
			return -1;
		}
		return 0;
	}
}
