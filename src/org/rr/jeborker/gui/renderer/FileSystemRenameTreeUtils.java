package org.rr.jeborker.gui.renderer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.rr.commons.utils.ListUtils;
import org.rr.commons.utils.StringUtils;

class FileSystemRenameTreeUtils {
	
	/**
	 * Get some change offers for the given file name.
	 */
	static List<String> getCommaChangeOffers(final String filename) {
		List<String> offers = new ArrayList<String>() {

			@Override
			public boolean add(String e) {
				String clean = clean(e);
				return super.add(clean);
			}

			@Override
			public boolean remove(Object o) {
				String clean = clean((String) o);
				return super.remove(clean);
			}
			
			/**
			 * Clean up the given string.
			 */
			private String clean(String s) {
				String result = s;
				result = StringUtils.replace(s, "  ", "");
				result = result.trim();
				return result;
			}			
		};
		String name = FilenameUtils.getBaseName(filename);
		String ext = FilenameUtils.getExtension(filename);
		if(name.indexOf(',') != -1) {
			offers.add(name.replaceAll("([\\w\\.]*),\\s{0,1}([\\w\\.]*)", "$2 $1").trim() + "." + ext);
			offers.add(name.replaceAll("([\\w\\.]*\\s[\\w\\.]*),\\s{0,1}([\\w\\.]*)", "$2 $1").trim() + "." + ext);
			offers.add(name.replaceAll("([\\w\\.]*\\s[\\w\\.]*),\\s{0,1}([\\w\\.]*\\s\\w*)", "$2 $1").trim() + "." + ext);
			offers.add(name.replaceAll("([\\w\\.]*),\\s{0,1}([\\w\\.]*)\\s([\\w\\.]{1,})", "$2 $3 $1").trim() + "." + ext);
		}
		
		offers = ListUtils.distinct(offers);
		offers.remove(filename);
		return offers;
	}
	
}
