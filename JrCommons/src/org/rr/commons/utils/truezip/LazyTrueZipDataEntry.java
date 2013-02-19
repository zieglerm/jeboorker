package org.rr.commons.utils.truezip;

import java.io.File;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.rr.commons.log.LoggerFactory;
import org.rr.commons.mufs.IResourceHandler;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;

class LazyTrueZipDataEntry extends TrueZipDataEntry {

	private IResourceHandler zipFileHandler;
	
	LazyTrueZipDataEntry(IResourceHandler zipFileHandler, String name) {
		super(name, (byte[]) null);
		this.zipFileHandler = zipFileHandler;
	}

	@Override
	public byte[] getBytes() {
		File zipFile = zipFileHandler.toFile();
		zipFile = new TFile(zipFile + "/" + path);
		TFileInputStream in = null;
		try {
			in = new TFileInputStream(zipFile);
		    byte[] bytes = IOUtils.toByteArray(in);
		    return bytes;
		} catch(Exception e) {
			LoggerFactory.getLogger().log(Level.SEVERE, "Failed to exctract " + path + " from archive " + zipFile, e);
		} finally {
		    IOUtils.closeQuietly(in);
		}	
		return null;
	}
	
	
}